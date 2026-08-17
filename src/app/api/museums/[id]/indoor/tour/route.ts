import { LOUVRE_MUSEUM_ID } from "@/louvre/data";
import { calculateLouvreTour, LouvreRoutingError } from "@/louvre/routing";
import type { LouvreTourRequest } from "@/louvre/types";

interface IndoorTourContext {
  params: Promise<{ id: string }>;
}

export async function POST(request: Request, context: IndoorTourContext): Promise<Response> {
  const { id } = await context.params;
  if (id !== LOUVRE_MUSEUM_ID) return Response.json({ error: "Indoor navigation is not available for this museum" }, { status: 404 });
  try {
    return Response.json(calculateLouvreTour(await request.json() as LouvreTourRequest), {
      headers: { "Cache-Control": "no-store" },
    });
  } catch (error) {
    const status = error instanceof LouvreRoutingError ? error.status : 400;
    return Response.json({ error: error instanceof Error ? error.message : "Unable to optimize tour" }, { status });
  }
}
