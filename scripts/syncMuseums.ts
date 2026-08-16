import { syncMuseumCatalog } from "../src/museums/sync";

let total = 0;
for (;;) {
  const result = await syncMuseumCatalog({
    batchSize: 100,
    maxBatches: 25,
    maxRuntimeMs: 240_000,
    leaseSeconds: 300,
  });
  total += result.processed;
  console.log(JSON.stringify({ ...result, totalProcessed: total }));
  if (result.status === "completed") break;
  if (result.status === "busy") {
    throw new Error("Another museum synchronization is already running");
  }
}
