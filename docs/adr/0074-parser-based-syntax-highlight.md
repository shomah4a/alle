# ADR 0074: パーサーベースシンタックスハイライトの導入

## ステータス

承認

## コンテキスト

現在のシンタックスハイライトは正規表現ベース（RegexStyler + StylingRule）で実装されている。
この方式はシンプルだが、構文解析の精度に限界がある。
例えばPythonモードでは変数名のハイライトができず、キーワードと組み込み関数のパターンマッチに留まっている。

Tree-sitterのようなパーサーライブラリを使うことで、構文木に基づいた正確なハイライトが可能になる。

### 関連ADR

- ADR 0061: Pythonモードのスクリプト側実装

## 決定

### ライブラリ選定

bonede/tree-sitter-ng（`io.github.bonede:tree-sitter` + 言語別アーティファクト）を使用する。

- JDK 8+対応（プロジェクトはJDK 21）
- JNIベース、Maven Central配布
- 活発なメンテナンス
- 言語別にMavenアーティファクトが分離されている

### SyntaxStyler の拡張

既存の行単位API（`styleLine`/`styleLineWithState`）はTree-sitterと統合できない。
行番号もバッファ全体テキストも受け取れないため。

`styleDocument` デフォルトメソッドを追加する。

```java
default ListIterable<ListIterable<StyledSpan>> styleDocument(ListIterable<String> lines)
```

- デフォルト実装は既存の `styleLineWithState` に委譲し、行ごとに結果を組み立てる
- RegexStyler/MarkdownStyler はデフォルト実装で動作継続
- TreeSitterStyler は `styleDocument` をオーバーライドし全文パース
- `RenderSnapshotFactory` は `styleDocument` を呼ぶように変更

### TreeSitterStyler

- `SyntaxStyler` を実装し、`styleDocument` をオーバーライド
- コンストラクタで言語（TSLanguage）とノード種別→FaceNameマッピングを受け取る
- UTF-8バイトオフセット→コードポイントオフセット変換を内部で処理

### ParserStylerRegistry

- 言語名（"python"等）→ SyntaxStyler ファクトリのレジストリ
- alle-core に配置
- Java側で組み込み言語のマッピングを登録

### スクリプト側API

`alle/internal/styling.py` に `parser_styler(language)` を追加する。
スクリプト側からはTree-sitterという実装詳細は見えない。

```python
def parser_styler(language: str) -> Any:
    """パーサーベースのスタイラーを取得する。"""
```

### ADR 0061 との関係

ADR 0061の方針（Pythonモードはスクリプト側で実装）は維持する。
スタイラーの生成方法が `regex_styler(rules)` から `parser_styler("python")` に変わるのみ。

### FaceName の拡充

Tree-sitterが提供するセマンティック情報に対応する定数を追加する。

- TYPE: 型名
- FUNCTION_NAME: 関数・メソッド名
- VARIABLE: 変数名
- OPERATOR: 演算子
- BUILTIN: 組み込み関数・定数

### RegexStyler の位置づけ

RegexStyler は廃止しない。
Tree-sitterグラマーが存在しない言語（Markdown等）のフォールバックとして残す。

## 根拠

- Tree-sitterは多くのエディタ（Neovim, Helix, Zed等）で採用されており、実績がある
- インクリメンタルパース対応で将来の性能最適化が可能
- パーサーの実装詳細をスクリプトAPIから隠蔽することで、将来のライブラリ差し替えにも対応できる
- 既存のSyntaxStylerインターフェースにデフォルトメソッドを追加する方式により、後方互換性を維持できる
