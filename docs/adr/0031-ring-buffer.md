# ADR 0031: 汎用リングバッファ

## ステータス

承認

## コンテキスト

メッセージバッファ（`*Messages*`）の実装にあたり、一定行数を保持するリングバッファが必要になった。
既存の依存ライブラリ（Eclipse Collections, JDK標準）に適切なリングバッファ実装がないため、
`libs` 以下に汎用モジュールとして作成する。

## 決定

### モジュール構成

- `libs/ring-buffer` に配置
- パッケージ: `io.github.shomah4a.alle.libs.ringbuffer`

### インターフェース

- `RingBuffer<T>` インターフェースを定義
- `Iterable<T>` を実装

### 実装

- `ArrayRingBuffer<T>`: `Object[]` + head + size による循環配列実装
- 先頭・末尾の操作が O(1)
- 容量超過時は最古の要素を上書き
- null要素は禁止
- 容量は1以上を要求

### unchecked cast について

Javaのジェネリクスの型消去により `Object[]` → `T` のキャストは構造的に不可避。
内部実装の `get` メソッド1箇所のみ `@SuppressWarnings("unchecked")` を許容する。

## 影響

- 既存コードへの変更は `settings.gradle.kts` の1行追加のみ
- 将来的にメッセージバッファで `RingBuffer<String>` として利用予定
