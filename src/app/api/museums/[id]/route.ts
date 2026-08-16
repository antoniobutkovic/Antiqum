import { getMuseumDetails } from "@/museums/database";

interface MuseumDetailsRouteContext {
  params: Promise<{ id: string }>;
}

export async function GET(request: Request, context: MuseumDetailsRouteContext): Promise<Response> {
  const { id } = await context.params;
  if (!/^Q[0-9]+$/.test(id)) {
    return Response.json({ error: "Invalid museum ID" }, { status: 400 });
  }
  const parameters = new URL(request.url).searchParams;
  const latitude = coordinate(parameters.get("latitude"), 45.815, -90, 90);
  const longitude = coordinate(parameters.get("longitude"), 15.9819, -180, 180);
  if (latitude === null || longitude === null) {
    return Response.json({ error: "Invalid reference coordinates" }, { status: 400 });
  }
  try {
    const museum = await getMuseumDetails(id, latitude, longitude);
    if (!museum) return Response.json({ error: "Museum not found" }, { status: 404 });
    return Response.json(museum, {
      headers: { "Cache-Control": "public, max-age=300, s-maxage=3600, stale-while-revalidate=86400" },
    });
  } catch (error) {
    console.error("Unable to load museum details", error);
    return Response.json({ error: "Unable to load museum details" }, { status: 503 });
  }
}

function coordinate(raw: string | null, fallback: number, minimum: number, maximum: number): number | null {
  if (raw === null) return fallback;
  const value = Number(raw);
  return Number.isFinite(value) && value >= minimum && value <= maximum ? value : null;
}
