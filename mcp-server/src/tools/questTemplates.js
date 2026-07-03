import { z } from "zod";
import { query, execute } from "../db.js";
import { textResult, withErrorHandling } from "../util.js";

export function registerQuestTemplateTools(server) {
  server.registerTool(
    "list_quest_templates",
    {
      title: "クエストテンプレート一覧取得",
      description: "quest_template_t からクエストテンプレート一覧を取得する。",
      inputSchema: {
        parentUserId: z.number().int().optional(),
      },
    },
    withErrorHandling(async ({ parentUserId }) => {
      const where = parentUserId !== undefined ? "WHERE parent_user_id = ?" : "";
      const params = parentUserId !== undefined ? [parentUserId] : [];
      const rows = await query(
        `SELECT * FROM quest_template_t ${where} ORDER BY quest_template_id`,
        params
      );
      return textResult(rows);
    })
  );

  server.registerTool(
    "create_quest_template",
    {
      title: "クエストテンプレート新規作成",
      description: "保護者がよく使うクエストをテンプレートとして登録する。",
      inputSchema: {
        parentUserId: z.number().int(),
        title: z.string().min(1).max(100),
        description: z.string().max(200).optional(),
        rewardAmount: z.number().int().min(0).default(0),
        exp: z.number().int().min(0).default(5),
      },
    },
    withErrorHandling(async ({ parentUserId, title, description, rewardAmount, exp }) => {
      const trimmedTitle = title.trim();
      if (trimmedTitle.length === 0) {
        return textResult({ error: "title は空白のみでは登録できません。" });
      }
      const result = await execute(
        `INSERT INTO quest_template_t (parent_user_id, title, description, reward_amount, exp)
         VALUES (?, ?, ?, ?, ?)`,
        [parentUserId, trimmedTitle, description ?? null, rewardAmount ?? 0, exp ?? 5]
      );
      return textResult({ questTemplateId: result.insertId, message: "作成しました。" });
    })
  );

  server.registerTool(
    "update_quest_template",
    {
      title: "クエストテンプレート更新",
      description: "既存のクエストテンプレートを更新する(削除は不可)。",
      inputSchema: {
        questTemplateId: z.number().int(),
        title: z.string().min(1).max(100).optional(),
        description: z.string().max(200).optional(),
        rewardAmount: z.number().int().min(0).optional(),
        exp: z.number().int().min(0).optional(),
      },
    },
    withErrorHandling(async ({ questTemplateId, title, description, rewardAmount, exp }) => {
      const sets = [];
      const params = [];
      if (title !== undefined) {
        if (title.trim().length === 0) {
          return textResult({ error: "title は空白のみでは更新できません。" });
        }
        sets.push("title = ?");
        params.push(title.trim());
      }
      if (description !== undefined) {
        sets.push("description = ?");
        params.push(description);
      }
      if (rewardAmount !== undefined) {
        sets.push("reward_amount = ?");
        params.push(rewardAmount);
      }
      if (exp !== undefined) {
        sets.push("exp = ?");
        params.push(exp);
      }
      if (sets.length === 0) {
        return textResult({ error: "更新する項目がありません。" });
      }
      params.push(questTemplateId);
      const result = await execute(
        `UPDATE quest_template_t SET ${sets.join(", ")} WHERE quest_template_id = ?`,
        params
      );
      if (result.affectedRows === 0) {
        return textResult({ error: `quest_template_id=${questTemplateId} は存在しません。` });
      }
      return textResult({ questTemplateId, message: "更新しました。" });
    })
  );
}
