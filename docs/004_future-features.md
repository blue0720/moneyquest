# 004: 追加機能(将来構想・拡張機能)

- 種別: 追加機能
- 優先度: 低(将来対応)
- ステータス: 対応中(5/5 完了)

## 背景・現状

要件定義書(最終要件定義書_Group1.docx)の「拡張性」節にて、今回リリースのスコープ外・将来構想として明記されている機能群。現行の要件(56機能)には含まれないため未実装だったが、追加機能として1つずつ実装していく。

## Todo

- [〇] 1. バッジ/称号機能の追加(クエスト達成数や継続日数などに応じてバッジ・称号を付与する仕組み)
      → クエスト達成数(`character_t.total_achievement_count`)に応じた6段階のバッジを実装済み。詳細は「実施内容」参照。
- [〇] 2. キャラクターの種類追加(現在1種類のキャラクターに加えて、選択可能な複数キャラクターを用意する)
      → 4種類(くさ/ほのお/みず/かみなり)を実装済み。詳細は「実施内容」参照。
- [〇] 3. NPCとの対戦機能(育成したキャラクターでNPCと対戦できる要素の追加)
      → レベル差に応じた確率判定で5体のNPCと対戦できる仕組みを実装済み。詳細は「実施内容」参照。
- [〇] 4. コインショップ機能(貯めたお金やコインでアイテム・キャラクター装飾等と交換できる仕組み)
      → クエスト報酬・対戦勝利で貯まるコインでフレーム/称号を購入・装備できる仕組みを実装済み。詳細は「実施内容」参照。
- [〇] 5. 曜日指定クエスト機能(特定の曜日のみ実施可能なクエストを設定できる仕組み)
      → クエスト(quest_t)単位で実施可能曜日をビットマスクで持たせる方式を実装済み。詳細は「実施内容」参照。

## 実施内容(1. バッジ/称号機能)

達成数に応じて動的に判定する方式とし、新規テーブルは追加していない(既存の`character_t.total_achievement_count`のみを参照)。

- `domain/model/BadgeDto.java`(新規): code, name, description, icon, requiredCount, earned
- `domain/service/BadgeService.java`(新規): 6段階のバッジ定義(はじめの一歩1回・がんばりやさん5回・コツコツマスター10回・ベテランクエスター30回・クエストのたつじん50回・でんせつのクエスター100回)を保持し、`getBadges(childUserId)`で獲得済みフラグ付きの一覧を返す
- `HomeController` / `QuestController` / `IncomeExpenseController`(`setChildHomeData`) / `SpendingLimitController` の子供ホーム関連アクションに`badgeList`をモデル追加として配線
- `templates/child-home.html`: ホームタブのキャラクターカードの下に「🎖️ しょうごう・バッジ」カードを追加。`th:if="${badgeList != null}"`でガードしているため、`badgeList`を配線していない`GraphController`経由のアクセスでも表示されないだけで壊れない
- `static/css/child-home-hometab.css`: `.badge-card`/`.badge-list`/`.badge-item`等のスタイルを追加(獲得済みは色付き、未獲得はグレーアウト)

### 検証方法

アプリを起動し、達成数が異なる複数の子供アカウント(41回・0回)でログインして`/child/home`を確認。41回のアカウントは1〜30回のバッジ4個が獲得済み表示・50/100回の2個が未獲得(グレーアウト)表示になること、0回のアカウントは6個すべて未獲得表示になることを確認。また`/child/quest`, `/child/records`, `/child/graph`, `/child/limit`の他タブが引き続き200 OKで表示され、`badgeList`を配線していない`GraphController`経由でも崩れないことを確認した。

## 実施内容(2. キャラクターの種類追加)

`character_t`に列追加(`ALTER TABLE character_t ADD COLUMN character_type VARCHAR(20) NOT NULL DEFAULT 'GRASS'`)。新規テーブルは追加していない。画像素材が用意されていないため、既存のレベル帯画像(`Lv_0.png`〜`Lv_50.png`)をCSSの`hue-rotate`フィルタで色違い表示するダミー対応とし、DB・選択UI・切替ロジックの実装を優先した。

