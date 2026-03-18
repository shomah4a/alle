# ADR 0032: Bufferインターフェース抽出とメッセージバッファ

## ステータス

提案

## コンテキスト

メッセージバッファ（`*Messages*`）の実装にあたり、現在の `Buffer` クラスは GapBuffer ベースの TextModel に密結合しており、
RingBuffer ベースの別ストレージを持つバッファを同一のインターフェースで扱えない。

Emacsにおけるバッファは Unix のファイルのような基底概念であり、
ストレージの実装に関わらずバッファはバッファとして一貫して扱えるべきである。

また、`*Messages*` のような readonly バッファの概念も必要であり、
Window からの編集操作に対して readonly チェックを挟む仕組みが求められる。

## 決定

### Buffer インターフェースの抽出

現在の `Buffer` クラスからインターフェースを抽出する。

```
Buffer (interface)
├── テキスト読み取り: length, lineCount, lineText, lineStartOffset,
│   lineIndexForOffset, getText, codePointAt, substring
├── テキスト書き込み: insertText, deleteText, apply
├── メタデータ: getName, getFilePath, getLineEnding, isDirty,
│   markDirty, markClean
├── モード: getMajorMode, setMajorMode, getMinorModes,
│   enableMinorMode, disableMinorMode
├── キーマップ: getLocalKeymap, setLocalKeymap, clearLocalKeymap
├── Undo: getUndoManager
└── isReadOnly()
```

### EditableBuffer（現 Buffer クラスのリネーム）

- `Buffer` → `EditableBuffer` にリネーム
- `EditableBuffer implements Buffer`
- GapBuffer (TextModel) ベースのストレージ
- `isReadOnly()` → `false`

### BufferFacade

- `Buffer` をラップし、書き込み系メソッドで `isReadOnly()` をチェック
- readonly の場合は `ReadOnlyBufferException` を送出
- Window が保持する型を `BufferFacade` に変更
- Window の `insert()`, `deleteBackward()`, `deleteForward()` が facade 経由になる

### MessageBuffer

- `Buffer` インターフェースを実装
- `RingBuffer<String>` をストレージとして使用
- `isReadOnly()` → `true`
- `message(String)` メソッドで行追加（具象型への直接アクセス）
- 起動時から `*Messages*` として BufferManager に登録

### エコーエリア

- ミニバッファ非アクティブ時に `*Messages*` の最後のメッセージをエコーエリアに表示
- 次のキー入力でメッセージ表示をクリア

## 影響

- `new Buffer(...)` の全箇所（約80箇所）を `new EditableBuffer(...)` に変更
- `import` は変更不要（`Buffer` がインターフェース名になるため）
- Window が `BufferFacade` を保持するよう変更
- `alle-core` に `libs:ring-buffer` 依存追加
- CommandContext に MessageBuffer 参照追加
- ScreenRenderer にエコーエリア描画追加
