# ADR 0121: makefile-mode

## ステータス

承認

## コンテキスト

Makefile編集のためのメジャーモードが存在しない。Makefileはインデントにタブ文字が必須であるなど、他の言語モードとは異なる要件を持つ。

## 決定

### makefile-mode の新規作成

RegexStylerベースのシンタックスハイライトと、タブ文字対応のオートインデント/サイクルインデントを提供するメジャーモードを作成する。

### INDENT_TABS_MODE 設定の追加

インデントにタブ文字を使用するかどうかの汎用設定 `INDENT_TABS_MODE` を `EditorSettings` に追加する。デフォルトは `false`。makefile-mode では `settingDefaults()` で `true` をモードデフォルトとして設定し、バッファのモード切替時に `BufferLocalSettings` の5層解決を通じて自動的に反映される。MakefileIndentState はバッファの `INDENT_TABS_MODE` 設定を参照してインデント文字列（タブまたはスペース）を決定する。

### AutoModeMap のファイル名マッチ対応

Makefile は拡張子を持たないため、`AutoModeMap` にファイル名完全一致マッチ機能を追加する。優先順位は「ファイル名完全一致 > 拡張子マッチ > デフォルト」とする。

### インデント方式

- tree-sitter は使用せず、正規表現ベースで行テキストを判定する
- ターゲット行（変数代入を除外した上でコロン判定）の後はタブインデント
- レシピ行（タブで始まる行）の後はタブ継続
- サイクルインデント: インデントなし / タブ1つ のサイクル

### ハイライト対象

- コメント (`# ...`)
- ターゲット行 (`target: deps`)
- 変数定義 (`VAR =`, `VAR :=`, `VAR ?=`, `VAR +=`)
- 自動変数 (`$@`, `$<`, `$^`, `$?`, `$*`)
- 変数参照・関数呼び出し (`$(VAR)`, `${VAR}`, `$(func args)`)
- 特殊ターゲット (`.PHONY` 等)
- ディレクティブ (`include`, `ifeq`, `ifdef`, `ifndef`, `else`, `endif`, `define`, `endef`, `export`, `unexport`, `override`, `vpath`)

## 影響

- `EditorSettings` に `INDENT_TABS_MODE` が追加される（既存モードへの影響なし、デフォルト false）
- `AutoModeMap` にファイル名マッチ機能が追加される（既存の拡張子マッチは変更なし）
- 将来的に他のモード（例: Go）でも `INDENT_TABS_MODE = true` を利用可能
