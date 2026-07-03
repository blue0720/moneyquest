import { z } from "zod";
import { query, execute } from "../db.js";
import { textResult, withErrorHandling } from "../util.js";

const STATUS_DESC = "requestStatus: 0=申請中, 1=承認済み, 2=却下";

export function registerSpendingLimitTools(server) {
  server.registerTool(
    "list_spending_limits",
    {
      title: "支出上限申請一覧取得",
      description: `spending_limit_t から支出上限申請の一覧を取得する。${STATUS_DESC}`,
      inputSchema: {
        childUserId: z.number().int().optional(),
        requestStatus: z.number().int().min(0).max(2).optional(),
      },
    },
    withErrorHandling(async ({ childUserId, requestStatus }) => {
      const conditions = [];
      const params = [];
      if (childUserId !== undefined) {
        conditions.push("child_user_id = ?");
        params.push(childUserId);
      }
      if (requestStatus !== undefined) {
        conditions.push("request_status = ?");
        params.push(requestStatus);
      }
      const where = conditions.length ? `WHERE ${conditions.join(" AND ")}` : "";
      const rows = await query(
        `SELECT * FROM spending_limit_t ${where} ORDER BY registered_date DESC`,
        params
      );
      return textResult(rows);
    })
  );

  server.registerTool(
    "create_spending_limit",
    {
      title: "支出上限の新規申請",
      description: "子供が支出上限金額目標を新規申請する。requestStatus は既定で 0(申請中) になる。",
      inputSchema: {
        childUserId: z.number().int(),
        limitAmount: z.number().int().min(0),
      },
    },
    withErrorHandling(async ({ childUserId, limitAmount }) => {
      const result = await execute(
        `INSERT INTO spending_limit_t (child_user_id, limit_amount, request_status)
         VALUES (?, ?, 0)`,
        [childUserId, limitAmount]
      );
      return textResult({ spendingLimitId: result.insertId, message: "申請を作成しました。" });
    })
  );

  server.registerTool(
    "update_spending_limit",
    {
      title: "支出上限申請の更新(承認/却下含む)",
      description: `既存の支出上限申請を更新する(削除は不可)。承認/却下も requestStatus の更新で行う。${STATUS_DESC}`,
      inputSchema: {
        spendingLimitId: z.number().int(),
        limitAmount: z.number().int().min(0).optional(),
        requestStatus: z.number().int().min(0).max(2).optional(),
      },
    },
    withErrorHandling(async ({ spendingLimitId, limitAmount, requestStatus }) => {
      const sets = [];
      const params = [];
      if (limitAmount !== undefined) {
        sets.push("limit_amount = ?");
        params.push(limitAmount);
      }
      if (requestStatus !== undefined) {
        sets.push("request_status = ?");
        params.push(requestStatus);
      }
      if (sets.length === 0) {
        return textResult({ error: "更新する項目がありません。" });
      }
      params.push(spendingLimitId);
      const result = await execute(
        `UPDATE spending_limit_t SET ${sets.join(", ")}, updated_date = NOW() WHERE spending_limit_id = ?`,
        params
      );
      if (result.affectedRows === 0) {
        return textResult({ error: `spending_limit_id=${spendingLimitId} は存在しません。` });
      }
      return textResult({ spendingLimitId, message: "更新しました。" });
    })
  );
}
