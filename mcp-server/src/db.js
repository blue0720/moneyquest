import mysql from "mysql2/promise";

const pool = mysql.createPool({
  host: process.env.MQ_MCP_DB_HOST ?? "localhost",
  port: Number(process.env.MQ_MCP_DB_PORT ?? 3306),
  database: process.env.MQ_MCP_DB_NAME ?? "mqdb",
  user: process.env.MQ_MCP_DB_USER ?? "mquser",
  password: process.env.MQ_MCP_DB_PASSWORD ?? "Rezo_0000",
  waitForConnections: true,
  connectionLimit: 5,
  dateStrings: true,
});

export async function query(sql, params = []) {
  const [rows] = await pool.query(sql, params);
  return rows;
}

export async function execute(sql, params = []) {
  const [result] = await pool.execute(sql, params);
  return result;
}
