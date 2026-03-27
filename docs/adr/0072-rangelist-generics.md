# ADR 0072: RangeList と FaceRangeList の汎用化

## ステータス

承認

## コンテキスト

`alle-core` の `buffer` パッケージに `RangeList` と `FaceRangeList` という2つのクラスが存在する。
両者は半開区間 `[start, end)` の複数範囲を管理するデータ構造であり、以下のメソッドがほぼ同一のロジックで実装されている:

- `remove(int start, int end)`
- `adjustForInsert(int index, int length)`
- `adjustForDelete(int index, int count)`
- `clear()`

差異は以下の通り:

- `RangeList`: 値を持たない区間管理。readOnly範囲・pointGuard範囲に使用。
- `FaceRangeList`: `Face` 値付きの区間管理。テキストスタイリングに使用。

また、`RangeList.put` には重複する既存範囲を丸ごと削除してしまう問題がある。
例えば `[0, 10)` が登録済みの状態で `put(3, 5)` を呼ぶと `[0, 10)` が消えて `[3, 5)` のみになる。
readOnly のような用途では範囲が拡大されるべきであり、この動作は不適切である。

## 決定

`RangeList<V>` としてジェネリック化し、`FaceRangeList` を廃止して統合する。

### put動作の統一

put はマージ方式に統一する:

1. `remove(start, end)` で既存範囲を分割保持
2. 新しいエントリを追加
3. 隣接する同値エントリを `Objects.equals` でマージ

値なし（`Void`）の用途では値が常に `null` であるため、隣接エントリは常にマージされる。
値あり（`Face`）の用途では、同じ `Face` の隣接エントリはマージされ、異なる `Face` は別エントリとして保持される。

### API

- `put(int start, int end, V value)` — 値あり put
- `put(int start, int end)` — 値なし put（`Void` 用オーバーロード、内部で `null` を渡す）
- `getEntries(int queryStart, int queryEnd)` — `getFaceSpans` の汎用版
- 既存の `contains`, `hasAny`, `findStart`, `findEnd` は維持

`TextPropertyStore` の `getFaceSpans` は `getEntries` の結果を `StyledSpan` に変換する。

## 結果

- コード重複が解消される
- `RangeList.put` の範囲縮小問題が修正される
- `TextPropertyStore` のパブリックインターフェースに変更はない
- 影響範囲は `buffer` パッケージ内に閉じる（両クラスともpackage-private）
