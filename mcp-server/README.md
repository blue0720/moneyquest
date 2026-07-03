# moneyquest-mcp-server

MoneyQuest の MySQL データベース(`mqdb`)を Claude から直接参照・作成・更新できるようにする MCP サーバー(stdio)。削除系のToolは提供していない(安全のため)。

## セットアップ

```
cd mcp-server
npm install
```

`.env`(gitignore対象)に接続情報を置く。値は `src/main/resources/application.properties` と同じものが既定値になっている。

```
MQ_MCP_DB_HOST=localhost
MQ_MCP_DB_PORT=3306
MQ_MCP_DB_NAME=mqdb
MQ_MCP_DB_USER=mquser
MQ_MCP_DB_PASSWORD=Rezo_0000
```

## Claude Code への登録

リポジトリ直下の `.mcp.json` に登録済み。Claude Code でこのプロジェクトを開けば自動的に `moneyquest-db` サーバーとして認識される(初回はMCPサーバーの承認プロンプトが出る)。

```json
{
  "mcpServers": {
    "moneyquest-db": {
      "command": "node",
      "args": ["mcp-server/src/index.js"]
    }
  }
}
```

## 提供している Tool 一覧

| ドメイン | 参照 | 作成/更新 |
|---|---|---|
| ユーザー | `list_users`, `get_user` | `create_user`, `update_user` |
| クエスト | `list_quests`, `get_quest` | `create_quest`, `update_quest` |
| クエストテンプレート | `list_quest_templates` | `create_quest_template`, `update_quest_template` |
| 収支記録 | `list_income_expense` | `create_income_expense`, `update_income_expense` |
| 支出上限申請 | `list_spending_limits` | `create_spending_limit`, `update_spending_limit`(承認/却下も`requestStatus`更新で行う) |
| キャラクター | `get_character` | `create_character`, `update_character` |

- 削除(DELETE)系のToolは意図的に用意していない。
- `user_t.password` はハッシュ済みの値のため、一覧・取得系のレスポンスには含めていない。`create_user`/`update_user` に渡す `password` は平文でよく、サーバー側でBCrypt(コスト10、`SecurityConfig`の`BCryptPasswordEncoder`と同一アルゴリズム)にハッシュ化してから保存する。
- 各テーブルのステータス値(quest_t.status、spending_limit_t.request_status、income_expense_t.record_type)の意味は各Toolのdescriptionに記載している(実装コードから調査した値)。

## 動作確認方法(手動)

```
cd mcp-server
node src/index.js
```

起動後、標準入力にJSON-RPCの`initialize`→`tools/list`→`tools/call`を1行ずつ流し込んで応答を確認できる。stdoutにJSON-RPC以外の文字列が混じると動作しなくなるため、`console.log`や`dotenv`の起動ログ等を追加しないよう注意する(ログは必ず`console.error`かstderrへ)。