- `domain/model/CharacterType.java`(新規): `GRASS`(くさ)/`FLAME`(ほのお)/`AQUA`(みず)/`THUNDER`(かみなり)の4種類、日本語表示名を保持
- `infra/entity/CharacterEntity.java` / `domain/model/CharacterDto.java`: `characterType`列を追加
- `domain/service/CharacterService.java`: `createCharacter(childUserId, characterType)`に変更(nullなら`GRASS`)、`updateCharacterType(childUserId, characterType)`を新規追加
- `presentation/form/UserForm.java`に`characterType`追加(子供作成時のみ使用)、`presentation/form/CharacterTypeForm.java`(新規)
- `presentation/controller/HomeController.java`に`POST /child/character/type`を追加
- `templates/parent-home.html`: 子供追加モーダルに4択のラジオチップを追加(デフォルト`GRASS`)
- `templates/child-home.html`: キャラクター名の隣に種類バッジを表示、画像に`character-type-{grass|flame|aqua|thunder}`クラスを付与、「しゅるいをかえる」ボタン・モーダルを追加
- `static/css/child-home-hometab.css` / `static/css/parent-family.css`: 色違い表現用の`hue-rotate`フィルタとチップUIのスタイルを追加
- `mcp-server/src/tools/characters.js`: `create_character`/`update_character`に`characterType`パラメータを追加

`CharacterDto`は既に5コントローラ(`HomeController`/`QuestController`/`IncomeExpenseController`/`SpendingLimitController`/`GraphController`)から配線済みのため、表示側の追加配線は不要だった。

### 検証方法

`./mvnw test`で`CharacterServiceTest`・`UserServiceTest`を含む全テストが通ることを確認(19+13件、失敗0件)。アプリを起動し、保護者で子供アカウントを新規作成する際に4種類から選べること、`/child/home`でその種類の色が反映されること、「しゅるいをかえる」から種類変更ができ画面に反映されることを確認。

## 実施内容(5. 曜日指定クエスト機能)

単発クエスト(`quest_t`)にのみ対象曜日を持たせ、テンプレート(`quest_template_t`)には持たせない方針とした。`quest_t`に列追加(`ALTER TABLE quest_t ADD COLUMN available_days INT NOT NULL DEFAULT 127`)。ビットマスク(月=1,火=2,水=4,木=8,金=16,土=32,日=64、127=毎日)で表現し、既存クエストは全て127(毎日実施可能)になるため後方互換。

- `domain/model/QuestDay.java`(新規): 曜日enum(ビット値・日本語ラベル・`DayOfWeek`からの変換ヘルパー)
- `infra/entity/QuestEntity.java`: `availableDays`列を追加(QuestEntityはテンプレートで直接使われているためDTO層の変更は不要)
- `presentation/form/QuestSendForm.java`: `availableDays`(チェックボックスからバインド)を追加
- `domain/model/QuestDay.java`: ビットマスク⇔曜日変換・表示整形のstaticユーティリティ(`isAvailableToday`/`format`/`toCsv`/`fromDayOfWeek`)を集約
- `infra/entity/QuestEntity.java`: テンプレート表示用に`isAvailableToday()`/`getAvailableDaysDisplay()`/`getAvailableDayCodes()`のインスタンスメソッドを公開(`QuestDay`のstaticに委譲)
- `domain/service/QuestService.java`: `createQuest`で未指定なら全曜日マスク、`updateQuest`で未指定なら既存値維持、`requestComplete`で今日が対象曜日でなければ`IllegalArgumentException`。表示用の`isAvailableToday`/`formatAvailableDays`/`getAvailableDayCodes`も`QuestDay`に委譲(サービス経由のAPIとして維持)
- `templates/parent-home.html`: クエスト追加/編集モーダルに曜日チェックボックス(月〜日)を追加、編集ボタンに`th:data-available-days="${quest.availableDayCodes}"`を付与し`static/js/parent-home.js`の`openQuestEditModal`で選択状態を復元
- `templates/child-home.html`: 完了ボタンの表示条件に`quest.isAvailableToday()`を追加し、対象外の日は「🗓 きょうは実施できません」を表示、実施可能曜日(`quest.availableDaysDisplay`。「毎日」または「月・水・金」等)を常時表示
  - ※ Thymeleafは`th:each`ループ内でのBean参照(`@questService.xxx(...)`)を「Instantiation of new objects and access to static classes or parameters is forbidden」で解析エラーにするため、表示用ロジックはService(Bean)ではなく`QuestEntity`のインスタンスメソッド経由で呼び出す構成にしている
- `static/css/parent-quest.css` / `static/css/child-quest.css`: 曜日チップ・非活性表示のスタイルを追加
- `mcp-server/src/tools/quests.js`: `create_quest`/`update_quest`/`list_quests`に`availableDays`ビットマスクの意味を明記し、パラメータを追加

### 検証方法

`./mvnw test`で`QuestServiceTest`(33件、失敗0件)を含む全テストが通ることを確認。特に「曜日未指定なら全曜日マスクで作成」「今日が対象曜日に含まれない場合は完了申請でIllegalArgumentException」の2点をユニットテストで検証。アプリを起動し、保護者がクエスト作成時に曜日を限定して送信し、対象外の曜日にログインした子供アカウントでは完了ボタンが出ず「実施できません」表示になること、対象の曜日ではボタンが表示され完了申請が通ることを確認。

