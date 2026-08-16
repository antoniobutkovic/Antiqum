import { randomUUID } from "node:crypto";

import {
  advanceMuseumSync,
  claimMuseumSyncLease,
  completeMuseumSync,
  failMuseumSync,
  releaseMuseumSyncLease,
  upsertMuseums,
} from "./database";
import { fetchMuseumIds, fetchMuseumRecords } from "./wikidata";

export interface MuseumSyncResult {
  status: "busy" | "incomplete" | "completed";
  runId?: string;
  processed: number;
  cursor?: string | null;
}

interface MuseumSyncOptions {
  batchSize?: number;
  maxBatches?: number;
  maxRuntimeMs?: number;
  leaseSeconds?: number;
}

export async function syncMuseumCatalog(
  options: MuseumSyncOptions = {},
): Promise<MuseumSyncResult> {
  const batchSize = Math.min(Math.max(options.batchSize ?? 50, 1), 500);
  const maxBatches = Math.max(options.maxBatches ?? 4, 1);
  const maxRuntimeMs = Math.max(options.maxRuntimeMs ?? 45_000, 5_000);
  const leaseSeconds = Math.max(options.leaseSeconds ?? 180, 30);
  const token = randomUUID();
  const lease = await claimMuseumSyncLease(token, randomUUID(), leaseSeconds);
  if (!lease) return { status: "busy", processed: 0 };

  const deadline = Date.now() + maxRuntimeMs;
  let cursor = lease.cursor;
  let processed = 0;
  let released = false;
  try {
    for (let batch = 0; batch < maxBatches && Date.now() < deadline; batch += 1) {
      const ids = await fetchMuseumIds(cursor, batchSize);
      if (ids.length === 0) {
        await completeMuseumSync(token, lease.runId);
        released = true;
        return { status: "completed", runId: lease.runId, processed, cursor: null };
      }

      const museums = await fetchMuseumRecords(ids);
      await upsertMuseums(museums, lease.runId);
      cursor = ids.at(-1) ?? cursor;
      if (!cursor) throw new Error("Wikidata returned a page without a synchronization cursor");
      await advanceMuseumSync(token, cursor, leaseSeconds);
      processed += museums.length;

      if (ids.length < batchSize) {
        await completeMuseumSync(token, lease.runId);
        released = true;
        return { status: "completed", runId: lease.runId, processed, cursor: null };
      }
    }

    await releaseMuseumSyncLease(token);
    released = true;
    return { status: "incomplete", runId: lease.runId, processed, cursor };
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    await failMuseumSync(token, message);
    released = true;
    throw error;
  } finally {
    if (!released) await releaseMuseumSyncLease(token);
  }
}
