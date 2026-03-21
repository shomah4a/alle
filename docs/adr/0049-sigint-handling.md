# ADR-0049: C-c (SIGINT) のターミナル制御無効化

## ステータス

採用

## コンテキスト

C-c がターミナルの SIGINT として処理され、エディタプロセスが終了してしまう。
Emacs のように C-c をプレフィックスキーとして使うには、SIGINT を無効化して
C-c を通常のキーストロークとして受け取る必要がある。

過去に `stty intr undef` による対処を試みたが、効かない環境があることが確認されている（コミット 0b100fa で撤回）。

## 決定

Lanterna が提供する `UnixLikeTerminal.CtrlCBehaviour.TRAP` を使用する。

`UnixTerminal` の3引数コンストラクタはデフォルトで `CtrlCBehaviour.CTRL_C_KILLS_APPLICATION` を使用しており、
C-c 受信時にプロセスを終了させる。4引数コンストラクタで `CtrlCBehaviour.TRAP` を指定することで、
C-c を通常のキーストロークとして `readInput()` から返却させる。

### 検討した代替案

1. **`stty intr undef` による無効化**: 効かない環境がある（撤回済み）
2. **JVM `sun.misc.Signal` によるハンドリング**: 内部APIへの依存、Lanterna のシグナルハンドリングとの競合リスク、キーストローク注入のためのポーリング変換が必要になり複雑
3. **`screen.pollInput()` によるポーリング + 注入機構**: CPU 使用率増大、ESC シーケンス解釈への影響リスク

いずれも Lanterna の組み込み機構で対処できるため不採用とした。

## 影響

- `Main.java` の `UnixTerminal` コンストラクタに `CtrlCBehaviour.TRAP` 引数を追加
- `TerminalInputSource` の `screen.readInput()`（ブロッキング）を `screen.pollInput()`（ノンブロッキング）+ 短いスリープに変更。シャットダウンフラグを定期的に検知可能にするため
- 既存の `KeyStrokeConverter` は `Character` タイプ + Ctrl 修飾子の変換に対応済み
- C-c がプレフィックスキーとして機能するようになる（例: C-x C-c による終了）