### 追加拡張: 特定日指定

曜日指定に加えて、単一の特定日(one-off)を指定できるように拡張した。曜日指定と特定日指定が両方設定されている場合はAND条件(両方を満たす日のみ実施可能)。`quest_t`に列追加(`ALTER TABLE quest_t ADD COLUMN specific_date DATE NULL`)。未設定(null)なら日付による制限なし(既存クエストは全てnullのため後方互換)。

- `infra/entity/QuestEntity.java`: `specificDate`列を追加。`isAvailableToday()`を拡張し、`specificDate`が設定されている場合はまずその日付と一致するかを判定してから曜日判定を行う(AND条件)
- `presentation/form/QuestSendForm.java`: `specificDate`(`<input type="date">`からバインド、`@DateTimeFormat(pattern = "yyyy-MM-dd")`)を追加
- `domain/service/QuestService.java`: `createQuest`/`updateQuest`双方で`specificDate`を単純上書き(曜日チェックボックスと異なり、編集モーダルは現在値を復元表示するため、空欄送信は「日付指定を解除する」という意図がとれる)
- `templates/parent-home.html`: クエスト追加/編集モーダルに日付inputを追加。編集ボタンに`th:data-specific-date="${quest.specificDate}"`(`LocalDate#toString()`がISO8601形式でHTML5 date inputの値と一致)を付与し、`static/js/parent-home.js`で復元
- `templates/child-home.html`: `#temporals`(Thymeleaf標準の日付フォーマットユーティリティ、Bean参照ではないためth:eachループ内でも制限に引っかからない)で「📅 じっしび: yyyy/MM/dd」を表示
- `mcp-server/src/tools/quests.js`: `create_quest`/`update_quest`に`specificDate`(YYYY-MM-DD、`update_quest`は`null`で解除可能)を追加

## 実施内容(3. NPCとの対戦機能)

対戦結果は永続化せず、NPCロースター自体もバッジ機能と同様にハードコードで持たせる方針とした(新規テーブルは対戦用には作っていない)。何回でも挑戦・報酬受け取り可能(周回可、1日制限などなし)。

- `domain/model/NpcDto.java`(新規)・`domain/model/BattleResultDto.java`(新規): NPC1体分の表示用DTOと、対戦1回分の結果DTO(勝敗・獲得コイン・勝率)
- `domain/service/BattleService.java`(新規): NPC5体(スライムン/ゴブジャー/ロックゴーレム/フレイムウルフ/ドラゴニア、必要レベル0/10/25/40/70、コイン報酬5/10/20/35/60)を保持。`getNpcList(childUserId)`でキャラのレベルに応じた解放状態・勝率を、`challenge(childUserId, npcCode)`で対戦結果を返す
  - 解放条件: キャラレベル >= NPCの必要レベル。未解放または存在しないNPCコードへの挑戦は例外
  - 勝率: `min(90, 50 + (レベル差) * 5)`(％)。1〜100の乱数がこの値以下なら勝利
  - 勝利時のみ`CharacterService.addCoins`でNPC固有のコインを加算
- `CharacterService`に`addCoins(childUserId, amount)`を追加(既存`addExp`と同じ形)
- `presentation/controller/BattleController.java`(新規): 他の子供用タブと同じ組み立て方(`ParentModelHelper.setDefaults`+character/badgeList等を手動配線)。`GET /child/battle`でタブ表示、`POST /child/battle/{code}/challenge`で対戦しRedirectAttributesのflash属性で結果を1回だけ画面へ渡す
- `templates/child-home.html`: 「たいせん」タブを追加。NPCカード一覧(解放前はグレーアウトしボタン非表示、解放後は勝率・コイン報酬・「ちょうせんする」ボタンを表示)、直前の対戦結果バナー
- `static/css/child-battle.css`(新規)

### 検証方法

`./mvnw test`で`BattleServiceTest`(乱数を固定できるテスト用コンストラクタで解放判定・勝率計算・勝敗判定・コイン加算を検証)を含む全テストが通ることを確認。アプリを起動し、レベルの低い子供アカウントで`/child/battle`にアクセスして上位NPCがグレーアウトしていること、解放済みNPCに複数回挑戦して勝敗とコイン残高の増減が反映されることを確認した。

## 実施内容(4. コインショップ機能)

コインは`character_t.coin_balance`に保持し、クエスト報酬(`quest_t.coin_reward`)とNPC対戦の勝利報酬の両方から貯まる。購入済みアイテムは新規テーブル`character_item_t`で管理し、現在装備中のものは`character_t.equipped_frame`/`equipped_title`で別管理する(画像素材が無いためCSSのみのコスメ表示)。

