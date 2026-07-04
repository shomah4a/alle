# ADR 0142: Java モード

## ステータス

承認済み

## コンテキスト

Java ファイル（`.java`）を編集する際にシンタックスハイライトとオートインデントが必要である。
tree-sitter-java が bonede 版 Java バインディング（`io.github.bonede:tree-sitter-java:0.23.4`）として利用可能であるため、tree-sitter ベースで実装する。

モード実装の前例として TypeScript モード（ADR 0137）を踏襲する。

## 決定

### モード名

モード名は `java` とする。対象拡張子は `.java` のみ。

### shebang 登録

行わない。

- JEP 330（single-file source launcher）による `#!/usr/bin/java --source` 形式のスクリプトは存在するが、実利用パターンの調査が必要なため対象外
- 必要になれば follow-up タスクとして扱う

### tree-sitter バインディング

`io.github.bonede:tree-sitter-java:0.23.4` を採用する。

- Maven Central に存在することを確認済み（bonede 版の最新）
- 由来元 `tree-sitter/tree-sitter-java` の tag `v0.23.4` に `queries/highlights.scm` が存在することを確認済み
- 同 JAR 内に Java 用パーサ（`org.treesitter.TreeSitterJava`）とネイティブライブラリが含まれる

#### 既知のリスク: ネイティブクラッシュ報告

bonede/tree-sitter-ng の Issue #64 に、tree-sitter-ng 0.24.4 + tree-sitter-java 0.23.4 の組み合わせで
クエリマッチ反復中の SIGSEGV（非決定的）が報告されている（本 ADR 作成時点で未解決）。

- 本リポジトリの core バインディングは 0.26.6 であり修正済みの可能性があるが、未確認
- 発生時は例外ではなく JVM クラッシュとなり、エディタ全体が落ちる
- 軽減策として、数百行規模の Java テキストをスタイリングするテストを追加して検証する
- 参照: https://github.com/bonede/tree-sitter-ng/issues/64

### highlights.scm

tree-sitter-java の highlights.scm は単独で完結しており（`; inherits` ディレクティブなし）、
TypeScript のような親言語クエリの連結（`inheritsFrom`）は不要。既存のビルド時ダウンロード機構をそのまま使う。

### キャプチャ名マッピングの拡張

`DefaultCaptureMapping` に以下を追加する：

- `attribute` → `ANNOTATION`（Java アノテーション。`@Override` / `@JsonProperty(...)` 等の名前識別子）
- `string.escape` → `STRING`（文字列内エスケープシーケンス。`NodeFaceMapping.resolve` は完全一致のみのため、既存の `escape` キーでは解決されない）

#### 既存言語への影響: YAML ディレクティブの表示変化（意図的変更）

`@attribute` は YAML の highlights.scm にも出現する（`yaml_directive` / `tag_directive` / `reserved_directive`）。
本マッピング追加により、従来未スタイルだった YAML ディレクティブ（`%YAML 1.2` 等）が
ANNOTATION face で描画されるようになる。

- この変化は**意図的変更として受容する**（防衛的計画評価 HIGH-1 に対するユーザー決定）
- ディレクティブは YAML における言語指示子でありアノテーションと意味論が近いこと、
  キャプチャマッピングを全言語共有の 1 系統に保てることが理由
- YAML ディレクティブのハイライトをテストで固定し、サイレントな挙動変化として残さない
- `string.escape` は既存言語の highlights.scm に出現しないため影響なし

### `#match?` プレディケート未評価の扱い

`TreeSitterStyler` はクエリの `#match?` プレディケートを評価しない。tree-sitter-java の
highlights.scm には `#match?` 付きパターンが 5 つあり、未評価に起因して以下の可能性がある：

- `foo.bar()` の小文字レシーバ `foo` が `@type`（TYPE face）として誤ハイライトされうる
- 全識別子が `@constant` にマッチしうるが、`@constant` / `@variable` とも VARIABLE にマップされるため表示上の差はない

これは JS / TS / Python の既存挙動と地続きの制約であり、本モードでは実挙動を
characterization テストで固定して受容する。プレディケート評価の実装は本 ADR のスコープ外
（対応する場合は全言語に影響する `TreeSitterStyler` の改修となる）。

