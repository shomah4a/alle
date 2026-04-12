# ADR 0122: シェルスクリプトモード

## ステータス

承認済み

## コンテキスト

シェルスクリプト（`.sh`, `.bash`）を編集する際にシンタックスハイライトやオートインデントが必要である。
tree-sitter-bashがbonede版Javaバインディング（`io.github.bonede:tree-sitter-bash:0.25.1`）として利用可能であるため、tree-sitterベースで実装する。

## 決定

### シンタックスハイライト

tree-sitter-bashによる構文解析と、公式 `highlights.scm` によるハイライトを使用する。
キャプチャ名はすべて `DefaultCaptureMapping` でカバーされているため、言語固有のマッピングは不要。

### オートインデント

初期実装では `CStyleIndentCommands` を使用し、`{}` ベースのインデントを提供する。
シェル固有の構文（`if/fi`, `do/done`, `case/esac`）に基づくインデントは別タスクとして扱う。

### 対象拡張子

- `.sh`
- `.bash`

`.zsh` はzsh固有の構文があるため対象外とし、必要に応じて別モードで対応する。

### 設定

- `INDENT_WIDTH`: 2
- `COMMENT_STRING`: `"# "`

## 結果

既存のtree-sitterモード（JavaScript, JSON, YAML）と同じパターンで実装する。
すべて追記型の変更であり、既存コードへの影響はない。
