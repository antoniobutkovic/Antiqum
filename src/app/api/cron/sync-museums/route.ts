import { timingSafeEqual } from "node:crypto";

import { requireEnvironment } from "@/config";
import { syncMuseumCatalog } from "@/museums/sync";

export const maxDuration = 60;

export async function GET(request: Request): Promise<Response> {
  let secret: string;
  try {
    secret = requireEnvironment("CRON_SECRET");
  } catch {
    return Response.json({ error: "Museum synchronization is not configured" }, { status: 503 });
  }

  if (!authorized(request.headers.get("authorization"), secret)) {
    return Response.json({ error: "Unauthorized" }, { status: 401 });
  }

  try {
    const result = await syncMuseumCatalog();
    return Response.json(result, {
      status: result.status === "busy" ? 202 : 200,
      headers: { "Cache-Control": "no-store" },
    });
  } catch (error) {
    console.error("Museum synchronization failed", error);
    return Response.json({ error: "Museum synchronization failed" }, { status: 503 });
  }
}

function authorized(authorization: string | null, secret: string): boolean {
  if (!authorization?.startsWith("Bearer ")) return false;
  const supplied = Buffer.from(authorization.slice("Bearer ".length));
  const expected = Buffer.from(secret);
  return supplied.length === expected.length && timingSafeEqual(supplied, expected);
}
