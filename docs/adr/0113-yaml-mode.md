# ADR 0113: YAML モードの追加

## ステータス

承認

## コンテキスト

エディタで `.yml`, `.yaml` ファイルを開いた際にシンタックスハイライトが効かない。
tree-sitter の Java バインディング (io.github.bonede) には YAML 用のパーサーが
存在するため、これを利用してモードを追加する。

## 決定

### モード構成

- **YamlMode**: `.yml`, `.yaml` ファイルに適用。tree-sitter-yaml を使用。

### tree-sitter バインディング

- `io.github.bonede:tree-sitter-yaml:0.5.0a`

既存の tree-sitter-python 等と同じ提供元のバインディングを使用する。

### highlights.scm の取得先

bonede のバージョン (0.5.0a) と上流リポジトリのタグが一致しないため、
`tree-sitter-grammars/tree-sitter-yaml` リポジトリの `v0.7.0` タグから取得する。
highlights.scm はクエリファイルであり、ノードタイプ名が一致していれば動作する。

### インデント機能

YAML 専用の AST ベースインデントステート (`YamlIndentState`) を新規作成した。
既存の `CStyleIndentState` は正規表現ベースの判定を含むが、
tree-sitter の AST がある以上、正規表現は劣化版でしかなく、
混在させると判定の信頼性と保守性を下げるため採用しなかった。

インデント増加の判定:
- 行末のトークンが AST 上でコロン `:` であるか（`tree.nodeAt` で判定）
- フロースタイル括弧 (`flow_mapping`, `flow_sequence`) 内であるか

インデントサイクルの候補は前行の状態から決定する。
現在行のインデント変更は AST に影響するため（例: インデントすると値として
パースされる）、候補計算には「前行の末尾トークンがコロンか」という
現在行に依存しない判定を使用する。
同一行での連続サイクルでは初回に計算した候補リストを再利用し、
AST 変更による候補の不安定化を防ぐ。

デフォルトインデント幅: 2

キーバインド:
- `Enter` → `yaml-newline-and-indent`
- `Tab` → `yaml-indent-line`
- `Shift+Tab` → `yaml-dedent-line`

### コメント機能

コメント文字列は `# ` を設定する。これにより `comment-dwim` 等の
既存コメントコマンドが YAML の `#` コメントに対応する。

### CStyleIndentState の正規表現依存について

CStyleIndentState は AST の `enclosingBracket` を使った判定と、
正規表現による行末括弧判定が混在している。
AST がある以上、正規表現は不要であり、AST に統一すべきである。
ただし影響範囲が JS/JSON モードに及ぶため、別タスクとして対応する。

## 結果

- `.yml`, `.yaml` ファイルでシンタックスハイライトが有効になる
- AST ベースのオートインデントが動作する
- `#` によるコメント操作が動作する
- YAML 専用のインデントステート (`YamlIndentState`, `YamlIndentCommands`) が追加される
