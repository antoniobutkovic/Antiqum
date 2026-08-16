import { createHash } from "node:crypto";

import type { MuseumCategory, MuseumRecord } from "./types";

const WIKIDATA_SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";
const WIKIDATA_API_ENDPOINT = "https://www.wikidata.org/w/api.php";
const USER_AGENT = "Antiqum/1.0 (museum catalog sync; https://antiqum.vercel.app)";

interface SparqlResponse {
  results?: {
    bindings?: Array<{ museum?: { value?: string } }>;
  };
}

interface WikidataClaim {
  rank?: string;
  mainsnak?: {
    snaktype?: string;
    datavalue?: { value?: unknown };
  };
}

interface WikidataEntity {
  id?: string;
  missing?: string;
  modified?: string;
  labels?: Record<string, { value?: string }>;
  descriptions?: Record<string, { value?: string }>;
  claims?: Record<string, WikidataClaim[]>;
}

interface WikidataEntitiesResponse {
  entities?: Record<string, WikidataEntity>;
}

export async function fetchMuseumIds(
  afterId: string | null,
  limit: number,
): Promise<string[]> {
  if (afterId !== null && !/^Q[0-9]+$/.test(afterId)) {
    throw new Error("Invalid Wikidata synchronization cursor");
  }
  const cursorFilter = afterId
    ? `FILTER(STR(?museum) > "http://www.wikidata.org/entity/${afterId}")`
    : "";
  const query = `
    SELECT DISTINCT ?museum
    WHERE {
      ?museum wdt:P31 wd:Q33506 .
      ?museum wdt:P625 ?location .
      ${cursorFilter}
    }
    ORDER BY ?museum
    LIMIT ${Math.min(Math.max(limit, 1), 500)}
  `;
  const url = new URL(WIKIDATA_SPARQL_ENDPOINT);
  url.searchParams.set("query", query);
  url.searchParams.set("format", "json");
  const response = await fetchJson<SparqlResponse>(url, {
    Accept: "application/sparql-results+json",
  });
  return (response.results?.bindings ?? [])
    .map((binding) => binding.museum?.value?.split("/").at(-1) ?? "")
    .filter((id) => /^Q[0-9]+$/.test(id));
}

export async function fetchMuseumRecords(ids: string[]): Promise<MuseumRecord[]> {
  const entities = await fetchEntities(ids, "info|labels|descriptions|claims");
  const relatedIds = new Set<string>();
  for (const entity of entities.values()) {
    for (const property of ["P17", "P31", "P131"]) {
      for (const id of entityIds(entity, property)) relatedIds.add(id);
    }
  }
  const relatedEntities = await fetchEntities([...relatedIds], "labels");
  const labels = new Map<string, string>();
  for (const [id, entity] of relatedEntities) {
    labels.set(id, entity.labels?.en?.value ?? id);
  }

  return ids.flatMap((id) => {
    const entity = entities.get(id);
    if (!entity || entity.missing !== undefined) return [];
    const coordinates = coordinateClaim(entity, "P625");
    if (!coordinates) return [];
    const name = entity.labels?.en?.value?.trim() || id;
    const description = entity.descriptions?.en?.value?.trim() || "Museum catalog entry from Wikidata.";
    const city = entityIds(entity, "P131").map((value) => labels.get(value) ?? "").find(Boolean) ?? "";
    const country = entityIds(entity, "P17").map((value) => labels.get(value) ?? "").find(Boolean) ?? "";
    const typeLabels = entityIds(entity, "P31").map((value) => labels.get(value) ?? "");
    const imageName = stringClaim(entity, "P18");
    const foundedYear = timeClaim(entity, "P571")?.match(/[+-](\d{1,4})-/)?.[1] ?? null;
    const normalized = {
      wikidataId: id,
      name,
      description,
      category: categoryFor([name, description, ...typeLabels].join(" ")),
      city,
      country,
      latitude: coordinates.latitude,
      longitude: coordinates.longitude,
      imageUrl: imageName
        ? `https://commons.wikimedia.org/wiki/Special:FilePath/${encodeURIComponent(imageName)}`
        : null,
      website: stringClaim(entity, "P856"),
      address: monolingualTextClaim(entity, "P6375"),
      foundedYear,
      sourceModifiedAt: entity.modified ?? null,
    };
    return [{
      ...normalized,
      contentHash: createHash("sha256").update(JSON.stringify(normalized)).digest("hex"),
    }];
  });
}

