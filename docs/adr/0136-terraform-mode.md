# ADR 0136: Terraform (HCL) モード

## ステータス

承認済み

## コンテキスト

Terraform / HCL ファイル（`.tf`, `.tfvars`, `.hcl`）を編集する際にシンタックスハイライトとオートインデントが必要である。
tree-sitter-hcl が bonede 版 Java バインディング（`io.github.bonede:tree-sitter-hcl:1.1.0a`）として利用可能であるため、tree-sitter ベースで実装する。

## 決定

### モード名

モード名は `terraform` とする。ユーザー体験を優先し、Terraform を使うケースが大半であろうという想定。
将来 Nomad / Packer / Vault 等の HCL 派生をサポートする際は、その時点でモード名を再考する（汎用 `hcl` モードへの分離、もしくは派生別の独立モード）。

### 対象拡張子

- `.tf`
- `.tfvars`
- `.hcl`

`.hcl` は Nomad/Packer/Consul/Vault 等でも使われるが、それらのほとんどは Terraform と同じ汎用 HCL ハイライトで実害がないため、現時点では同一モードで扱う。

### shebang 登録

非対応とする。HCL は実行可能スクリプトではないため。

### シンタックスハイライト

tree-sitter-hcl による構文解析と、`nvim-treesitter` リポジトリの `queries/hcl/highlights.scm` を使用する。

#### highlights.scm の取得元

- bonede 1.1.0a の由来元 `tree-sitter-grammars/tree-sitter-hcl` v1.1.x には `queries/` ディレクトリが存在しない
- 旧版 `mitchellh/tree-sitter-hcl` には highlights.scm があるが、ノード名が古い（`numeric_literal` / `string_literal` / `(true) (false)`）。`tree-sitter-grammars` 系の新ノード名（`numeric_lit` / `bool_lit`）と乖離しており、サイレントにハイライトされない懸念があるため不採用とする
- `nvim-treesitter` リポジトリの `queries/hcl/highlights.scm` は新ノード名（`numeric_lit` / `bool_lit` / `null_lit` 等）に基づいており、bonede 1.1.0a と整合する
- ADR 0113 の YAML 同様、bonede の由来元と異なるリポジトリから highlights.scm を取得する

#### キャプチャ名マッピングの拡張

`nvim-treesitter` の hcl/highlights.scm は以下のキャプチャを使うが、現行 `DefaultCaptureMapping` ではマッピングが未定義のものがある。これらを `DefaultCaptureMapping` に追加する（汎用的な公式キャプチャ慣習であり、将来他言語でも利用できる）。

- `@keyword.repeat` / `@keyword.conditional` → `KEYWORD`
- `@boolean` → `BUILTIN`
- `@variable.member` → `VARIABLE`

`@punctuation.delimiter` / `@none` / `@spell` 等は対応 FaceName が無いため未マップ（フォールバックで DEFAULT）。

### オートインデント

初期実装では `CStyleIndentCommands` を使用し、`{}` / `[]` / `()` ベースのインデントを提供する。

#### HCL 固有の制約

- **heredoc 内のインデントは対象外**：`<<EOF ... EOF` 内では C スタイルインデントが意図しない動作をする可能性があるが、本タスクの範囲外とする。実用上は heredoc 内で改行しても致命的な編集体験悪化が起きないことをテストで確認する
- **`HCL_BRACKET_TYPES` の決定**：tree-sitter-hcl の実 AST に基づき決定する。`body` ノードはファイル全体や block 本体に対応するが、トップレベル `body` には対応する物理的な `{}` が無く、すべての行が +1 インデントされる退行リスクがあるため、含めないか、含めても無害な扱いとなるかを実装時に検証する

### 設定

- `INDENT_WIDTH`: 2（Terraform `fmt` のデフォルト）
- `COMMENT_STRING`: `"# "`（HCL は `#` / `//` をサポートするが、Terraform 公式は `#` を推奨）

## 結果

既存の tree-sitter モード（JavaScript / JSON / YAML / Shell）と同じパターンで実装する。
すべて追記型の変更であり、既存コードへの影響はない。`DefaultCaptureMapping` への追加は新規キャプチャのみで既存マッピングの変更は無いため、既存言語のハイライトには影響しない。
