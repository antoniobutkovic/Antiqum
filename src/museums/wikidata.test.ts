import assert from "node:assert/strict";
import test from "node:test";

import { fetchMuseumIds, fetchMuseumRecords } from "./wikidata";

test("discovers museum subclasses such as art and archaeology museums", async () => {
  const originalFetch = globalThis.fetch;
  let query = "";
  globalThis.fetch = async (input) => {
    const url = new URL(input instanceof Request ? input.url : input.toString());
    query = url.searchParams.get("query") ?? "";
    return Response.json({
      results: { bindings: [{ museum: { value: "http://www.wikidata.org/entity/Q19675" } }] },
    });
  };

  try {
    assert.deepEqual(await fetchMuseumIds(null, 50), ["Q19675"]);
    assert.match(query, /wdt:P31\/wdt:P279\* wd:Q33506/);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

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
    if (url.hostname === "commons.wikimedia.org") {
      return Response.json({
        query: {
          pages: [{
            title: "File:Primary museum.jpg",
            imageinfo: [{
              thumburl: "https://upload.wikimedia.org/primary-museum-1600.jpg",
              extmetadata: {
                LicenseShortName: { value: "CC BY-SA 4.0" },
                Artist: { value: "<b>Museum Photographer</b>" },
              },
            }],
          }],
        },
      });
    }
    if (url.hostname === "query.wikidata.org") {
      return Response.json({ results: { bindings: [] } });
    }
    throw new Error(`Unexpected request: ${url}`);
  };

  try {
    const records = await fetchMuseumRecords(["Q1", "Q2"]);
    assert.equal(records[0]?.imageUrl, "https://upload.wikimedia.org/fallback-museum-1200.jpg");
    assert.equal(
      records[1]?.imageUrl,
      "https://upload.wikimedia.org/primary-museum-1600.jpg",
    );
    assert.equal(records[1]?.images[0]?.license, "CC BY-SA 4.0");
    assert.equal(records[1]?.images[0]?.photographer, "Museum Photographer");
    const wikipediaRequest = requestedUrls.find((url) => url.hostname === "en.wikipedia.org");
    assert.equal(wikipediaRequest?.searchParams.get("pilicense"), "free");
    assert.equal(wikipediaRequest?.searchParams.get("titles"), "Fallback Museum");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("formats Wikidata opening-hour qualifiers and official closure dates", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input) => {
    const url = new URL(input instanceof Request ? input.url : input.toString());
    if (url.hostname === "query.wikidata.org") {
      return Response.json({ results: { bindings: [] } });
    }
    if (url.hostname === "commons.wikimedia.org") {
      return Response.json({ query: { pages: [] } });
    }
    if (url.hostname !== "www.wikidata.org") throw new Error(`Unexpected request: ${url}`);
    const requestedIds = url.searchParams.get("ids") ?? "";
    if (requestedIds === "Q10") {
      return Response.json({
        entities: {
          Q10: {
            id: "Q10",
            labels: { en: { value: "Free Data Museum" } },
            claims: {
              P625: [{
                rank: "normal",
                mainsnak: {
                  snaktype: "value",
                  datavalue: { value: { latitude: 48.8566, longitude: 2.3522 } },
                },
              }],
              P3025: [{
                rank: "normal",
                mainsnak: { snaktype: "value", datavalue: { value: { id: "Q26214163" } } },
                qualifiers: {
                  P8626: [{ snaktype: "value", datavalue: { value: { id: "Q55811483" } } }],
                  P8627: [{ snaktype: "value", datavalue: { value: { id: "Q55811883" } } }],
                },
              }],
              P3026: [{
                rank: "normal",
                mainsnak: { snaktype: "value", datavalue: { value: { id: "Q2705" } } },
              }],
              P3999: [{
                rank: "normal",
                mainsnak: { snaktype: "value", datavalue: { value: { time: "+2020-01-01T00:00:00Z" } } },
              }],
            },
          },
        },
      });
    }
    return Response.json({
      entities: {
        Q26214163: { id: "Q26214163", labels: { en: { value: "all days of the week" } } },
        Q55811483: { id: "Q55811483", labels: { en: { value: "10:00" } } },
        Q55811883: { id: "Q55811883", labels: { en: { value: "17:00" } } },
        Q2705: { id: "Q2705", labels: { en: { value: "24 December" } } },
      },
    });
  };

  try {
    const records = await fetchMuseumRecords(["Q10"]);
    assert.deepEqual(records[0]?.regularOpeningHours, [
      "all days of the week: 10:00 – 17:00",
      "Closed: 24 December",
    ]);
    assert.equal(records[0]?.closureStatus, "Closed in 2020");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("uses a linked Commons category when P18 and Wikipedia images are unavailable", async () => {
  const originalFetch = globalThis.fetch;
  let requestedCategory: string | null = null;
  globalThis.fetch = async (input) => {
    const url = new URL(input instanceof Request ? input.url : input.toString());
    if (url.hostname === "www.wikidata.org") {
      return Response.json({
        entities: {
          Q20: {
            id: "Q20",
            labels: { en: { value: "Commons Category Museum" } },
            claims: {
              P373: [{
                rank: "normal",
                mainsnak: { snaktype: "value", datavalue: { value: "Commons Category Museum" } },
              }],
              P625: [{
                rank: "normal",
                mainsnak: {
                  snaktype: "value",
                  datavalue: { value: { latitude: 48.8606, longitude: 2.3376 } },
                },
              }],
            },
          },
        },
      });
    }
    if (url.hostname === "query.wikidata.org") {
      return Response.json({ results: { bindings: [] } });
    }
    if (url.hostname === "commons.wikimedia.org") {
      requestedCategory = url.searchParams.get("gcmtitle");
      return Response.json({
        query: {
          pages: [
            {
              title: "File:Commons Category Museum logo.png",
              imageinfo: [{
                thumburl: "https://upload.wikimedia.org/museum-logo.png",
                mime: "image/png",
              }],
            },
            {
              title: "File:Commons Category Museum facade.jpg",
              imageinfo: [{
                thumburl: "https://upload.wikimedia.org/museum-facade-1600.jpg",
                mime: "image/jpeg",
                extmetadata: {
                  LicenseShortName: { value: "CC BY 4.0" },
                  Artist: { value: "Category Photographer" },
                },
              }],
            },
          ],
        },
      });
    }
    throw new Error(`Unexpected request: ${url}`);
  };

  try {
    const records = await fetchMuseumRecords(["Q20"]);
    assert.equal(requestedCategory, "Category:Commons Category Museum");
    assert.equal(records[0]?.imageUrl, "https://upload.wikimedia.org/museum-facade-1600.jpg");
    assert.equal(records[0]?.images[0]?.source, "wikimedia_commons");
    assert.equal(records[0]?.images[0]?.license, "CC BY 4.0");
    assert.equal(records[0]?.images[0]?.photographer, "Category Photographer");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("uses Commons structured-data depictions when no image links exist", async () => {
  const originalFetch = globalThis.fetch;
  let commonsSearch: string | null = null;
  globalThis.fetch = async (input) => {
    const url = new URL(input instanceof Request ? input.url : input.toString());
    if (url.hostname === "www.wikidata.org") {
      return Response.json({
        entities: {
          Q30: {
            id: "Q30",
            labels: { en: { value: "Depicted Museum" } },
            claims: {
              P625: [{
                rank: "normal",
                mainsnak: {
                  snaktype: "value",
                  datavalue: { value: { latitude: 48.861, longitude: 2.336 } },
                },
              }],
            },
          },
        },
      });
    }
    if (url.hostname === "query.wikidata.org") {
      return Response.json({ results: { bindings: [] } });
    }
    if (url.hostname === "commons.wikimedia.org") {
      commonsSearch = url.searchParams.get("gsrsearch");
      return Response.json({
        query: {
          pages: [{
            title: "File:Depicted Museum exterior.webp",
            imageinfo: [{
              thumburl: "https://upload.wikimedia.org/depicted-museum-1600.webp",
              mime: "image/webp",
              extmetadata: { LicenseShortName: { value: "CC0" } },
            }],
          }],
        },
      });
    }
    throw new Error(`Unexpected request: ${url}`);
  };

  try {
    const records = await fetchMuseumRecords(["Q30"]);
    assert.equal(commonsSearch, "haswbstatement:P180=Q30");
    assert.equal(records[0]?.imageUrl, "https://upload.wikimedia.org/depicted-museum-1600.webp");
    assert.equal(records[0]?.images[0]?.license, "CC0");
  } finally {
    globalThis.fetch = originalFetch;
  }
});
