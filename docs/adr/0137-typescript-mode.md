# ADR 0137: TypeScript モード

## ステータス

承認済み

## コンテキスト

TypeScript ファイル（`.ts`, `.mts`, `.cts`）を編集する際にシンタックスハイライトとオートインデントが必要である。
tree-sitter-typescript が bonede 版 Java バインディング（`io.github.bonede:tree-sitter-typescript:0.23.2`）として利用可能であるため、tree-sitter ベースで実装する。

## 決定

### モード名

モード名は `typescript` とする。対象拡張子は `.ts` / `.mts` / `.cts`。

### TSX (`.tsx`) の取り扱い

**本 ADR の対象外**とする。

- TSX は別パーサ（tree-sitter-tsx）を要するため、追加依存・追加ノードタイプ・別 highlights.scm が必要となる
- `.tsx` を `typescript-mode` で代用すると JSX 構文がパースエラーになる懸念があるため、`.tsx` には拡張子登録自体を行わない（= `text-mode` フォールバック。現状からの後退は発生しない）
- 後続タスクとして `tsx-mode` 追加を `docs/tasks.md` に積む

### shebang 登録

`ts-node` のみを登録する。

- `#!/usr/bin/env ts-node` 形式の TypeScript スクリプトに対応
- `tsx` / `bun` / `deno` は実利用パターンの調査が必要なため後続タスクとして `docs/tasks.md` に積む

### tree-sitter バインディング

`io.github.bonede:tree-sitter-typescript:0.23.2` を採用する。

- 由来元 `tree-sitter/tree-sitter-typescript` v0.23.2 に `queries/highlights.scm` が存在する
- 同 JAR 内に TypeScript 用パーサ（`org.treesitter.TreeSitterTypescript`）とネイティブライブラリが含まれる

### highlights.scm の継承機構

tree-sitter-typescript の `queries/highlights.scm` は **JavaScript の highlights.scm を継承する前提**で書かれており、TypeScript 固有のキャプチャ（`@type`, `@type.builtin`, `@variable.parameter`, `@keyword`）のみを定義している。
TypeScript 文法は JavaScript の上位互換であり JavaScript の AST ノード型を流用する。

そのため、最終的なクエリは `javascript の highlights.scm` + `typescript の highlights.scm` を **連結したもの**を使う。nvim-treesitter 等は `; inherits: ecma` ディレクティブで実現しているが、本プロジェクトの `HighlightQueryLoader` はそれを解釈しないため、`build.gradle.kts` でビルド時に連結する。

#### 実装方式

- `TreeSitterGrammar` に `inheritsFrom: String?` フィールドを追加
- `inheritsFrom` が指定された場合、その言語の highlights.scm を先頭に連結して書き込む
- 親言語をリスト内で先に配置する制約を持つ（同一ループ内で逐次処理されるため）

#### キャッシュ無効化（フィンガープリント方式）

従来の `if (!destFile.exists())` 単独判定では、親言語のバージョンを更新しても子言語の連結結果が古いまま残る問題がある。
そのため、生成 highlights.scm の隣に `${language}.fingerprint` ファイルを置き、`tag` および親言語の `tag` を記録する。フィンガープリント不一致または欠落の場合は再ダウンロードする。

#### 既存言語への影響

- `inheritsFrom` のデフォルトは `null`。既存 6 言語（python / javascript / json / yaml / bash / hcl）は引数省略で従来どおり動作する
- フィンガープリント機構は既存言語にも適用されるが、初回ビルド時にフィンガープリントが欠落するため再ダウンロードが 1 度走る。以降は同一フィンガープリントでキャッシュヒットする

### キャプチャ名マッピングの拡張

`DefaultCaptureMapping` に以下を追加する：

- `@type.builtin` → `BUILTIN`（TypeScript の `number` / `string` / `boolean` 等の組み込み型）
- `@variable.parameter` → `VARIABLE`（関数パラメータ）

`@punctuation.bracket` は TypeScript の highlights.scm が使用するが、対応 FaceName が存在しないため未マップのまま（DEFAULT フォールバック）。これは JavaScript / HCL / YAML での既存挙動と同じ。

#### 既存言語への影響

事前調査の結果、`@type.builtin` / `@variable.parameter` は **既存 6 言語の highlights.scm に一切出現しない**。したがってマッピング追加は新規キャプチャ名のみの拡張であり、既存言語のハイライト挙動は変化しない。

### bracket types

JavaScript 用の bracket types に加え、TypeScript 固有の以下を追加する：

- `interface_body`（`interface Foo { ... }`）
- `enum_body`（`enum Color { ... }`）
- `object_type`（`{ a: string; b: number; }` 形式の型リテラル）
- `tuple_type`（`[A, B]` 形式のタプル型）

#### 非対応の判断

- `type_parameters` / `type_arguments`（`<T, U>`）は `CStyleIndentConfig` が文字ベース判定（`(`, `[`, `{`）であるため対象外
- ジェネリクスを複数行に分割するケースは稀であり、実害は限定的
- 対応するには `CStyleIndent` 全体の改修が必要で、JavaScript / Terraform を含む既存モードに波及するため見送り

### 設定

- `INDENT_WIDTH`: 2（TypeScript / Prettier のデファクト）
- `COMMENT_STRING`: `"// "`（JavaScript と同じ）

### `variable.parameter` の粒度

`@variable.parameter` は `variable.member` 等と同様に `VARIABLE` にマップする初期実装とする。
関数パラメータを別 FaceName で強調表示するには `FaceName.PARAMETER` 相当の新設が必要だが、本タスクのスコープ外。
要望があれば後続タスクとして検討する（`docs/tasks.md` 参照）。

## 結果

- `.ts` / `.mts` / `.cts` ファイルでシンタックスハイライト・オートインデントが有効になる
- `// ` によるコメント操作が動作する
- `TreeSitterGrammar` の `inheritsFrom` 機構が今後の派生言語（CSS と SCSS、Bash と Zsh 等）でも再利用可能になる
- フィンガープリントによるキャッシュ無効化により、親言語のバージョン更新時に派生言語側も自動で再生成される
- `@type.builtin` / `@variable.parameter` のキャプチャマッピング追加は新規キャプチャ名のみで既存言語のハイライト挙動には影響しない
- `.tsx` / 追加 shebang / `variable.parameter` 粒度向上は follow-up タスク（`docs/tasks.md` 参照）
