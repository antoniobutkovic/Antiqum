import { neon } from "@neondatabase/serverless";

import { requireEnvironment } from "./config";

export interface DatabaseCategory {
  id: number;
  name: string;
  createdAt: string;
}

function database() {
  return neon(requireEnvironment("DATABASE_URL"));
}

export async function checkDatabaseConnection(): Promise<void> {
  const sql = database();
  await sql`SELECT 1`;
}

export async function listCategories(): Promise<DatabaseCategory[]> {
  const sql = database();
  const rows = await sql`
    SELECT
      id,
      name,
      created_at AS "createdAt"
    FROM categories
    ORDER BY sort_order, LOWER(name), id
  `;
  return rows as DatabaseCategory[];
}
