import assert from "node:assert/strict";
import test from "node:test";

import { fetchMuseumRecords } from "./wikidata";

test("uses a linked Wikipedia page image when P18 is missing", async () => {
  const originalFetch = globalThis.fetch;
  const requestedUrls: URL[] = [];
  globalThis.fetch = async (input) => {
    const url = new URL(input instanceof Request ? input.url : input.toString());
    requestedUrls.push(url);
    if (url.hostname === "www.wikidata.org") {
      return Response.json({
        entities: {
          Q1: {
            id: "Q1",
            labels: { en: { value: "Fallback Museum" } },
            descriptions: { en: { value: "museum with a Wikipedia image" } },
            claims: {
              P625: [{
                rank: "normal",
                mainsnak: {
                  snaktype: "value",
                  datavalue: { value: { latitude: 48.8566, longitude: 2.3522 } },
                },
              }],
            },
            sitelinks: {
              enwiki: {
                title: "Fallback Museum",
                url: "https://en.wikipedia.org/wiki/Fallback_Museum",
              },
            },
          },
          Q2: {
            id: "Q2",
            labels: { en: { value: "P18 Museum" } },
            claims: {
              P18: [{
                rank: "normal",
                mainsnak: {
                  snaktype: "value",
                  datavalue: { value: "Primary museum.jpg" },
                },
              }],
              P625: [{
                rank: "normal",
                mainsnak: {
                  snaktype: "value",
                  datavalue: { value: { latitude: 51.5, longitude: -0.1 } },
                },
              }],
            },
          },
        },
      });
    }
    if (url.hostname === "en.wikipedia.org") {
      return Response.json({
        query: {
          pages: [{
            title: "Fallback Museum",
            thumbnail: { source: "https://upload.wikimedia.org/fallback-museum-1200.jpg" },
          }],
        },
      });
    }
    throw new Error(`Unexpected request: ${url}`);
  };

  try {
    const records = await fetchMuseumRecords(["Q1", "Q2"]);
    assert.equal(records[0]?.imageUrl, "https://upload.wikimedia.org/fallback-museum-1200.jpg");
    assert.equal(
      records[1]?.imageUrl,
      "https://commons.wikimedia.org/wiki/Special:FilePath/Primary%20museum.jpg",
    );
    const wikipediaRequest = requestedUrls.find((url) => url.hostname === "en.wikipedia.org");
    assert.equal(wikipediaRequest?.searchParams.get("pilicense"), "free");
    assert.equal(wikipediaRequest?.searchParams.get("titles"), "Fallback Museum");
  } finally {
    globalThis.fetch = originalFetch;
  }
});
