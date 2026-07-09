import { z } from "zod";
import { query, execute } from "../db.js";
import { textResult, withErrorHandling } from "../util.js";

const STATUS_DESC =
  "status: 0=未完了(既読), 1=完了申請中(承認待ち), 2=承認済み, 3=却下, 4=新規(未読), 5=更新(未読)";

const AVAILABLE_DAYS_DESC =
  "availableDays: 実施可能曜日のビットマスク(月=1,火=2,水=4,木=8,金=16,土=32,日=64を合計)。127=毎日実施可能(既定値)。";

const SPECIFIC_DATE_DESC =
  "specificDate: 実施可能な特定の日付(YYYY-MM-DD、任意)。availableDaysと両方設定されている場合はAND条件(両方を満たす日のみ実施可能)。未設定(null)なら日付による制限なし。";

const COIN_REWARD_DESC =
  "coinReward: クエスト承認時にrewardAmount/expと並行して付与されるコイン報酬(任意、既定値0)。コインはcharacter_t.coin_balanceに加算され、コインショップ(character_item_t)での購入に使える。";

export function registerQuestTools(server) {
  server.registerTool(
    "list_quests",
    {
      title: "クエスト一覧取得",
      description: `quest_t からクエスト一覧を取得する。${STATUS_DESC} ${AVAILABLE_DAYS_DESC} ${SPECIFIC_DATE_DESC} ${COIN_REWARD_DESC}`,
      inputSchema: {
        childUserId: z.number().int().optional().describe("この子供に紐づくクエストのみ取得"),
        status: z.number().int().min(0).max(5).optional(),
      },
    },
    withErrorHandling(async ({ childUserId, status }) => {
      const conditions = [];
      const params = [];
      if (childUserId !== undefined) {
        conditions.push("child_user_id = ?");
        params.push(childUserId);
      }
      if (status !== undefined) {
        conditions.push("status = ?");
        params.push(status);
      }
      const where = conditions.length ? `WHERE ${conditions.join(" AND ")}` : "";
      const rows = await query(
        `SELECT * FROM quest_t ${where} ORDER BY quest_id DESC`,
        params
      );
      return textResult(rows);
    })
  );

  server.registerTool(
    "get_quest",
    {
      title: "クエスト詳細取得",
      description: "quest_id を指定してクエスト1件を取得する。",
      inputSchema: { questId: z.number().int() },
    },
    withErrorHandling(async ({ questId }) => {
      const rows = await query("SELECT * FROM quest_t WHERE quest_id = ?", [questId]);
      if (rows.length === 0) {
        return textResult({ error: `quest_id=${questId} は存在しません。` });
      }
      return textResult(rows[0]);
    })
  );

  server.registerTool(
    "create_quest",
    {
      title: "クエスト新規作成",
      description:
        `保護者が子供にクエストを送信する。status は既定で 4(新規/未読) になる。${AVAILABLE_DAYS_DESC} ${SPECIFIC_DATE_DESC} ${COIN_REWARD_DESC}`,
      inputSchema: {
        childUserId: z.number().int(),
        title: z.string().min(1).max(100),
        description: z.string().max(200).optional(),
        rewardAmount: z.number().int().min(0).default(0),
        exp: z.number().int().min(0).default(5),
        coinReward: z.number().int().min(0).default(0),
        limitAmount: z.number().int().optional(),
        availableDays: z.number().int().min(0).max(127).default(127),
        specificDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
      },
    },
    withErrorHandling(async ({ childUserId, title, description, rewardAmount, exp, coinReward, limitAmount, availableDays, specificDate }) => {
      const trimmedTitle = title.trim();
      if (trimmedTitle.length === 0) {
        return textResult({ error: "title は空白のみでは登録できません。" });
      }
      const result = await execute(
        `INSERT INTO quest_t (child_user_id, title, description, reward_amount, exp, coin_reward, limit_amount, available_days, specific_date, status)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 4)`,
        [childUserId, trimmedTitle, description ?? null, rewardAmount ?? 0, exp ?? 5, coinReward ?? 0, limitAmount ?? null, availableDays ?? 127, specificDate ?? null]
      );
      return textResult({ questId: result.insertId, message: "作成しました。" });
    })
  );

  server.registerTool(
    "update_quest",
    {
      title: "クエスト更新",
      description: `既存クエストを更新する(削除は不可)。${STATUS_DESC} ${AVAILABLE_DAYS_DESC} ${SPECIFIC_DATE_DESC} ${COIN_REWARD_DESC} specificDateにnullを渡すと日付指定を解除できる。`,
      inputSchema: {
        questId: z.number().int(),
        title: z.string().min(1).max(100).optional(),
        description: z.string().max(200).optional(),
        rewardAmount: z.number().int().min(0).optional(),
        exp: z.number().int().min(0).optional(),
        coinReward: z.number().int().min(0).optional(),
        limitAmount: z.number().int().optional(),
        availableDays: z.number().int().min(0).max(127).optional(),
        specificDate: z.union([z.string().regex(/^\d{4}-\d{2}-\d{2}$/), z.null()]).optional(),
        status: z.number().int().min(0).max(5).optional(),
      },
    },
    withErrorHandling(
      async ({ questId, title, description, rewardAmount, exp, coinReward, limitAmount, availableDays, specificDate, status }) => {
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
        if (coinReward !== undefined) {
          sets.push("coin_reward = ?");
          params.push(coinReward);
        }
        if (limitAmount !== undefined) {
          sets.push("limit_amount = ?");
          params.push(limitAmount);
        }
        if (availableDays !== undefined) {
          sets.push("available_days = ?");
          params.push(availableDays);
        }
        if (specificDate !== undefined) {
          sets.push("specific_date = ?");
          params.push(specificDate);
        }
        if (status !== undefined) {
          sets.push("status = ?");
          params.push(status);
        }
        if (sets.length === 0) {
          return textResult({ error: "更新する項目がありません。" });
        }
        params.push(questId);
        const result = await execute(
          `UPDATE quest_t SET ${sets.join(", ")}, updated_date = NOW() WHERE quest_id = ?`,
          params
        );
        if (result.affectedRows === 0) {
          return textResult({ error: `quest_id=${questId} は存在しません。` });
        }
        return textResult({ questId, message: "更新しました。" });
      }
    )
  );
}
