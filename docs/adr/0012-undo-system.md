# ADR-0012: Undo/Redoシステムの基盤

## ステータス

承認済み

**関連**: [ADR-0067](0067-undo-transaction.md) でundo記録の自動化・トランザクション機能・UndoEntry廃止を実施。

## コンテキスト

エディタにUndo/Redo機能を提供するための基盤設計が必要である。
副作用の外部化方針に従い、Buffer自身は履歴を持たず、
操作の逆操作を返すことで外部での履歴管理を可能にする。

## 決定

### TextChange

- テキスト変更操作を表すsealed interface
- `Insert(offset, text)` と `Delete(offset, text)` の2種類
- `inverse()` で逆操作を返す（InsertはDelete、DeleteはInsert）

```java
sealed interface TextChange {
    record Insert(int offset, String text) implements TextChange {}
    record Delete(int offset, String text) implements TextChange {}

    default TextChange inverse() {
        return switch (this) {
            case Insert(var o, var t) -> new Delete(o, t);
            case Delete(var o, var t) -> new Insert(o, t);
        };
    }
}
```

### Bufferの操作が逆操作を返す

- `Buffer.insertText(index, text)` → `TextChange.Delete` を返す
- `Buffer.deleteText(index, count)` → `TextChange.Insert` を返す（削除前にテキストを取得）
- `Buffer.apply(TextChange)` → TextChangeを適用し、逆操作の `TextChange` を返す
- Bufferは履歴を持たない。履歴管理は外部（将来のUndoManager等）の責務

### 削除時のテキスト保持

`Delete` レコードは削除対象のテキスト内容を保持する。
`Buffer.deleteText()` は削除前に `substring` でテキストを取得してから削除を実行する。
この順序は不変条件として守る必要がある。

### 初期ロード時のUndo対象外方針

`BufferIO.load()` でのテキスト挿入はUndo対象外とする。
将来Undo履歴管理を導入する際、ロード時の挿入はUndoスタックに積まない設計とする。
現時点ではinsertTextの戻り値を無視することでこの方針を満たす。

### Undo方式の決定は保留

Emacsスタイル（undo-only）か明示的Redo（2スタック方式）かは、
履歴管理の実装時に決定する。TextChangeの基盤はどちらの方式にも対応可能。

## 帰結

- Bufferが履歴を持たないため、テストが容易で責務が明確
- 逆操作の導出が機械的で、バグが入りにくい
- 既存のinsertText/deleteTextの戻り値型が変わる破壊的変更
- 将来のUndo履歴管理は、TextChangeのリスト/スタックを管理するだけで実装可能
