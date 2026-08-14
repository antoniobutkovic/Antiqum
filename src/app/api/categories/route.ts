import { listCategories } from "@/database";
import { json } from "@/http";

export async function GET(): Promise<Response> {
  try {
    const categories = await listCategories();
    return json({ categories });
  } catch {
    return json({ error: "Unable to load categories" }, 503);
  }
}
