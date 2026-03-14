# ADR-0008: コード品質ツール

## ステータス

承認済み

## コンテキスト

コードの品質と一貫性を維持するため、フォーマッター・静的解析・Null安全性チェックを導入する。

## 決定

以下のツールをサブプロジェクト共通で適用する。

### フォーマッター: Spotless + Palantir Java Format

- 4スペースインデント
- import順序の自動整列
- 未使用importの自動削除

### ワイルドカードインポート禁止

- `import *` をカスタムタスク(`checkNoWildcardImports`)でエラーにする
- `spotlessCheck` の `finalizedBy` として実行

### 静的解析: ErrorProne

- コンパイル時に静的解析を実行
- `UnicodeInCode` はテストメソッド名に日本語を使用するため無効化

### Null安全性: NullAway + JSpecify

- `@NullMarked` アノテーションが付与されたコードに対してのみNullチェックを適用(`onlyNullMarked = true`)
- NullAwayの指摘はエラーとして扱う

## 帰結

- コードスタイルが自動的に統一される
- コンパイル時にバグの可能性を検出できる
- Null安全性を段階的に導入できる（`@NullMarked` を付与したパッケージから順次）
