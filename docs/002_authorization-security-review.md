# 002: 権限制御(IDOR)に関する横断的セキュリティレビュー

- 種別: セキュリティレビュー
- 優先度: 高
- ステータス: 完了

## 背景・現状

- バグ管理表(全28件、全て修正完了)のうち6件(No.8, 10, 13, 21, 24, 28)は、URLパラメータ改ざんにより本来アクセス権限のないリソース(他家族の子供のクエスト編集/削除/承認/却下、他人の支出上限却下、自分自身の管理者アカウント削除等)を操作できてしまうIDOR(Insecure Direct Object Reference)系の不具合だった。
- 個別の不具合は再テストで修正確認済みだが、同種の「所有者チェック漏れ」というパターンが繰り返し検出されていたことから、システムテスト設計書がカバーしていない他のPOST/更新系エンドポイントにも同様の考慮漏れが残っている可能性がある。

## Todo

- [〇] 1. 全ControllerのPOST/更新系エンドポイント(クエスト・収支記録・支出上限・キャラクター・アカウント関連)を棚卸しし、パスパラメータで指定されるリソースIDに対して「ログインユーザーが所有者/家族関係にあるか」のチェックが各Serviceメソッドで行われているか確認する。
      → 26件のPOSTエンドポイント全件を確認。**バグ管理表で「修正完了」となっている No.8, 10, 13, 28 は、実際のコードには所有者チェックが一切実装されておらず、現在も脆弱な状態だったことが判明**(下記「実施内容」参照)。ドキュメント上のステータスと実装が一致していなかった。
- [〇] 2. チェック漏れが見つかった箇所には、修正済みの箇所(例: QuestService)と同様の所有者チェックを追加する。
      → QuestTemplateService(`findByQuestTemplateIdAndParentUser_UserId`)を模範パターンとして、以下7箇所に所有者チェックを追加した。
- [〇] 3. 可能であれば、他ユーザーIDを付与したリクエストを送る権限バイパステストをシステムテストに追加する。
      → システムテスト設計書への正式追加はスコープ外としたが、実装した全修正について、実際にアプリを起動し2つの異なる保護者アカウント・2つの異なる管理者アカウントでログインした上でのクロスアカウント攻撃テスト(curlによるHTTPリクエスト)を実施し、攻撃が阻止されること・正規操作は引き続き成功することの両方をDBの実データで確認した。

## 実施内容(発見した脆弱性と修正)

実装コードを直接確認した結果、ドキュメント上「修正完了」とされている脆弱性を含め、合計7箇所で所有者チェックが欠落していた。

| # | 箇所 | 内容 | 対応 |
|---|---|---|---|
| 1 | `QuestService.updateQuest` | 保護者が**任意の**クエストをIDだけで編集できた(バグNo.8相当、未修正だった) | `parentUserId`を引数に追加し、`childUser.parentUserId`との一致チェックを追加 |
| 2 | `QuestService.deleteQuest` | 保護者が**任意の**クエストをIDだけで削除できた(バグNo.8相当、未修正だった) | 同上 |
| 3 | `QuestService.requestComplete` | 子供が**他人の**クエストをIDだけで完了申請できた(バグNo.10相当、未修正だった) | `childUserId`を引数に追加し、`childUser.userId`との一致チェックを追加 |
| 4 | `QuestService.approveQuest` | 保護者が**他家族の**クエストを承認でき、報酬付与やexp加算まで実行できた(バグNo.13相当、未修正だった) | 既存の`parentUser`引数を使い、所有者チェックを追加 |
| 5 | `QuestService.rejectQuest` | 保護者が**他家族の**クエストを却下できた(バグNo.13相当、未修正だった) | `parentUserId`を引数に追加し、所有者チェックを追加 |
| 6 | `SpendingService.approveLimit` / `rejectLimit` | 保護者が**他家族の**支出上限申請を承認・却下できた(バグNo.28は却下のみ言及、承認も同様に未修正だった) | 所有者チェックを追加(`SecurityException`をスロー、Controller側でエラー画面へリダイレクト) |
| 7 | `IncomeExpenseService.createRecord` | 保護者が**他家族の**子供に収入記録を作成できた(新規に発見。バグ管理表に記載なし) | `targetChildUserId`の`parentUserId`一致チェックを追加 |
| 8 | `UserController.updateAdmin` | 管理者が**他の管理者アカウント**を編集・パスワード変更できた(乗っ取り。新規に発見) | `id == loginUser.getUserId()`でない場合は`/admin/error`へリダイレクト |
| 9 | `UserController.deleteAdmin` | 管理者が**自分自身**を削除できた(バグNo.24、未修正だった) | 自分自身のIDの場合は`/admin/error`へリダイレクト |

修正しなかった(既に正しく実装されていた)箇所: `QuestTemplateService`(更新/削除)、`IncomeExpenseService`(更新/削除)、`UserService.updateChildUser`/`deleteFamilyUser`(保護者による子供アカウント編集/削除、バグNo.21相当)。これらは元から`findByXxxAndParentUser_UserId`系のクエリや明示的な所有者チェックが実装済みだった。

### 検証方法

1. `./mvnw compile` でコンパイルが通ることを確認。
2. `./mvnw spring-boot:run` でアプリを起動(prodプロファイル、Spring Security認証あり)。
3. `parent0@test.jp`(子供4,5の保護者)と`parent1@test.jp`(子供6,7の保護者)の2アカウントでログインし、parent1のセッションから`parent0`の子供に紐づくクエスト(quest_id=1,2)・支出上限申請(spending_limit_id=1)に対して編集・削除・承認・却下をcurlで直接攻撃 → **すべて権限エラー画面にリダイレクトされ、DBのデータは一切変更されないことを確認**。
4. `admin0@test.jp`と`admin1@test.jp`の2管理者アカウントでログインし、admin1のセッションからadmin0のアカウント編集・admin1自身の削除を攻撃 → **すべて`/admin/error`にリダイレクトされ、DBのデータは変更されないことを確認**。
5. 正規操作(parent0が自分の子供のクエストを編集、admin1が自分のアカウントを編集)が引き続き成功することをDBの実データで確認(リグレッションなし)。

## 対象箇所

- `src/main/java/com/example/moneyquest/app/domain/service/QuestService.java`(変更済み)
- `src/main/java/com/example/moneyquest/app/domain/service/SpendingService.java`(変更済み)
- `src/main/java/com/example/moneyquest/app/domain/service/IncomeExpenseService.java`(変更済み)
- `src/main/java/com/example/moneyquest/app/presentation/controller/QuestController.java`(変更済み)
- `src/main/java/com/example/moneyquest/app/presentation/controller/SpendingLimitController.java`(変更済み)
- `src/main/java/com/example/moneyquest/app/presentation/controller/IncomeExpenseController.java`(変更済み)
- `src/main/java/com/example/moneyquest/app/presentation/controller/UserController.java`(変更済み)
- 004_テスト/マネークエスト_バグ管理表.xlsx (No.8, 10, 13, 24, 28)

## 備考

ドキュメント上は全バグ修正完了・全テストOKで「完了」扱いだったが、実際のコードとの突き合わせにより、**そのうち5件(No.8, 10, 13, 24, 28)は実装が伴っていなかった**ことが判明した。バグ管理表・システムテスト設計書のステータス更新は本チケットの対応範囲外だが、成果物一式の他の関係者にも共有し、ドキュメント側の記載修正を検討することを推奨する。
