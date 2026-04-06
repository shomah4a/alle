# ADR 0115: CStyleIndentState の AST ベース統一

## ステータス

承認

## コンテキスト

CStyleIndentState には2種類のインデント判定が混在している。

1. AST ベース: `getBracketIndent` で `SyntaxTree.enclosingBracket` を使い、
   括弧ノード内のインデント位置を計算する
2. 正規表現ベース: `CStyleIndentConfig.openBracketEndPattern()` で
   「行末が開き括弧で終わるか」をテキストマッチで判定する

tree-sitter の AST がある以上、正規表現は劣化版であり、混在させると
判定の信頼性と保守性が下がる。YAML モードでは AST 単独の
`YamlIndentState` を新規作成した（ADR 0113）が、既存の JavaScript / JSON
モードにも同じ問題が残っている。

また `CStyleIndentState` のコンストラクタは `SyntaxAnalyzer` を nullable で
受け取るが、実際の利用箇所（JavaScriptMode, JsonMode）では必ず非 null を
渡しており、null はテストでのみ使われている。

## 決定

### SyntaxAnalyzer の必須化

`CStyleIndentState` のコンストラクタで `SyntaxAnalyzer` を non-null の
必須パラメータとする。テストでは tree-sitter パーサーを使用する。

### 正規表現フィールドの除去

`CStyleIndentConfig` から `openBracketEndPattern` と `closeBracketStartPattern` を
除去する。record のフィールドは `indentWidth`, `openBrackets`, `closeBrackets` の
3つのみとなる。

### AST ベースの行末開き括弧判定

`SyntaxTree.nodeAt()` で行末（またはカーソル位置直前）のトークンを取得し、
それが `openBrackets` に含まれる文字かを判定する。

コメント付き行末（例: `{ // comment`）では、行末トークンがコメントノードに
なるため、コメントノードをスキップして意味のある最後のトークンを探索する。
tree-sitter の AST では `comment` タイプのノードとして識別可能である。

### 判定対象の違い

- `cycleIndent`: 前行全体の末尾トークンを判定
- `newlineAndIndent`: カーソル位置直前のトークンを判定

共通の判定メソッドに位置パラメータを持たせて対応する。

## 結果

- CStyleIndentState から正規表現への依存が除去される
- CStyleIndentConfig が純粋な設定値（数値と文字集合のみ）になる
- SyntaxAnalyzer が必須となり、AST なしでのインデント判定という不整合がなくなる
- JavaScript / JSON モードのインデント判定が AST ベースに統一される
