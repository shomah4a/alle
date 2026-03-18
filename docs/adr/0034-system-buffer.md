# ADR 0034: システムバッファ（reserved buffer）

## ステータス

承認

## コンテキスト

`*Messages*` や `*Warnings*` はエディタが内部的に管理するバッファであり、
CommandLoop や ScreenRenderer が参照を保持している。
ユーザーが `kill-buffer` でこれらを削除すると、BufferManager から消えた状態で
`message()` が呼ばれ、switch-to-buffer で辿り着けなくなる問題がある。

## 決定

### isSystemBuffer() の導入

- `Buffer` インターフェースに `isSystemBuffer()` メソッドを追加する
- デフォルトは `false`（default method）
- `MessageBuffer` は `true` を返す
- `BufferFacade` はラップ先に委譲する

### KillBufferCommand の変更

- `isSystemBuffer()` が `true` のバッファは削除を拒否する
- エコーエリアに通知メッセージを表示する
- ウィンドウの切り替えも行わない（バッファは生存し続ける）

## 影響

- Buffer インターフェースにメソッド追加（default method のためコンパイル破壊なし）
- KillBufferCommand に早期 return ガード追加
- 既存の `*scratch*` 再作成ロジックは残す（EditableBuffer なので isSystemBuffer=false）
