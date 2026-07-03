import { z } from "zod";
import bcrypt from "bcryptjs";
import { query, execute } from "../db.js";
import { textResult, withErrorHandling } from "../util.js";

const AUTHORITY_DESC =
  "権限: 0=管理者(admin), 1=保護者(parent), 2=子供(child)";

// password列はハッシュ値のため一覧・取得系のレスポンスには含めない
const SAFE_COLUMNS =
  "user_id, parent_user_id, authority, user_name, mail_address";

export function registerUserTools(server) {
  server.registerTool(
    "list_users",
    {
      title: "ユーザー一覧取得",
      description:
        "user_t からユーザー一覧を取得する(パスワードハッシュは含まない)。" +
        AUTHORITY_DESC,
      inputSchema: {
        authority: z
          .number()
          .int()
          .min(0)
          .max(2)
          .optional()
          .describe("絞り込む権限。省略時は全件。"),
        parentUserId: z
          .number()
          .int()
          .optional()
          .describe("この保護者IDに紐づく子供のみ取得する場合に指定。"),
      },
    },
    withErrorHandling(async ({ authority, parentUserId }) => {
      const conditions = [];
      const params = [];
      if (authority !== undefined) {
        conditions.push("authority = ?");
        params.push(authority);
      }
      if (parentUserId !== undefined) {
        conditions.push("parent_user_id = ?");
        params.push(parentUserId);
      }
      const where = conditions.length ? `WHERE ${conditions.join(" AND ")}` : "";
      const rows = await query(
        `SELECT ${SAFE_COLUMNS} FROM user_t ${where} ORDER BY user_id`,
        params
      );
      return textResult(rows);
    })
  );

  server.registerTool(
    "get_user",
    {
      title: "ユーザー詳細取得",
      description: "user_id を指定してユーザー1件を取得する(パスワードハッシュは含まない)。",
      inputSchema: {
        userId: z.number().int().describe("user_t.user_id"),
      },
    },
    withErrorHandling(async ({ userId }) => {
      const rows = await query(
        `SELECT ${SAFE_COLUMNS} FROM user_t WHERE user_id = ?`,
        [userId]
      );
      if (rows.length === 0) {
        return textResult({ error: `user_id=${userId} は存在しません。` });
      }
      return textResult(rows[0]);
    })
  );

  server.registerTool(
    "create_user",
    {
      title: "ユーザー新規作成",
      description:
        "保護者/子供/管理者アカウントを新規作成する。パスワードは自動的にBCryptでハッシュ化してから保存する。" +
        AUTHORITY_DESC +
        " authority=2(子供)の場合、parentUserId(保護者のuser_id)が必須。",
      inputSchema: {
        authority: z.number().int().min(0).max(2),
        userName: z.string().min(1).max(50),
        mailAddress: z.string().email().max(255),
        password: z.string().min(1).max(255).describe("平文パスワード。保存前にハッシュ化する。"),
        parentUserId: z
          .number()
          .int()
          .optional()
          .describe("authority=2(子供)の場合に必須の保護者user_id。"),
      },
    },
    withErrorHandling(async ({ authority, userName, mailAddress, password, parentUserId }) => {
      if (authority === 2 && !parentUserId) {
        return textResult({ error: "子供アカウントには parentUserId が必須です。" });
      }
      const hash = await bcrypt.hash(password, 10);
      const result = await execute(
        `INSERT INTO user_t (parent_user_id, authority, user_name, mail_address, password)
         VALUES (?, ?, ?, ?, ?)`,
        [authority === 2 ? parentUserId : null, authority, userName.trim(), mailAddress, hash]
      );
      return textResult({ userId: result.insertId, message: "作成しました。" });
    })
  );

  server.registerTool(
    "update_user",
    {
      title: "ユーザー更新",
      description:
        "既存ユーザーの氏名/メールアドレス/パスワードを更新する(削除は不可)。パスワードを指定した場合のみ再ハッシュ化する。",
      inputSchema: {
        userId: z.number().int(),
        userName: z.string().min(1).max(50).optional(),
        mailAddress: z.string().email().max(255).optional(),
        password: z.string().min(1).max(255).optional(),
      },
    },
    withErrorHandling(async ({ userId, userName, mailAddress, password }) => {
      const sets = [];
      const params = [];
      if (userName !== undefined) {
        sets.push("user_name = ?");
        params.push(userName.trim());
      }
      if (mailAddress !== undefined) {
        sets.push("mail_address = ?");
        params.push(mailAddress);
      }
      if (password !== undefined) {
        sets.push("password = ?");
        params.push(await bcrypt.hash(password, 10));
      }
      if (sets.length === 0) {
        return textResult({ error: "更新する項目がありません。" });
      }
      params.push(userId);
      const result = await execute(
        `UPDATE user_t SET ${sets.join(", ")} WHERE user_id = ?`,
        params
      );
      if (result.affectedRows === 0) {
        return textResult({ error: `user_id=${userId} は存在しません。` });
      }
      return textResult({ userId, message: "更新しました。" });
    })
  );
}
