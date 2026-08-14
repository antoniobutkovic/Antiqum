import { checkDatabaseConnection } from "@/database";
import { json } from "@/http";

export async function GET(): Promise<Response> {
  try {
    await checkDatabaseConnection();
    return json({ status: "ok" });
  } catch {
    return json({ status: "unavailable" }, 503);
  }
}
