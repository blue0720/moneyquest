import { z } from "zod";
import { query, execute } from "../db.js";
import { textResult, withErrorHandling } from "../util.js";

const RECORD_TYPE_DESC = "recordType: 0=収入, 1=支出";

export function registerIncomeExpenseTools(server) {
  server.registerTool(
    "list_income_expense",
    {
      title: "収支記録一覧取得",
      description: `income_expense_t から収支記録一覧を取得する。${RECORD_TYPE_DESC}`,
      inputSchema: {
        childUserId: z.number().int().optional(),
        recordType: z.number().int().min(0).max(1).optional(),
      },
    },
    withErrorHandling(async ({ childUserId, recordType }) => {
      const conditions = [];
      const params = [];
      if (childUserId !== undefined) {
        conditions.push("child_user_id = ?");
        params.push(childUserId);
      }
      if (recordType !== undefined) {
        conditions.push("record_type = ?");
        params.push(recordType);
      }
      const where = conditions.length ? `WHERE ${conditions.join(" AND ")}` : "";
      const rows = await query(
        `SELECT * FROM income_expense_t ${where} ORDER BY registered_date DESC`,
        params
      );
      return textResult(rows);
    })
  );

  server.registerTool(
    "create_income_expense",
    {
      title: "収支記録の新規作成",
      description: `収入または支出の記録を新規作成する。${RECORD_TYPE_DESC}`,
      inputSchema: {
        childUserId: z.number().int(),
        recordType: z.number().int().min(0).max(1),
        category: z.string().max(50).optional(),
        amount: z.number().int().min(0),
        memo: z.string().max(200).optional(),
      },
    },
    withErrorHandling(async ({ childUserId, recordType, category, amount, memo }) => {
      const result = await execute(
        `INSERT INTO income_expense_t (child_user_id, record_type, category, amount, memo)
         VALUES (?, ?, ?, ?, ?)`,
        [childUserId, recordType, category ?? null, amount, memo ?? null]
      );
      return textResult({ incomeExpenseId: result.insertId, message: "作成しました。" });
    })
  );

  server.registerTool(
    "update_income_expense",
    {
      title: "収支記録の更新",
      description: "既存の収支記録を更新する(削除は不可)。",
      inputSchema: {
        incomeExpenseId: z.number().int(),
        category: z.string().max(50).optional(),
        amount: z.number().int().min(0).optional(),
        memo: z.string().max(200).optional(),
      },
    },
    withErrorHandling(async ({ incomeExpenseId, category, amount, memo }) => {
      const sets = [];
      const params = [];
      if (category !== undefined) {
        sets.push("category = ?");
        params.push(category);
      }
      if (amount !== undefined) {
        sets.push("amount = ?");
        params.push(amount);
      }
      if (memo !== undefined) {
        sets.push("memo = ?");
        params.push(memo);
      }
      if (sets.length === 0) {
        return textResult({ error: "更新する項目がありません。" });
      }
      params.push(incomeExpenseId);
      const result = await execute(
        `UPDATE income_expense_t SET ${sets.join(", ")} WHERE income_expense_id = ?`,
        params
      );
      if (result.affectedRows === 0) {
        return textResult({ error: `income_expense_id=${incomeExpenseId} は存在しません。` });
      }
      return textResult({ incomeExpenseId, message: "更新しました。" });
    })
  );
}
