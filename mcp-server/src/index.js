import path from "node:path";
import { fileURLToPath } from "node:url";
import dotenv from "dotenv";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

// 起動時のカレントディレクトリに関わらず mcp-server/.env を読み込む
const __dirname = path.dirname(fileURLToPath(import.meta.url));
// quiet: dotenv の起動ログがstdoutに出ると MCP の JSON-RPC ストリームを壊すため必ず抑制する
dotenv.config({ path: path.join(__dirname, "..", ".env"), quiet: true });

import { registerUserTools } from "./tools/users.js";
import { registerQuestTools } from "./tools/quests.js";
import { registerQuestTemplateTools } from "./tools/questTemplates.js";
import { registerIncomeExpenseTools } from "./tools/incomeExpense.js";
import { registerSpendingLimitTools } from "./tools/spendingLimits.js";
import { registerCharacterTools } from "./tools/characters.js";

const server = new McpServer({
  name: "moneyquest-mcp",
  version: "0.1.0",
});

registerUserTools(server);
registerQuestTools(server);
registerQuestTemplateTools(server);
registerIncomeExpenseTools(server);
registerSpendingLimitTools(server);
registerCharacterTools(server);

const transport = new StdioServerTransport();
await server.connect(transport);
