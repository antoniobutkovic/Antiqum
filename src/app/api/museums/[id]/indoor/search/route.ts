import { LOUVRE_MUSEUM_ID } from "@/louvre/data";
import { searchLouvreLocations } from "@/louvre/routing";

interface IndoorSearchContext {
  params: Promise<{ id: string }>;
}

export async function GET(request: Request, context: IndoorSearchContext): Promise<Response> {
  const { id } = await context.params;
  if (id !== LOUVRE_MUSEUM_ID) return Response.json({ error: "Indoor navigation is not available for this museum" }, { status: 404 });
  return Response.json(searchLouvreLocations(new URL(request.url).searchParams.get("q") ?? ""), {
    headers: { "Cache-Control": "public, max-age=60, s-maxage=300" },
  });
}
