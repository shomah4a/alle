# ADR 0102: バッファ切り替え時のビュー状態保存・復元

## ステータス

承認

## コンテキスト

Window でバッファを A → B → A と切り替えると、A に戻った際にカーソル位置(point)やスクロール位置がリセットされ、作業文脈が失われる。
ADR 0101 で導入した BufferHistoryEntry はバッファの識別情報のみを保持しており、ビュー固有の状態（point、表示開始行等）は記録していない。

## 決定

### BufferIdentifier の導入

既存の `BufferHistoryEntry` sealed interface を `BufferIdentifier` にリネームする。バッファの識別責務を明確にする。

```java
public sealed interface BufferIdentifier {
    record ByPath(Path path) implements BufferIdentifier {}
    record ByName(String name) implements BufferIdentifier {}
}
```

### ViewState の導入

ビュー固有の状態をまとめた record を導入する。

```java
public record ViewState(
    int point,
    int displayStartLine,
    int displayStartVisualLine,
    int displayStartColumn,
    @Nullable Integer mark
) {}
```

### BufferHistoryEntry の再定義

`BufferIdentifier` と `ViewState` を組み合わせたクラスとして再定義する。
`equals`/`hashCode` は `identifier` ベースで実装し、MRU リストの重複排除が識別子ベースで動作するようにする。
record ではなく通常のクラスとする。record の equals セマンティクス（全フィールド比較）と identifier ベースの等価性が矛盾するため。

### Window.setBuffer() の変更

- 同一バッファへの切り替えは noop とする（リセットしない）
- 切り替え前にビュー状態をキャプチャし、BufferHistoryEntry として履歴に記録する
- 切り替え先バッファが履歴にある場合、保存された ViewState で復元する
- 履歴にない場合は従来通りリセットする

### ViewState 復元時のクランプ

バッファ内容が変更されている可能性があるため、復元時に point と displayStartLine をバッファの範囲内にクランプする。
内部フィールドへの直接代入を行い、setPoint() の例外スローを回避する。

## 結果

- バッファを切り替えて戻った際にカーソル位置やスクロール位置が復元される
- 同一バッファへの setBuffer() は noop となり不要なリセットが発生しない
- BufferIdentifier と ViewState の分離により、識別とビュー状態の責務が明確になる
