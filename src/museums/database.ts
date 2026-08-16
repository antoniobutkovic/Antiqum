import { neon } from "@neondatabase/serverless";

import { requireEnvironment } from "@/config";

import {
  decodeMuseumCursor,
  encodeMuseumCursor,
  museumQueryScope,
} from "./pagination";
import type {
  MuseumApiItem,
  MuseumPageQuery,
  MuseumPageResult,
  MuseumRecord,
} from "./types";

interface MuseumRow {
  wikidataId: string;
  name: string;
  description: string;
  category: string;
  city: string;
  country: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
  imageUrl: string | null;
  website: string | null;
  address: string | null;
  foundedYear: string | null;
  sortName: string;
}

export interface MuseumSyncLease {
  token: string;
  runId: string;
  cursor: string | null;
}

function database() {
  return neon(requireEnvironment("DATABASE_URL"));
}

export async function listMuseums(query: MuseumPageQuery): Promise<MuseumPageResult> {
  const sql = database();
  const scopeQuery = {
    latitude: query.latitude,
    longitude: query.longitude,
    radiusKm: query.radiusKm,
    sort: query.sort,
    category: query.category,
    search: query.search,
  };
  const scope = museumQueryScope(scopeQuery);
  const cursor = query.cursor
    ? decodeMuseumCursor(query.cursor, query.sort, scope)
    : null;
  const commonParameters = [
    query.latitude,
    query.longitude,
    query.category,
    query.search,
    query.radiusKm,
  ];
  const distanceExpression = `
    6371.0 * 2.0 * ASIN(
      SQRT(LEAST(1.0, GREATEST(0.0,
        POWER(SIN(RADIANS(latitude - $1::float8) / 2.0), 2) +
        COS(RADIANS($1::float8)) * COS(RADIANS(latitude)) *
        POWER(SIN(RADIANS(longitude - $2::float8) / 2.0), 2)
      )))
    )
  `;
  const rankedQuery = `
    WITH ranked AS (
      SELECT
        wikidata_id AS "wikidataId",
        name,
        description,
        category,
        city,
        country,
        latitude,
        longitude,
        image_url AS "imageUrl",
        website,
        address,
        founded_year AS "foundedYear",
        LOWER(name) AS "sortName",
        ${distanceExpression} AS "distanceKm"
      FROM museums
      WHERE is_active = TRUE
        AND ($3::text IS NULL OR category = $3::text)
        AND (
          $4::text IS NULL OR
          search_vector @@ websearch_to_tsquery('simple', $4::text)
        )
    )
  `;

  let rows: MuseumRow[];
  if (query.sort === "distance") {
    rows = await sql.query(
      `${rankedQuery}
       SELECT *
       FROM ranked
       WHERE ($5::float8 IS NULL OR "distanceKm" <= $5::float8)
         AND (
           $6::float8 IS NULL OR
           "distanceKm" > $6::float8 OR
           ("distanceKm" = $6::float8 AND "wikidataId" > $7::text)
         )
       ORDER BY "distanceKm", "wikidataId"
       LIMIT $8::integer`,
      [
        ...commonParameters,
        cursor?.key ?? null,
        cursor?.id ?? null,
        query.limit + 1,
      ],
    ) as MuseumRow[];
  } else {
    rows = await sql.query(
      `${rankedQuery}
       SELECT *
       FROM ranked
       WHERE ($5::float8 IS NULL OR "distanceKm" <= $5::float8)
         AND (
           $6::text IS NULL OR
           "sortName" > $6::text OR
           ("sortName" = $6::text AND "wikidataId" > $7::text)
         )
       ORDER BY "sortName", "wikidataId"
       LIMIT $8::integer`,
      [
        ...commonParameters,
        cursor?.key ?? null,
        cursor?.id ?? null,
        query.limit + 1,
      ],
    ) as MuseumRow[];
  }

  const hasMore = rows.length > query.limit;
  const pageRows = rows.slice(0, query.limit);
  const museums: MuseumApiItem[] = pageRows.map((row) => ({
    id: row.wikidataId,
    name: row.name,
    description: row.description,
    category: apiCategory(row.category),
    city: row.city,
    country: row.country,
    latitude: Number(row.latitude),
    longitude: Number(row.longitude),
    distanceKm: Number(row.distanceKm),
    imageUrl: row.imageUrl,
    website: row.website,
    address: row.address,
    foundedYear: row.foundedYear,
  }));
  const last = pageRows.at(-1);
  const nextCursor = hasMore && last
    ? encodeMuseumCursor({
        version: 1,
        sort: query.sort,
        key: query.sort === "distance" ? Number(last.distanceKm) : last.sortName,
        id: last.wikidataId,
        scope,
      })
    : null;

  return { museums, nextCursor, hasMore };
}

export async function claimMuseumSyncLease(
  token: string,
  runId: string,
  leaseSeconds: number,
): Promise<MuseumSyncLease | null> {
  const sql = database();
  const rows = await sql.query(
    `UPDATE museum_sync_state
     SET lock_token = $1,
         locked_until = NOW() + ($3::integer * INTERVAL '1 second'),
         run_id = COALESCE(run_id, $2),
         last_started_at = CASE WHEN run_id IS NULL THEN NOW() ELSE last_started_at END,
         last_error = NULL,
         updated_at = NOW()
     WHERE source = 'wikidata'
       AND (locked_until IS NULL OR locked_until < NOW())
     RETURNING cursor, run_id AS "runId"`,
    [token, runId, leaseSeconds],
  ) as Array<{ cursor: string | null; runId: string }>;
  const state = rows[0];
  return state ? { token, runId: state.runId, cursor: state.cursor } : null;
}

