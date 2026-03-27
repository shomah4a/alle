# ADR-0067: Undoトランザクションと自動記録

## ステータス

承認済み

## コンテキスト

comment-regionなどの複数バッファ編集を伴うコマンドを実行した後、undoがコマンド単位で行えない。
現状は1編集=1 UndoEntryで記録されるため、3行コメント化すると3回undoが必要になる。

また、UndoManagerへの記録（record()呼び出し）が各コマンドの責務になっており、
CommentRegionCommand等ではrecord()を呼んでおらず、そもそもundoが効かない。

## 決定

### 1. undo記録の自動化

TextBuffer.insertText/deleteText内でUndoManager.record()を自動的に呼び出す。
呼び出し側が個別にrecord()する必要をなくし、記録漏れを構造的に防ぐ。

undo/redo実行時やBufferIO.load時など記録を抑制したい場合は
withoutRecording(Runnable)を使用する。
既存のsuppressRecording/resumeRecordingは外部公開せず、
withoutRecordingの内部実装としてのみ使用する。

```java
public void withoutRecording(Runnable action)
```

### 2. UndoEntryからcursorPositionを削除

UndoEntryはTextChangeのみを保持する。
undo/redo後のカーソル位置はTextChangeのoffsetから算出する。
- Insert: offset位置へ移動
- Delete: offset位置へ移動
- Compound: 先頭のchangeのoffsetへ移動

### 3. TextChange.Compoundの追加

TextChange sealed interfaceにCompoundバリアントを追加する。

```java
record Compound(ListIterable<TextChange> changes) implements TextChange {}
```

- inverse(): 各changeのinverseを逆順に並べたCompoundを返す
- Buffer.apply(): changesを順番に適用する

### 4. UndoManager.withTransaction

複数編集を1つのundo単位にまとめるためのAPI。

```java
public void withTransaction(Runnable action)
```

- トランザクション中、record()はバッファリストに溜める
- action完了時にCompoundとして1つのエントリにまとめてundoStackに積む
- ネスト禁止（ネスト時はIllegalStateException）
- 例外安全性はtry-finallyで保証

呼び出し側はトランザクションを意識せず、通常通りbuffer.insertText/deleteTextを呼ぶだけでよい。

### 5. 既存コードへの影響

- Window.insert/deleteBackward/deleteForward: record()呼び出しを削除
- KillRegionCommand: record()呼び出しを削除
- CommentRegionCommand: withTransactionで囲む
- UndoCommand/RedoCommand: withoutRecordingで囲む。カーソル位置算出ロジックをTextChangeベースに変更
- BufferIO.load: withoutRecordingで囲む

## 帰結

- record()呼び忘れによるundo不可バグが構造的に排除される
- 複数編集コマンドはwithTransactionで囲むだけでコマンド単位のundoが可能になる
- UndoEntryの簡素化により、カーソル位置の管理がundo/redo側に集約される
- TextChangeにCompoundが加わり、switch式のパターンが3つに増える（sealed interfaceの網羅性チェックでコンパイル時に検出可能）
