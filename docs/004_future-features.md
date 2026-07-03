# 004: 追加機能(将来構想・拡張機能)

- 種別: 追加機能
- 優先度: 低(将来対応)
- ステータス: 対応中(1/5 完了)

## 背景・現状

要件定義書(最終要件定義書_Group1.docx)の「拡張性」節にて、今回リリースのスコープ外・将来構想として明記されている機能群。現行の要件(56機能)には含まれないため未実装だったが、追加機能として1つずつ実装していく。

## Todo

- [〇] 1. バッジ/称号機能の追加(クエスト達成数や継続日数などに応じてバッジ・称号を付与する仕組み)
      → クエスト達成数(`character_t.total_achievement_count`)に応じた6段階のバッジを実装済み。詳細は「実施内容」参照。
- [ ] 2. キャラクターの種類追加(現在1種類のキャラクターに加えて、選択可能な複数キャラクターを用意する)
- [ ] 3. NPCとの対戦機能(育成したキャラクターでNPCと対戦できる要素の追加)
- [ ] 4. コインショップ機能(貯めたお金やコインでアイテム・キャラクター装飾等と交換できる仕組み)
- [ ] 5. 曜日指定クエスト機能(特定の曜日のみ実施可能なクエストを設定できる仕組み)

## 実施内容(1. バッジ/称号機能)

達成数に応じて動的に判定する方式とし、新規テーブルは追加していない(既存の`character_t.total_achievement_count`のみを参照)。

- `domain/model/BadgeDto.java`(新規): code, name, description, icon, requiredCount, earned
- `domain/service/BadgeService.java`(新規): 6段階のバッジ定義(はじめの一歩1回・がんばりやさん5回・コツコツマスター10回・ベテランクエスター30回・クエストのたつじん50回・でんせつのクエスター100回)を保持し、`getBadges(childUserId)`で獲得済みフラグ付きの一覧を返す
- `HomeController` / `QuestController` / `IncomeExpenseController`(`setChildHomeData`) / `SpendingLimitController` の子供ホーム関連アクションに`badgeList`をモデル追加として配線
- `templates/child-home.html`: ホームタブのキャラクターカードの下に「🎖️ しょうごう・バッジ」カードを追加。`th:if="${badgeList != null}"`でガードしているため、`badgeList`を配線していない`GraphController`経由のアクセスでも表示されないだけで壊れない
- `static/css/child-home-hometab.css`: `.badge-card`/`.badge-list`/`.badge-item`等のスタイルを追加(獲得済みは色付き、未獲得はグレーアウト)

### 検証方法

アプリを起動し、達成数が異なる複数の子供アカウント(41回・0回)でログインして`/child/home`を確認。41回のアカウントは1〜30回のバッジ4個が獲得済み表示・50/100回の2個が未獲得(グレーアウト)表示になること、0回のアカウントは6個すべて未獲得表示になることを確認。また`/child/quest`, `/child/records`, `/child/graph`, `/child/limit`の他タブが引き続き200 OKで表示され、`badgeList`を配線していない`GraphController`経由でも崩れないことを確認した。

## 対象箇所

- 001_要件定義/最終要件定義書_Group1.docx(拡張性 節)
- `src/main/java/com/example/moneyquest/app/domain/model/BadgeDto.java`(新規)
- `src/main/java/com/example/moneyquest/app/domain/service/BadgeService.java`(新規)
- `src/main/java/com/example/moneyquest/app/presentation/controller/{HomeController,QuestController,IncomeExpenseController,SpendingLimitController}.java`(変更)
- `src/main/resources/templates/child-home.html`(変更)
- `src/main/resources/static/css/child-home-hometab.css`(変更)
- 2〜5(キャラクター種類追加・NPC対戦・コインショップ・曜日指定クエスト)は実装時に `domain/model`, `domain/service`, `infra/entity`, `presentation/controller` の各層への機能追加が必要になる見込み(詳細設計は着手前に別途検討)