- `infra/entity/CharacterItemEntity.java`(新規、`character_item_t`)・`infra/repository/CharacterItemRepository.java`(新規)
- `domain/model/ShopItemDto.java`(新規): 商品1件分の表示用DTO(コード・カテゴリ・名前・コスト・所持/装備フラグ)
- `domain/service/ShopService.java`(新規): カタログをハードコードで保持
  - フレーム: ノーマル(無料・常時所持)/シルバー(20)/ゴールド(50)/レインボー(100)
  - 称号: なし(無料・常時所持)/きらきら(15)/ゆうしゃ(40)/レジェンド(100)
  - `purchase(childUserId, itemCode)`: 未所持かつコイン残高が足りればコイン消費→購入記録保存→自動装備。所持済みなら何もしない
  - `equip(childUserId, itemCode)`: 所持済み(またはNONE系デフォルト)のみ装備切替可能。未所持は例外
- `CharacterService`に`spendCoins`(残高不足なら例外)・`updateEquippedFrame`・`updateEquippedTitle`を追加。`CharacterDto`に`coinBalance`/`equippedFrame`/`equippedTitle`を追加
- `QuestEntity`/`QuestSendForm`/`QuestService`に`coinReward`を追加。クエスト承認時に`rewardAmount`(お小遣い)・`exp`と並行してコインも加算されるようにした
- `presentation/controller/ShopController.java`(新規): 他の子供用タブと同じ組み立て方。`GET /child/shop`でタブ表示、`POST /child/shop/{code}/purchase`・`POST /child/shop/{code}/equip`で購入・装備切替
- `templates/child-home.html`: 「ショップ」タブを追加(コイン残高・フレーム/称号カード一覧・購入/装備ボタン)。ホームタブのキャラクターカードに装備中フレーム(枠の色)・称号(名前横のチップ)を反映
- `templates/parent-home.html`: クエスト追加/編集モーダルに「コイン報酬」入力欄を追加
- `static/css/child-shop.css`(新規、フレーム装飾・称号チップのスタイルも含む)

### 検証方法

`./mvnw test`で`ShopServiceTest`(所持/装備判定、購入時のコイン消費・自動装備、未所持アイテムの装備拒否)・`QuestServiceTest`の`coinReward`関連ケースを含む全テストが通ることを確認。アプリを起動し、保護者がクエストにコイン報酬を設定→子供が完了申請→承認後にコイン残高が増えること、NPC対戦の勝利でもコインが増えること、`/child/shop`で残高に応じて購入可否が切り替わり購入後にホームタブの見た目(フレーム/称号)に反映されることを確認した。

## 対象箇所

- 001_要件定義/最終要件定義書_Group1.docx(拡張性 節)
- `src/main/java/com/example/moneyquest/app/domain/model/BadgeDto.java`(新規)
- `src/main/java/com/example/moneyquest/app/domain/service/BadgeService.java`(新規)
- `src/main/java/com/example/moneyquest/app/domain/model/{CharacterType,QuestDay}.java`(新規)
- `src/main/java/com/example/moneyquest/app/presentation/form/CharacterTypeForm.java`(新規)
- `src/main/java/com/example/moneyquest/app/presentation/controller/{HomeController,QuestController,IncomeExpenseController,SpendingLimitController}.java`(変更)
- `src/main/java/com/example/moneyquest/app/domain/service/{CharacterService,UserService,QuestService}.java`(変更)
- `src/main/java/com/example/moneyquest/app/infra/entity/{CharacterEntity,QuestEntity}.java`(変更)
- `src/main/resources/templates/{parent-home,child-home}.html`(変更)
- `src/main/resources/static/css/{child-home-hometab,parent-family,parent-quest,child-quest}.css`(変更)
- `src/main/resources/static/js/{child-home,parent-home}.js`(変更)
- `mcp-server/src/tools/{characters,quests}.js`(変更)
- `src/main/java/com/example/moneyquest/app/domain/model/{NpcDto,BattleResultDto,ShopItemDto}.java`(新規)
- `src/main/java/com/example/moneyquest/app/domain/service/{BattleService,ShopService}.java`(新規)
- `src/main/java/com/example/moneyquest/app/infra/entity/CharacterItemEntity.java`(新規)
- `src/main/java/com/example/moneyquest/app/infra/repository/CharacterItemRepository.java`(新規)
- `src/main/java/com/example/moneyquest/app/presentation/controller/{BattleController,ShopController}.java`(新規)
- `src/main/resources/static/css/{child-battle,child-shop}.css`(新規)
- `mcp-server/src/tools/characterItems.js`(新規)
