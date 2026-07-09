import { z } from "zod";
import { query, execute } from "../db.js";
import { textResult, withErrorHandling } from "../util.js";

const ITEM_CODE_DESC =
  "itemCode: コインショップの商品コード。フレーム(FRAME_NONE/FRAME_SILVER/FRAME_GOLD/FRAME_RAINBOW)または称号(TITLE_NONE/TITLE_SHINE/TITLE_HERO/TITLE_LEGEND)。" +
  "FRAME_NONE/TITLE_NONEは無料の初期状態のため character_item_t には記録されない(常に所持扱い)。";

export function registerCharacterItemTools(server) {
  server.registerTool(
    "list_character_items",
    {
      title: "購入済みショップアイテム一覧取得",
      description:
        `子供の user_id を指定して、コインショップで購入済みのアイテム(character_item_t)一覧を取得する。${ITEM_CODE_DESC} ` +
        "現在装備中のものは character_t.equipped_frame/equipped_title を参照(get_character)。",
      inputSchema: { childUserId: z.number().int() },
    },
    withErrorHandling(async ({ childUserId }) => {
      const rows = await query(
        "SELECT * FROM character_item_t WHERE child_user_id = ? ORDER BY purchased_date DESC",
        [childUserId]
      );
      return textResult(rows);
    })
  );

  server.registerTool(
    "create_character_item",
    {
      title: "ショップアイテム購入記録の作成",
      description:
        `コインショップでの購入を character_item_t に記録する(削除は不可)。${ITEM_CODE_DESC} ` +
        "通常はアプリ側のショップ機能(ShopService.purchase)がコイン残高チェック・消費と合わせて行うため、このToolは確認・手動補正用途で使う。同一子供・同一itemCodeは重複登録できない。",
      inputSchema: {
        childUserId: z.number().int(),
        itemCode: z.string().min(1).max(30),
      },
    },
    withErrorHandling(async ({ childUserId, itemCode }) => {
      const existing = await query(
        "SELECT character_item_id FROM character_item_t WHERE child_user_id = ? AND item_code = ?",
        [childUserId, itemCode]
      );
      if (existing.length > 0) {
        return textResult({ error: `child_user_id=${childUserId} は既に itemCode=${itemCode} を所持しています。` });
      }
      const result = await execute(
        "INSERT INTO character_item_t (child_user_id, item_code, purchased_date) VALUES (?, ?, NOW())",
        [childUserId, itemCode]
      );
      return textResult({ characterItemId: result.insertId, message: "作成しました。" });
    })
  );
}