async function fetchEntities(
  ids: string[],
  props: string,
): Promise<Map<string, WikidataEntity>> {
  const result = new Map<string, WikidataEntity>();
  for (let index = 0; index < ids.length; index += 50) {
    const chunk = ids.slice(index, index + 50);
    if (chunk.length === 0) continue;
    const url = new URL(WIKIDATA_API_ENDPOINT);
    url.searchParams.set("action", "wbgetentities");
    url.searchParams.set("ids", chunk.join("|"));
    url.searchParams.set("props", props);
    url.searchParams.set("languages", "en");
    url.searchParams.set("languagefallback", "1");
    url.searchParams.set("format", "json");
    url.searchParams.set("formatversion", "2");
    const response = await fetchJson<WikidataEntitiesResponse>(url);
    for (const [id, entity] of Object.entries(response.entities ?? {})) {
      result.set(id, entity);
    }
  }
  return result;
}

async function fetchJson<T>(url: URL, headers: Record<string, string> = {}): Promise<T> {
  let lastError: Error | null = null;
  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      const response = await fetch(url, {
        headers: { "User-Agent": USER_AGENT, ...headers },
        signal: AbortSignal.timeout(20_000),
      });
      if (!response.ok) {
        throw new Error(`Wikidata returned HTTP ${response.status}`);
      }
      return await response.json() as T;
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error));
      if (attempt < 2) await delay(500 * 2 ** attempt);
    }
  }
  throw lastError ?? new Error("Wikidata request failed");
}

function preferredClaim(entity: WikidataEntity, property: string): WikidataClaim | null {
  const claims = (entity.claims?.[property] ?? []).filter(
    (claim) => claim.mainsnak?.snaktype === "value" && claim.mainsnak.datavalue?.value !== undefined,
  );
  return claims.find((claim) => claim.rank === "preferred") ?? claims[0] ?? null;
}

function stringClaim(entity: WikidataEntity, property: string): string | null {
  const value = preferredClaim(entity, property)?.mainsnak?.datavalue?.value;
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function monolingualTextClaim(entity: WikidataEntity, property: string): string | null {
  const value = preferredClaim(entity, property)?.mainsnak?.datavalue?.value;
  if (!value || typeof value !== "object") return null;
  const text = (value as { text?: unknown }).text;
  return typeof text === "string" && text.trim() ? text.trim() : null;
}

function timeClaim(entity: WikidataEntity, property: string): string | null {
  const value = preferredClaim(entity, property)?.mainsnak?.datavalue?.value;
  if (!value || typeof value !== "object") return null;
  const time = (value as { time?: unknown }).time;
  return typeof time === "string" ? time : null;
}

function coordinateClaim(
  entity: WikidataEntity,
  property: string,
): { latitude: number; longitude: number } | null {
  const value = preferredClaim(entity, property)?.mainsnak?.datavalue?.value;
  if (!value || typeof value !== "object") return null;
  const latitude = Number((value as { latitude?: unknown }).latitude);
  const longitude = Number((value as { longitude?: unknown }).longitude);
  return Number.isFinite(latitude) && Number.isFinite(longitude)
    ? { latitude, longitude }
    : null;
}

function entityIds(entity: WikidataEntity, property: string): string[] {
  return (entity.claims?.[property] ?? []).flatMap((claim) => {
    const value = claim.mainsnak?.datavalue?.value;
    if (!value || typeof value !== "object") return [];
    const id = (value as { id?: unknown }).id;
    return typeof id === "string" && /^Q[0-9]+$/.test(id) ? [id] : [];
  });
}

function categoryFor(value: string): MuseumCategory {
  const text = value.toLowerCase();
  if (text.includes("natural history")) return "natural_history";
  if (text.includes("archaeolog")) return "archaeology";
  if (text.includes("ethnograph")) return "ethnography";
  if (text.includes("maritime") || text.includes("naval")) return "maritime";
  if (text.includes("military") || text.includes("war museum")) return "military";
  if (text.includes("technolog") || text.includes("technical museum")) return "technology";
  if (text.includes("science")) return "science";
  if (text.includes("histor") || text.includes("heritage")) return "history";
  if (text.includes("art") || text.includes("gallery")) return "art";
  return "other";
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
