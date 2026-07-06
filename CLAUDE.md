# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

マネークエストは、家族のおこづかい管理を行う Spring Boot 4 / Java 21 の Web アプリ。保護者が子供の収入記録・支出上限・クエスト(お手伝い)を設定し、子供はクエストをこなし、支出を記録し、キャラクターを育成する。ロールは `parent`(保護者)・`child`(子供)・`admin`(管理者)の3種類。Thymeleaf によるサーバーサイドレンダリングで、SPAフロントエンドは持たない。

## ビルド・実行

Maven Wrapper(`mvnw` / `mvnw.cmd`)を使用する。システムに Maven がインストールされている前提はない。

```
./mvnw spring-boot:run          # アプリを起動
./mvnw test                     # 全テストを実行
./mvnw test -Dtest=ClassName#methodName   # 単一テストを実行
./mvnw clean package            # WAR をビルド
```

- `src/main/resources/application.properties` は `jdbc:mysql://localhost:3306/mqdb`(ユーザー `mquser`)への接続を前提にしており、ローカルの MySQL80 サービス(MySQL 8.0)にこの設定のまま接続できるようセットアップ済み(`mqdb` データベース、`mquser`ユーザー、スキーマ・初期データとも作成済み)。`spring-boot-starter-data-jpa` は起動時に必ずこのデータソースへ接続しにいくため、`dev`/`prod` どちらのプロファイルで動かす場合も MySQL80 サービスが起動している必要がある(Windows サービスとして常駐。停止している場合は「サービス」アプリまたは `net start MySQL80` で起動する)。
  - スキーマ(DDL)・初期データ(DML)の再投入が必要になった場合は、納品物一式(デスクトップの「最終納品用」フォルダ)の `003_製造/初期データ/moneyquest_DDL.sql`(Shift-JIS)と `moneyquest_DML.sql`(UTF-8)を使う。文字コードが異なる点に注意し、`mysql --default-character-set=cp932 mqdb < moneyquest_DDL.sql` → `mysql --default-character-set=utf8mb4 mqdb < moneyquest_DML.sql` の順で流し込む。
- `application.properties` の `spring.profiles.active` が認証の有無を切り替える: `prod` では本番用の Spring Security 認証(`SecurityConfig`)が有効、`dev` では認証チェックが無効化される(`DevSecurityConfig`、CSRF も off)。ローカルで画面確認だけしたい場合はこの値を切り替える。両方の設定が同時に有効になることはない(`@Profile` により排他)。プロファイルの切り替えは datasource 設定には影響しない(どちらの場合も同じ MySQL 接続が必要)。

## アーキテクチャ

パッケージルートは `com.example.moneyquest.app`。3層構成になっている。

- `presentation/controller` — 機能領域ごとに `@Controller` を1つ配置(例: `QuestController`, `IncomeExpenseController`, `SpendingLimitController`)。同じドメイン(例: クエスト)に対する保護者向け操作と子供向け操作は、コメントの見出し(`👨‍👩‍👧 保護者用機能` / `👦 子供用機能`)で区切られつつ*同じ Controller クラス*に同居している。
- `presentation/form` — POST された HTML フォームをバインドする、Spring バリデーション付きのフォームオブジェクト(`QuestSendForm`, `IncomeExpenseForm` など)。ドメインの DTO とは別物。
- `presentation/controller/pageproperty/TransitionTargetPageNameKeyword` — すべてのルートパスと Thymeleaf テンプレート名を一元管理する唯一の場所。Controller・`SecurityConfig`・`DevSecurityConfig` はいずれも文字列をハードコードせずこの定数を参照している。新しいルートを追加する際はまずここに定義すること。
- `domain/model` — サービス層とプレゼンテーション層の間でデータをやり取りするための素の DTO(`QuestDto`, `UserDto` など)。JPA エンティティとは分離されている。
- `domain/service` — 集約ごとに1サービス(`QuestService`, `UserService`, `SpendingService`, `CharacterService`, `GraphService` など)。`infra/entity` と `domain/model` の相互変換を担う。
- `infra/entity` + `infra/repository` — Spring Data JPA のエンティティとリポジトリ。MySQL とやり取りするのはこの層のみ。
- `security` — `SecurityConfig`(prod用)/ `DevSecurityConfig`(dev用)。いずれもプロファイルで切り替わる。アクセス制御は URL プレフィックス単位: `/parent/**` → `ROLE_PARENT`、`/child/**` → `ROLE_CHILD`、`/admin/**` → `ROLE_ADMIN`。ログインは `/login` への POST で、パラメータ名は Spring 標準の `username` ではなく独自の `mailAddress`/`password` を使う。

### テンプレートは「画面ごと」ではなく「ロールごと」

`src/main/resources/templates` 配下には Thymeleaf テンプレートが数枚しかない(`parent-home.html`, `child-home.html`, `admin-home.html`、および login/register/error 系ページ)。各ロールの多数の「タブ」(クエスト・収支・グラフ・家族・承認 など)はすべて1つのテンプレートの中のセクションであり、別々のルート/ページにはなっていない。どのタブを表示するかは Controller のアクションがセットするモデル属性 `activeTab` で切り替える。保護者ホームや子供ホームを描画する Controller アクションを新規に追加する場合、そのアクションだけで完結する属性を渡すのではなく、共有テンプレートが要求するモデル全体を満たす必要がある(足りないと Thymeleaf がエラーになる)。

### `ParentModelHelper.setDefaults(...)`

`parent-home`(`TransitionTargetPageNameKeyword.PARENT_HOME_HTML`)を返す Controller メソッドは、必ず最初に `ParentModelHelper.setDefaults(model, loginUser, userService, questService, questTemplateService, spendingService)` を呼び出し、そのアクション固有の属性(`questList`, `activeTab` など)だけを後から追加・上書きする。これにより共有テンプレートが無条件に参照する必須属性(`childList`, `familyList`, 承認件数、初期化済みの `incomeExpenseForm` など)が補われる。`child-home` 側には同等のヘルパーがまだ存在せず、子供向けの各 Controller メソッドがモデルを個別に手動で組み立てている。

### 静的アセット

CSS/JS/画像は `src/main/resources/static/{css,js,images}` 配下に画面(タブ)単位で分かれて置かれている(例: `child-quest.css`, `parent-family.css`)。グローバル1枚のスタイルシートではない。

## mcp-server(MCPサーバー)

`mcp-server/` は Java アプリ本体とは独立した Node.js 製の MCP サーバーで、`mqdb` を Claude から直接参照・作成・更新するための Tool 群を提供する(削除系は未提供)。リポジトリ直下の `.mcp.json` で Claude Code に登録済み。詳細は `mcp-server/README.md` を参照。Java 側のエンティティ/ステータス定数を変更した場合、`mcp-server/src/tools/*.js` の記述(特にステータス値の意味を書いたdescription文言)が古くならないよう合わせて更新すること。
