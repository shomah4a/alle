# ADR 0114: tree-sitter キャプチャ優先度の解決

## ステータス

承認

## コンテキスト

tree-sitter の highlights.scm では、同一ノードに対して複数のキャプチャパターンが
マッチすることがある。例えば YAML のマッピングキーでは `string_scalar` ノードに
`@string` と `@property` の両方がマッチする。

tree-sitter の慣習では、highlights.scm で後に定義されたパターンが優先される
（より具体的なパターンが後に書かれる）。しかし現在の `TreeSitterStyler` は
全キャプチャのスパンをそのまま保持しており、同一範囲の重複が解決されない。
描画側（`ScreenRenderer`）は最初に見つかったスパンを使うため、汎用的な
`@string` が優先され、具体的な `@property` が無視される。

## 決定

### 修正箇所

`TreeSitterStyler.styleDocument()` でスパンを start 順にソートした後、
同一範囲 `[start, end)` の重複スパンを解決する。同一範囲のスパンが複数存在する場合、
リスト内で最初のものを残し、それ以降のものを除去する。

### 責務の分離

- `TreeSitterStyler`: 重複解決済みのスパンリストを返す責務
- `ScreenRenderer`: 渡されたスパンを描画するだけの責務（変更なし）
- `RenderSnapshotFactory.mergeWithTextPropertyFace()`: 変更なし（入力が整理済み）

### ソート安定性

`TSQueryCursor.nextMatch()` は、より具体的なパターン（親ノードの条件を含むパターン等）の
マッチを先に返す。例えば YAML の `string_scalar` ノードに対して、
`(block_mapping_pair key: ... (string_scalar) @property)` のマッチが
汎用の `(string_scalar) @string` より先に返される。

Java の `List.sort()` は安定ソートのため、同一 start のスパンの追加順序が維持される。
よって、同一範囲のスパンのうちリスト先頭にあるものが最も具体的なパターンに対応する。

### 返却順序への依存に関する注意

`TSQueryCursor.nextMatch()` が「具体的なパターンのマッチを先に返す」という動作は、
tree-sitter の Java バインディング (io.github.bonede) 0.26.6 での実測に基づく。
tree-sitter 公式ドキュメントではパターンID昇順（= 定義順）でマッチを返すとされているが、
同一ノードに複数パターンがマッチした場合の具体的な順序保証は明確ではない。

この順序が将来のバージョンアップで変更された場合、
`TreeSitterStylerYamlTest` の「キャプチャ優先度」テストで検知可能である。

## 結果

- YAML のマッピングキーが `@property`（VARIABLE）としてハイライトされる
- 他の言語で同一ノードに複数キャプチャがマッチするケースも正しく解決される
- 描画側の変更は不要