実装時に確認した実挙動:

- メソッド呼び出しのレシーバ識別子は大文字・小文字によらず VARIABLE face になる
  （`foo.bar()` の `foo` も `Foo.bar()` の `Foo` も VARIABLE。メソッド名は FUNCTION_NAME）
- 懸念していた「小文字レシーバの TYPE への誤昇格」は発生しない。`TreeSitterStyler` の
  重複解決が同一範囲では先に返されたマッチを採用する仕組みであり、highlights.scm 先頭の
  汎用パターン `(identifier) @variable` が `#match?` 付きの `@type` パターンより優先されるため
- 副作用として、大文字始まりレシーバも TYPE にならず VARIABLE に倒れる
- 以上を JavaModeTest の characterization テスト 2 件で固定済み

### bracket types

`CStyleIndentState` の `enclosingBracket` 解決に使う括弧系ノードタイプとして以下を登録する
（実装時に tree-sitter-java v0.23.4 の node-types.json と突き合わせて過不足を検証する）：

- `parenthesized_expression`, `argument_list`, `formal_parameters`, `inferred_parameters`,
  `annotation_argument_list`, `resource_specification`
- `array_initializer`, `array_access`, `element_value_array_initializer`
- `block`, `constructor_body`, `class_body`, `interface_body`, `enum_body`,
  `annotation_type_body`, `switch_block`, `module_body`

#### 非対応の判断

- `type_parameters` / `type_arguments`（`<T, U>`）は `CStyleIndentConfig` が文字ベース判定
  （`(`, `[`, `{`）であるため対象外（ADR 0137 と同じ判断）

### コメントノード型の言語別注入（ADR 0115 の部分変更）

実装安全性評価で、`CStyleIndentState` のコメントスキップ判定がノード型名 `comment` を
ハードコードしており（ADR 0115 の設計）、tree-sitter-java のコメントノード型
`line_comment` / `block_comment` に一致しない欠陥が指摘された。再現テストで以下を確認した：

- `class Foo { // comment` 行末での newline-and-indent が、新行をインデント幅 4 ではなく
  コメント開始カラム（12）に整列させる
- `void f() { // comment` の次行での indent サイクルも同様にコメント開始カラムに整列する
- 原因は `findFirstContentChild` / `isOpenBracketBeforeColumn` が `line_comment` を
  スキップせず「意味のあるトークン」と誤認するため。既存 7 言語はすべて `comment` 型のため
  影響は Java のみ

対応として、コメントとみなすノード型集合を `CStyleIndentConfig` に追加し、言語ごとに
明示的に注入する方式を採用する（ユーザー決定）。

- 括弧文字（openBrackets / closeBrackets）と同じ「言語固有値は config が持つ」方針に揃える
- 既存言語（JavaScript / TypeScript / Terraform / JSON / ShellScript）は `comment` を、
  Java は `line_comment` / `block_comment` を注入する
  （実装時の構築箇所 grep で JsonMode / ShellScriptMode も CStyleIndentConfig を
  構築していることを確認したため、注入対象に含めた）
- ハードコード定数 `COMMENT_NODE_TYPE` は廃止する

### bracket types の追加（実装安全性評価 LOW-1 対応）

node-types.json で実在を確認したうえで、以下 2 ノード型を JAVA_BRACKET_TYPES に追加する：

- `element_value_array_initializer`（`@Target({ElementType.METHOD, ...})` のアノテーション引数配列の `{}`）
- `module_body`（module-info.java の `module ... {}`）

### 設定

- `INDENT_WIDTH`: 4（Java の一般慣習）
- `COMMENT_STRING`: `"// "`

## 結果

- `.java` ファイルでシンタックスハイライト・オートインデントが有効になる
- `// ` によるコメント操作が動作する
- Java アノテーションが ANNOTATION face で描画される
- YAML ディレクティブ（`%YAML` 等）が未スタイルから ANNOTATION face に変わる（意図的変更、テストで固定）
- `#match?` 未評価に起因するハイライト挙動は characterization テストで固定される
