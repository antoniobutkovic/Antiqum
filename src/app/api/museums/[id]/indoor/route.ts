import { indoorBootstrap, LOUVRE_MUSEUM_ID } from "@/louvre/data";

interface IndoorRouteContext {
  params: Promise<{ id: string }>;
}

export async function GET(_request: Request, context: IndoorRouteContext): Promise<Response> {
  const { id } = await context.params;
  if (id !== LOUVRE_MUSEUM_ID) return Response.json({ error: "Indoor navigation is not available for this museum" }, { status: 404 });
  return Response.json(indoorBootstrap(), {
    headers: { "Cache-Control": "public, max-age=300, s-maxage=3600, stale-while-revalidate=86400" },
  });
}
