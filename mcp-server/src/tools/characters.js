import { z } from "zod";
import { query, execute } from "../db.js";
import { textResult, withErrorHandling } from "../util.js";

export function registerCharacterTools(server) {
  server.registerTool(
    "get_character",
    {
      title: "キャラクター情報取得",
      description: "子供の user_id を指定してキャラクター情報(character_t)を1件取得する。",
      inputSchema: { childUserId: z.number().int() },
    },
    withErrorHandling(async ({ childUserId }) => {
      const rows = await query(
        "SELECT * FROM character_t WHERE child_user_id = ?",
        [childUserId]
      );
      if (rows.length === 0) {
        return textResult({ error: `child_user_id=${childUserId} のキャラクターは存在しません。` });
      }
      return textResult(rows[0]);
    })
  );

  const CHARACTER_TYPES = ["GRASS", "FLAME", "AQUA", "THUNDER"];
  const CHARACTER_TYPE_DESC =
    "characterType: GRASS(くさタイプ)/FLAME(ほのおタイプ)/AQUA(みずタイプ)/THUNDER(かみなりタイプ)の4種類。子供アカウント作成時に選択し、子供ホームの「しゅるいをかえる」からいつでも変更可能。";

  server.registerTool(
    "create_character",
    {
      title: "キャラクター新規作成",
      description:
        `子供アカウント登録時にキャラクターが未作成の場合に新規作成する(1子供につき1体、既に存在する場合はエラー)。${CHARACTER_TYPE_DESC}`,
      inputSchema: {
        childUserId: z.number().int(),
        characterName: z.string().min(1).max(50),
        characterType: z.enum(CHARACTER_TYPES).default("GRASS"),
      },
    },
    withErrorHandling(async ({ childUserId, characterName, characterType }) => {
      const existing = await query(
        "SELECT character_id FROM character_t WHERE child_user_id = ?",
        [childUserId]
      );
      if (existing.length > 0) {
        return textResult({ error: `child_user_id=${childUserId} には既にキャラクターが存在します。` });
      }
      const result = await execute(
        `INSERT INTO character_t (child_user_id, character_type, character_name, level, total_achievement_count, current_exp)
         VALUES (?, ?, ?, 0, 0, 0)`,
        [childUserId, characterType ?? "GRASS", characterName.trim()]
      );
      return textResult({ characterId: result.insertId, message: "作成しました。" });
    })
  );

  server.registerTool(
    "update_character",
    {
      title: "キャラクター更新",
      description:
        `キャラクター名・種類・レベル・経験値・累計達成数を更新する(削除は不可)。クエスト承認時のexp加算・達成数加算はアプリ側のロジック(CharacterService)に準ずるため、通常はこのToolで直接レベルやexpを操作するのではなく、確認・手動補正用途で使う。${CHARACTER_TYPE_DESC}`,
      inputSchema: {
        childUserId: z.number().int(),
        characterName: z.string().min(1).max(50).optional(),
        characterType: z.enum(CHARACTER_TYPES).optional(),
        level: z.number().int().min(0).optional(),
        currentExp: z.number().int().min(0).optional(),
        totalAchievementCount: z.number().int().min(0).optional(),
      },
    },
    withErrorHandling(
      async ({ childUserId, characterName, characterType, level, currentExp, totalAchievementCount }) => {
        const sets = [];
        const params = [];
        if (characterName !== undefined) {
          if (characterName.trim().length === 0) {
            return textResult({ error: "characterName は空白のみでは更新できません。" });
          }
          sets.push("character_name = ?");
          params.push(characterName.trim());
        }
        if (characterType !== undefined) {
          sets.push("character_type = ?");
          params.push(characterType);
        }
        if (level !== undefined) {
          sets.push("level = ?");
          params.push(level);
        }
        if (currentExp !== undefined) {
          sets.push("current_exp = ?");
          params.push(currentExp);
        }
        if (totalAchievementCount !== undefined) {
          sets.push("total_achievement_count = ?");
          params.push(totalAchievementCount);
        }
        if (sets.length === 0) {
          return textResult({ error: "更新する項目がありません。" });
        }
        params.push(childUserId);
        const result = await execute(
          `UPDATE character_t SET ${sets.join(", ")} WHERE child_user_id = ?`,
          params
        );
        if (result.affectedRows === 0) {
          return textResult({ error: `child_user_id=${childUserId} のキャラクターは存在しません。` });
        }
        return textResult({ childUserId, message: "更新しました。" });
      }
    )
  );
}
