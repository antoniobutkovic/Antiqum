import { createHash } from "node:crypto";

import type { MuseumPageQuery, MuseumSort } from "./types";

interface CursorPayload {
  version: 1;
  sort: MuseumSort;
  key: number | string;
  id: string;
  scope: string;
}

export class InvalidMuseumCursorError extends Error {}

export function museumQueryScope(query: Omit<MuseumPageQuery, "cursor" | "limit">): string {
  return createHash("sha256")
    .update(
      JSON.stringify({
        latitude: query.latitude,
        longitude: query.longitude,
        radiusKm: query.radiusKm,
        sort: query.sort,
        category: query.category,
        search: query.search,
      }),
    )
    .digest("base64url");
}

export function encodeMuseumCursor(payload: CursorPayload): string {
  return Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
}

export function decodeMuseumCursor(
  cursor: string,
  expectedSort: MuseumSort,
  expectedScope: string,
): CursorPayload {
  try {
    const value = JSON.parse(Buffer.from(cursor, "base64url").toString("utf8")) as Partial<CursorPayload>;
    const keyIsValid = expectedSort === "distance"
      ? typeof value.key === "number" && Number.isFinite(value.key)
      : typeof value.key === "string" && value.key.length > 0;
    if (
      value.version !== 1 ||
      value.sort !== expectedSort ||
      value.scope !== expectedScope ||
      typeof value.id !== "string" ||
      !/^Q[0-9]+$/.test(value.id) ||
      !keyIsValid
    ) {
      throw new InvalidMuseumCursorError("Cursor does not match this museum query");
    }
    return value as CursorPayload;
  } catch (error) {
    if (error instanceof InvalidMuseumCursorError) throw error;
    throw new InvalidMuseumCursorError("Invalid museum cursor");
  }
}
