# ADR 0037: 終了処理ハンドラと save-buffers-kill-alle

## ステータス

承認

## コンテキスト

C-x C-c で終了する際、将来的に未保存バッファの確認などの前処理を挟みたい。
現状の `QuitCommand` は `TerminalInputSource.requestShutdown()` を直接呼ぶだけで拡張性がない。
終了前の処理を優先度付きで登録・実行できる仕組みが必要。

## 決定

### ShutdownRequestable インターフェース

- `requestShutdown()` メソッドのみを持つインターフェースをコアモジュールに定義
- `InputSource` とは責務を分離する（入力読み取りと終了要求は別）
- `TerminalInputSource` が `InputSource` と `ShutdownRequestable` の両方を実装

### ShutdownHandler

- 優先度（int、小さいほど先に実行）とハンドラ（`Supplier<CompletableFuture<Boolean>>`）のペアで登録
- `executeAll()` で優先度順にハンドラを順次実行
- ハンドラが `false` を返したら以降のハンドラを実行せず終了を中断
- 全ハンドラが `true` を返したら `CompletableFuture<Boolean>` として `true` を返す
- 非同期にすることで、将来のミニバッファ入力を伴うハンドラ（未保存確認等）でメインループをブロックしない

### SaveBuffersKillAlleCommand

- `ShutdownHandler.executeAll()` を呼び、全ハンドラ通過後にコールバックで `requestShutdown()` を呼ぶ
- コマンド名: `save-buffers-kill-alle`
- コアモジュールに配置（`ShutdownRequestable` 経由で終了要求するため TUI 依存なし）

### キーバインド

- C-x C-c → `save-buffers-kill-alle`
- C-q → `quit`（即時終了、開発用として残す）

## 影響

- `InputSource` インターフェースには変更なし（テストコードへの影響ゼロ）
- `QuitCommand` は alle-tui に残存（即時終了用）
- 今回はハンドラ未登録で、実質的に即時終了と同じ動作
- 未保存バッファ確認は別タスクでハンドラとして追加予定
