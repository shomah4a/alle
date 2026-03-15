# ADR-0027: Kill Ring・Undo/Redo・Region選択の導入

## ステータス

提案

## コンテキスト

エディタとしての基本的な編集機能として、以下が不足している:
- テキスト削除時の蓄積と貼り付け（kill ring / yank）
- 操作の取り消しとやり直し（undo / redo）
- 範囲選択（mark / region）

これらはEmacsの基本操作に相当し、相互に依存関係がある。
特にkill-regionはregionの概念が前提であり、kill-ringはkillコマンド群の前提となる。

## 決定

### Mark/Region

- Windowにmark（カーソルとは別の位置マーカー）を追加する
- C-SPACEでmarkを設定し、markとpointの間がregionとなる
- バッファ切替時にmarkはクリアする

### Kill Ring

- KillRingクラスをリングバッファとして実装する（デフォルト60エントリ）
- 削除系コマンド（kill-line, kill-region）は削除テキストをkill-ringに蓄積する
- 連続killではappendToLastにより前回エントリに追記する
- copy-region (M-w) は削除せずにkill-ringにpushする
- yank (C-y) はkill-ringのcurrentを挿入する

### Undo/Redo

- UndoManagerをBuffer単位で持つ
- TextChangeとカーソル位置のペア（UndoEntry）を記録する
- undo操作自体がUndoManagerに記録されないよう、記録抑制機構を設ける
- 通常の編集操作でredoスタックをクリアする
- C-/ でundo、C-? (Ctrl+Shift+/) でredo

### キーバインド

| キー | コマンド |
|------|----------|
| C-SPACE | set-mark |
| C-w | kill-region |
| M-w | copy-region |
| C-y | yank |
| C-/ | undo |
| C-? | redo |

C-kのkill-lineは既存だが、kill-ring蓄積を追加する。

## 帰結

- KillRingはCommandContext経由で各コマンドに提供する
- UndoManagerはBuffer経由でアクセスする（Buffer.getUndoManager()）
- Window.deleteForward/deleteBackwardのAPI変更は行わず、削除前にsubstringでテキストを取得する方針
- 連続killの判定にはCommandContext.lastCommandを使用する
