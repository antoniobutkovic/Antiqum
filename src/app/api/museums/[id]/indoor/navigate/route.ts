import { LOUVRE_MUSEUM_ID } from "@/louvre/data";
import { calculateLouvreRoute, LouvreRoutingError } from "@/louvre/routing";
import type { LouvreRouteRequest } from "@/louvre/types";

interface IndoorNavigateContext {
  params: Promise<{ id: string }>;
}

export async function POST(request: Request, context: IndoorNavigateContext): Promise<Response> {
  const { id } = await context.params;
  if (id !== LOUVRE_MUSEUM_ID) return Response.json({ error: "Indoor navigation is not available for this museum" }, { status: 404 });
  try {
    return Response.json(calculateLouvreRoute(await request.json() as LouvreRouteRequest), {
      headers: { "Cache-Control": "no-store" },
    });
  } catch (error) {
    const status = error instanceof LouvreRoutingError ? error.status : 400;
    return Response.json({ error: error instanceof Error ? error.message : "Unable to calculate route" }, { status });
  }
}
