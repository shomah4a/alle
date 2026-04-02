# ADR 0101: ウィンドウのバッファ履歴をMRUリストに拡張する

## ステータス

承認

→ ADR 0102 で BufferHistoryEntry にビュー状態を追加し、BufferIdentifier を分離した。

## コンテキスト

Window は直前に表示していたバッファを `previousBuffer` として単一の BufferFacade 参照で保持していた。
kill-buffer を連続で発行した場合、1回目の kill で previousBuffer が消費されると2回目以降は `findReplacementBuffer` のフォールバックに頼ることになり、ユーザーの作業文脈に沿わないバッファに切り替わる。

また、BufferFacade の強参照を保持するため、kill 済みバッファの GC が阻害されるリスクがあった。

ADR 0095 で previousBuffer を優先する仕組みを導入したが、履歴が1つしかないという制約は残っていた。

→ ADR 0095 の判断を本 ADR で拡張する。

## 決定

### バッファ識別子の型

バッファ履歴のエントリとして sealed interface `BufferHistoryEntry` を導入する。

```java
public sealed interface BufferHistoryEntry {
    record ByPath(Path path) implements BufferHistoryEntry {}
    record ByName(String name) implements BufferHistoryEntry {}
}
```

- ファイルパスを持つバッファは `ByPath` で記録する。`BufferNameUniquifier` による displayName 変更の影響を受けない。
- ファイルパスを持たないバッファ（`*scratch*` 等）は `ByName` で記録する。

### Window の変更

- `previousBuffer` フィールドを廃止し、`MutableList<BufferHistoryEntry>` に置換する。
- `setBuffer()` 時に旧バッファの識別子を履歴先頭に追加する。同一バッファへの切り替えは記録しない。重複エントリは除去する。
- 外部には `ListIterable<BufferHistoryEntry>` として公開する。

### バッファ解決

履歴エントリからバッファを解決する際は:
- `ByPath` → `BufferManager.findByPath()`
- `ByName` → `BufferManager.findByName()`

解決できないエントリ（既に削除済み等）はスキップして次のエントリを試行する。

### MRU リストの上限

上限は設けない。kill-buffer 時に該当エントリを除去するため、存在するバッファ数が事実上の上限となる。

## 結果

- kill-buffer を連続で発行しても、履歴に基づいて作業文脈に沿ったバッファに切り替えられる
- BufferFacade の強参照を保持しないため、GC が阻害されない
- displayName の変更に対してパスベースの識別で堅牢である
- ADR 0095 の previousBuffer 優先の考え方を自然に拡張している
