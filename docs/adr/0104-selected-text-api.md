# ADR 0104: 選択テキスト取得APIの追加

## ステータス

承認済み

## コンテキスト

スクリプトからカレントバッファの選択中文字列を取得するには、`region_start()` / `region_end()` でリージョン境界を取得し、`buffer.substring(start, end)` で文字列を抽出する必要がある。
この組み合わせは頻出パターンであるため、便利メソッドとして直接提供する。

## 決定

`WindowFacade` に `selectedText()` メソッドを追加し、Python側の `Window` クラスにも `selected_text()` を追加する。

### 返り値の型

- Java側: `Optional<String>` を返す
- Python側: `str | None` を返す

mark未設定（選択なし）の場合は空値（`Optional.empty()` / `None`）を返す。
mark == point の場合は空文字列 `""` を返す。

これにより「選択操作をしていない」と「選択範囲が0文字」を区別できる。
既存の `getRegionStart()` / `getRegionEnd()` が `Optional<Integer>` を返す設計と一貫性がある。

## 影響

- 既存APIへの破壊的変更なし（メソッド追加のみ）
- `WindowFacade` / `window.py` の2ファイルに変更
