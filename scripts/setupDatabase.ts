import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

import { neon } from "@neondatabase/serverless";

import { requireEnvironment } from "../src/config";

const schemaUrl = new URL("../db/schema.sql", import.meta.url);
const schema = await readFile(fileURLToPath(schemaUrl), "utf8");
const statements = schema
  .split(";")
  .map((statement) => statement.trim())
  .filter(Boolean);
const sql = neon(requireEnvironment("DATABASE_URL"));

await sql.transaction((transaction) =>
  statements.map((statement) => transaction.query(statement)),
);

console.log("Database schema is ready");
