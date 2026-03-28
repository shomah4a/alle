# ADR 0075: TreeSitterStylerのキャッシュとインクリメンタルパース

## ステータス

承認

## コンテキスト

ADR 0074 で導入した TreeSitterStyler は、`styleDocument()` の呼び出しごとに
TSParser の生成・全文パース・クエリ実行・リソース破棄を行っている。
`RenderSnapshotFactory.create()` はレンダリングフレームごとにこのメソッドを呼ぶため、
テキスト変更がないスクロールやカーソル移動でもフルパースが発生し、不要な負荷がかかっている。

### 関連ADR

- ADR 0074: パーサーベースシンタックスハイライトの導入

## 決定

### テキスト未変更時のキャッシュ

TreeSitterStyler に前回のテキストとスタイリング結果をキャッシュする。

- `cachedText`: 前回の `styleDocument()` に渡されたフルテキスト
- `cachedResult`: 前回のスタイリング結果

`styleDocument()` 呼び出し時にテキストが前回と同一であればキャッシュ結果を返す。
スクロール・カーソル移動・ウィンドウリサイズ等、テキスト変更を伴わないレンダリングでパースを完全にスキップする。

### TSTreeキャッシュとインクリメンタルパース

テキスト変更時には tree-sitter のインクリメンタルパースを使用して差分のみ再解析する。

- `cachedTree`: 前回の TSTree をフィールドに保持
- テキスト変更時に旧テキストと新テキストの共通 prefix/suffix から変更箇所を特定
- `TSInputEdit` を構築して `cachedTree.edit()` を呼び、`parseString(cachedTree, newText)` で差分パース
- パース後は古い TSTree を明示的に close し、新しい TSTree をキャッシュ

### 差分検出アルゴリズム

外部インターフェース（SyntaxStyler, Buffer 等）の変更なしに、`styleDocument()` 内で完結する。

1. 旧テキストと新テキストの先頭から一致するコードポイント数（共通プレフィックス）を計算
2. 末尾から一致するコードポイント数（共通サフィックス）を計算（プレフィックスと重複しないよう制限）
3. 変更領域のコードポイントオフセットを UTF-8 バイトオフセットと TSPoint に変換
4. `TSInputEdit` を構築

### リソース管理

- tree-sitter-ng の TSTree は Cleaner パターンを実装しており、GC 時にネイティブリソースが自動解放される
- TreeSitterStyler に AutoCloseable は不要
- 新しい tree 生成時に古い tree は明示的に `close()` でタイムリーに解放
- インクリメンタルパースが失敗した場合はフルパースにフォールバック

### Utf8OffsetConverter の拡張

TSInputEdit の構築に必要な以下のメソッドを追加する。

- `codePointOffsetToUtf8ByteOffset`: コードポイントオフセット → UTF-8 バイトオフセット
- `codePointOffsetToRowColumn`: コードポイントオフセット → 行番号と行内 UTF-8 バイト列オフセット

### スコープ

- 変更対象は `TreeSitterStyler.java` と `Utf8OffsetConverter.java`
- `SyntaxStyler` インターフェースの変更は不要
- `RenderSnapshotFactory` の変更は不要
- スタイラーはバッファごとのモードインスタンスが保持しており、ステートのスコープはバッファに閉じている

## 根拠

- テキスト未変更時のキャッシュにより、スクロール等での不要なパースを完全に排除できる
- インクリメンタルパースにより、テキスト変更時も変更箇所周辺のみの再解析で済む
- 差分検出を `styleDocument()` 内で行うことで、外部インターフェースの変更が不要
- TSTree の Cleaner 実装により、明示的なライフサイクル管理なしでもリソースリークを防止できる
- フォールバック機構により、インクリメンタルパース失敗時も安全に動作する
