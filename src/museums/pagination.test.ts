import assert from "node:assert/strict";
import test from "node:test";

import {
  decodeMuseumCursor,
  encodeMuseumCursor,
  InvalidMuseumCursorError,
  museumQueryScope,
} from "./pagination";

const query = {
  latitude: 45.815,
  longitude: 15.9819,
  radiusKm: null,
  sort: "distance" as const,
  category: null,
  search: null,
};

test("museum cursors round-trip only within the same query scope", () => {
  const scope = museumQueryScope(query);
  const cursor = encodeMuseumCursor({
    version: 1,
    sort: "distance",
    key: 12.5,
    id: "Q123",
    scope,
  });

  assert.deepEqual(decodeMuseumCursor(cursor, "distance", scope), {
    version: 1,
    sort: "distance",
    key: 12.5,
    id: "Q123",
    scope,
  });
  assert.throws(
    () => decodeMuseumCursor(cursor, "distance", museumQueryScope({ ...query, search: "art" })),
    InvalidMuseumCursorError,
  );
});

test("museum cursors reject malformed input", () => {
  assert.throws(
    () => decodeMuseumCursor("not-a-cursor", "distance", museumQueryScope(query)),
    InvalidMuseumCursorError,
  );
});
