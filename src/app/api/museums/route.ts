import { listMuseums } from "@/museums/database";
import { InvalidMuseumCursorError } from "@/museums/pagination";
import { museumCategories, type MuseumCategory, type MuseumSort } from "@/museums/types";

export async function GET(request: Request): Promise<Response> {
  try {
    const query = parseMuseumQuery(new URL(request.url).searchParams);
    const page = await listMuseums(query);
    return Response.json(page, {
      headers: {
        "Cache-Control": "public, max-age=60, stale-while-revalidate=300",
      },
    });
  } catch (error) {
    if (error instanceof InvalidMuseumCursorError || error instanceof MuseumQueryError) {
      return Response.json({ error: error.message }, { status: 400 });
    }
    console.error("Unable to list museums", error);
    return Response.json({ error: "Unable to load museums" }, { status: 503 });
  }
}

class MuseumQueryError extends Error {}

function parseMuseumQuery(parameters: URLSearchParams) {
  const limit = integerParameter(parameters, "limit", 20, 1, 100);
  const latitude = numberParameter(parameters, "latitude", 45.815, -90, 90);
  const longitude = numberParameter(parameters, "longitude", 15.9819, -180, 180);
  const radiusKm = optionalNumberParameter(parameters, "radiusKm", 0.1, 20_000);
  const sortValue = parameters.get("sort") ?? "distance";
  if (sortValue !== "distance" && sortValue !== "alphabetical") {
    throw new MuseumQueryError("sort must be distance or alphabetical");
  }
  const category = parseCategory(parameters.get("category"));
  const search = parameters.get("query")?.trim().slice(0, 120) || null;
  const cursor = parameters.get("cursor")?.trim() || null;

  return {
    cursor,
    limit,
    latitude,
    longitude,
    radiusKm,
    sort: sortValue as MuseumSort,
    category,
    search,
  };
}

function parseCategory(value: string | null): MuseumCategory | null {
  if (!value || value.toLowerCase() === "all") return null;
  const normalized = value
    .replace(/([a-z])([A-Z])/g, "$1_$2")
    .replace(/[ -]+/g, "_")
    .toLowerCase();
  if (!museumCategories.includes(normalized as MuseumCategory)) {
    throw new MuseumQueryError("Unknown museum category");
  }
  return normalized as MuseumCategory;
}

function integerParameter(
  parameters: URLSearchParams,
  name: string,
  fallback: number,
  minimum: number,
  maximum: number,
): number {
  const raw = parameters.get(name);
  if (raw === null) return fallback;
  const value = Number(raw);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new MuseumQueryError(`${name} must be an integer between ${minimum} and ${maximum}`);
  }
  return value;
}

function numberParameter(
  parameters: URLSearchParams,
  name: string,
  fallback: number,
  minimum: number,
  maximum: number,
): number {
  const raw = parameters.get(name);
  if (raw === null) return fallback;
  const value = Number(raw);
  if (!Number.isFinite(value) || value < minimum || value > maximum) {
    throw new MuseumQueryError(`${name} must be between ${minimum} and ${maximum}`);
  }
  return value;
}

function optionalNumberParameter(
  parameters: URLSearchParams,
  name: string,
  minimum: number,
  maximum: number,
): number | null {
  const raw = parameters.get(name);
  if (raw === null) return null;
  const value = Number(raw);
  if (!Number.isFinite(value) || value < minimum || value > maximum) {
    throw new MuseumQueryError(`${name} must be between ${minimum} and ${maximum}`);
  }
  return value;
}