export async function upsertMuseums(
  museums: MuseumRecord[],
  runId: string,
): Promise<void> {
  if (museums.length === 0) return;
  const sql = database();
  await sql.query(
    `INSERT INTO museums (
       wikidata_id, name, description, category, city, country,
       latitude, longitude, image_url, website, address, founded_year,
       source_modified_at, content_hash, last_seen_run_id, is_active
     )
     SELECT
       record.wikidata_id,
       record.name,
       record.description,
       record.category,
       record.city,
       record.country,
       record.latitude,
       record.longitude,
       record.image_url,
       record.website,
       record.address,
       record.founded_year,
       record.source_modified_at,
       record.content_hash,
       $2,
       TRUE
     FROM jsonb_to_recordset($1::jsonb) AS record(
       wikidata_id TEXT,
       name TEXT,
       description TEXT,
       category TEXT,
       city TEXT,
       country TEXT,
       latitude DOUBLE PRECISION,
       longitude DOUBLE PRECISION,
       image_url TEXT,
       website TEXT,
       address TEXT,
       founded_year TEXT,
       source_modified_at TIMESTAMPTZ,
       content_hash TEXT
     )
     ON CONFLICT (wikidata_id) DO UPDATE SET
       name = EXCLUDED.name,
       description = EXCLUDED.description,
       category = EXCLUDED.category,
       city = EXCLUDED.city,
       country = EXCLUDED.country,
       latitude = EXCLUDED.latitude,
       longitude = EXCLUDED.longitude,
       image_url = EXCLUDED.image_url,
       website = EXCLUDED.website,
       address = EXCLUDED.address,
       founded_year = EXCLUDED.founded_year,
       source_modified_at = EXCLUDED.source_modified_at,
       content_hash = EXCLUDED.content_hash,
       last_seen_run_id = EXCLUDED.last_seen_run_id,
       is_active = TRUE,
       updated_at = CASE
         WHEN museums.content_hash IS DISTINCT FROM EXCLUDED.content_hash THEN NOW()
         ELSE museums.updated_at
       END`,
    [JSON.stringify(museums.map(toDatabaseMuseum)), runId],
  );
}

export async function advanceMuseumSync(
  token: string,
  cursor: string,
  leaseSeconds: number,
): Promise<void> {
  const sql = database();
  await sql.query(
    `UPDATE museum_sync_state
     SET cursor = $2,
         locked_until = NOW() + ($3::integer * INTERVAL '1 second'),
         updated_at = NOW()
     WHERE source = 'wikidata' AND lock_token = $1`,
    [token, cursor, leaseSeconds],
  );
}

export async function completeMuseumSync(token: string, runId: string): Promise<void> {
  const sql = database();
  await sql.transaction([
    sql`UPDATE museums
        SET is_active = FALSE, updated_at = NOW()
        WHERE is_active = TRUE
          AND last_seen_run_id <> ${runId}
          AND EXISTS (
            SELECT 1 FROM museum_sync_state
            WHERE source = 'wikidata'
              AND lock_token = ${token}
              AND locked_until > NOW()
          )`,
    sql`UPDATE museum_sync_state
        SET cursor = NULL,
            run_id = NULL,
            lock_token = NULL,
            locked_until = NULL,
            last_completed_at = NOW(),
            last_error = NULL,
            updated_at = NOW()
        WHERE source = 'wikidata'
          AND lock_token = ${token}
          AND locked_until > NOW()`,
  ]);
}

export async function failMuseumSync(token: string, message: string): Promise<void> {
  const sql = database();
  await sql.query(
    `UPDATE museum_sync_state
     SET lock_token = NULL,
         locked_until = NULL,
         last_error = $2,
         updated_at = NOW()
     WHERE source = 'wikidata' AND lock_token = $1`,
    [token, message.slice(0, 2000)],
  );
}

export async function releaseMuseumSyncLease(token: string): Promise<void> {
  const sql = database();
  await sql.query(
    `UPDATE museum_sync_state
     SET lock_token = NULL, locked_until = NULL, updated_at = NOW()
     WHERE source = 'wikidata' AND lock_token = $1`,
    [token],
  );
}

function toDatabaseMuseum(museum: MuseumRecord) {
  return {
    wikidata_id: museum.wikidataId,
    name: museum.name,
    description: museum.description,
    category: museum.category,
    city: museum.city,
    country: museum.country,
    latitude: museum.latitude,
    longitude: museum.longitude,
    image_url: museum.imageUrl,
    website: museum.website,
    address: museum.address,
    founded_year: museum.foundedYear,
    source_modified_at: museum.sourceModifiedAt,
    content_hash: museum.contentHash,
  };
}

function apiCategory(category: string): string {
  const names: Record<string, string> = {
    art: "Art",
    history: "History",
    archaeology: "Archaeology",
    science: "Science",
    natural_history: "NaturalHistory",
    technology: "Technology",
    military: "Military",
    ethnography: "Ethnography",
    maritime: "Maritime",
    other: "Other",
  };
  return names[category] ?? "Other";
}
