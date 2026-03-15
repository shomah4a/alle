# Kill Ring・Undo/Redo・Region選択 実装計画

## フェーズ1: Mark/Region（C-SPACE範囲選択）

### 1-1. Window にmark追加
- `@Nullable Integer mark` フィールド
- `setMark(int)` / `clearMark()` / `getMark()` → `Optional<Integer>`
- `getRegionStart()` / `getRegionEnd()`: markとpointの小さい方/大きい方
- `setBuffer()` でmarkをクリア
- テスト追加

### 1-2. SetMarkCommand
- C-SPACEでpointの位置にmarkを設定
- コマンド名: `set-mark`
- C-SPACEはターミナル上で0x00（NUL）として送信される
- KeyStrokeConverter に対応追加が必要か確認
- キーバインド登録
- テスト追加

### コミット: Mark/Region基盤 + SetMarkCommand

## フェーズ2: Kill Ring

### 2-1. KillRing基盤
- リングバッファ（MutableList + インデックス管理）
- `push(String)`: 新エントリ追加
- `appendToLast(String)`: 最新エントリに追記（連続kill用）
- `current()`: `Optional<String>` 最新エントリ
- デフォルトサイズ60
- テスト追加

### コミット: KillRing基盤

### 2-2. CommandContextにKillRing追加
- CommandContext recordにkillRingフィールド追加
- CommandLoop.handleEntry()でKillRingを渡す
- TestCommandContextFactory修正
- 既存テストのコンパイル修正

### 2-3. KillLineCommand修正
- 削除前にsubstringでテキスト取得
- kill-ringにpush（連続killではappendToLast）
- 連続kill判定: lastCommand が "kill-line" の場合
- 既存テスト維持 + kill-ring蓄積テスト追加

### コミット: KillLineCommand kill-ring蓄積

### 2-4. KillRegionCommand (C-w)
- mark〜point間のテキストを削除
- 削除テキストをkill-ringにpush
- markをクリア
- markが未設定の場合は何もしない
- テスト追加

### 2-5. CopyRegionCommand (M-w)
- mark〜point間のテキストをkill-ringにpush（削除しない）
- markをクリア
- markが未設定の場合は何もしない
- テスト追加

### コミット: KillRegionCommand + CopyRegionCommand

### 2-6. YankCommand (C-y)
- kill-ringのcurrentをpointに挿入
- kill-ringが空の場合は何もしない
- テスト追加

### コミット: YankCommand

## フェーズ3: Undo/Redo

### 3-1. UndoManager基盤
- `UndoEntry(TextChange change, int cursorPosition)` record
- undoStack / redoStack（MutableList）
- `record(TextChange, int cursorPosition)`: 変更記録、redoスタッククリア
- `undo()`: `Optional<UndoEntry>` 逆操作を返す
- `redo()`: `Optional<UndoEntry>` 再実行操作を返す
- `suppressRecording()` / `resumeRecording()`: 記録抑制
- テスト追加

### 3-2. Bufferへの組み込み
- BufferにUndoManagerフィールド追加
- insertText/deleteTextでUndoManagerに記録
- `getUndoManager()` メソッド追加
- 既存テストへの影響なし（UndoManagerは内部で自動記録）

### コミット: UndoManager基盤 + Buffer組み込み

### 3-3. UndoCommand (C-/) + RedoCommand (C-?)
- UndoCommand: buffer.getUndoManager().undo() → buffer.apply() （記録抑制下で実行）
- RedoCommand: buffer.getUndoManager().redo() → buffer.apply() （記録抑制下で実行）
- カーソル位置もUndoEntryから復元
- キーバインド登録
- C-/ のKeyStroke表現確認（ターミナル上では0x1F）
- C-? のKeyStroke表現確認
- テスト追加

### コミット: UndoCommand + RedoCommand

## リスク対策

1. **Undo再記録問題**: UndoManager.suppressRecording()/resumeRecording()で制御
2. **削除テキスト取得**: deleteForward前にsubstringで取得（API変更回避）
3. **C-SPACEキー表現**: Lanternaでの検証が必要（0x00 or KeyType固有値）
4. **バッファ切替時のmark**: setBuffer()内でclearMark()
