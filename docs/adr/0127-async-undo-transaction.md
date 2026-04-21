# ADR 0127: withTransaction の非同期対応とキューイング

## ステータス

採用

## コンテキスト

`CommandLoop.handleEntry` は `UndoManager#withTransaction(Runnable)` でコマンド実行を包み、
複数のバッファ編集を 1 undo 単位にまとめている。
しかし `withTransaction` は `Runnable` を同期的に実行して即座にコミットするため、
`InputPrompter#prompt` を介す非同期コマンド（M-x 経由の実行等）では
プロンプト完了後のコールバック内編集がトランザクション外で record され、
行単位に undo が積まれてしまう。

具体的に `M-x kill-rectangle` / `M-x comment-region` / `M-x indent-region` 等で
1 undo にまとまらない問題が発生する。

`StringRectangleCommand` は自前で `atomicOperation + withTransaction` を張って回避しているが、
本来はフレームワーク側で保証すべきである。

### 過去の試行

- 案 A: CommandLoop で非同期境界跨ぎの transaction 保持 → スレッド安全性の問題
- 案 B: ExecuteCommandCommand.executeByName で begin/commit 分離 → 型による保証がない
- リエントラント化のみ → 異なるコンテキストから同一 UndoManager に対して
  トランザクションが同時に開かれた場合、変更が混入する問題

## 決定

`withTransaction` の���グネチャを `Supplier<CompletableFuture<Void>>` を受け取る形に変更し、
トランザクションの直列実行をキューで保証する。

### API 変更

```java
// 旧
void withTransaction(Runnable action)

// 新
CompletableFuture<Void> withTransaction(Supplier<CompletableFuture<Void>> action)
```

### キューイングによる直列化

- 先行トランザクションが完了するまで次のトランザクションの実行を遅延する
- 各トランザクションは排他的に実行され、変更が混入しない
- 同期コマンドは `CompletableFuture.completedFuture(null)` を返すため、即座にコミットされる

### スレッド安全性

- `transactionBuffer` への読み書きを `synchronized` で保護する
- 非同期トランザクション中に worker スレッドから future が完了するケースに対応

### 呼び出し側への影響

- `CommandLoop.handleEntry`: `command.execute(context)` の future をそのまま返す形に変更
- `ExecuteCommandCommand.executeByName`: `withTransaction` で包む
- `StringRectangleCommand`: 自前トランザクション管理を削除
- `alle-script/BufferFacade`: `withUndoTransaction` のシグネチャ変更

## 帰結

- M-x 経由のコマンドが自動的に 1 undo 単位にまとまる
- 各コマンドが自前でトランザクションを管理する必要がなくなる
- 将来の query-replace セッション全体を 1 undo にまとめる対応にもこの仕組みが利用可能
