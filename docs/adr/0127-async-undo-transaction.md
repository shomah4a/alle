# ADR 0127: withTransaction の非同期対応と TransactionalCommand

## ステータス

採用

## コンテキスト

`CommandLoop.handleEntry` は `UndoManager#withTransaction(Runnable)` でコマンド実行を包み、
複数のバッファ編集を 1 undo 単位にまとめていた。
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
- CommandLoop の withTransaction を future 完了まで保持 → コマンドの execute が
  CompletableFuture を返す以上、コンテキストが切り替わっているにも関わらず
  トランザクションが継続する不整合。また executeByName 内で withTransaction を
  貼ると CommandLoop のトランザクションとキューイングでデッドロック

### 設計上の気づき

CommandLoop でトランザクションを貼ること自体が意味をなさない。
コマンドの `execute` が `CompletableFuture<Void>` を返す設計で、
CommandLoop の `withTransaction(Runnable)` が包めるのは同期的に完了する部分だけ。
同期的に完了するコマンド（self-insert 等）は単一編集なのでトランザクション不要。
複数編集を行う同期コマンドは「たまたま同期で完了するから動いている」だけで、
設計上の保証ではない。

## 決定

2つの変更を行う。

### 1. `withTransaction` の非同期対応

`withTransaction` のシグネチャを `Supplier<CompletableFuture<Void>>` を受け取る形に変更し、
トランザクションの直列実行をキューで保証する。

```java
// 旧
void withTransaction(Runnable action)

// 新
CompletableFuture<Void> withTransaction(Supplier<CompletableFuture<Void>> action)
```

#### キューイングによる直列化

- 先行トランザクションが完了するまで次のトランザクションの実行を遅延する
- 各トランザクションは排他的に実行され、変更が混入しない
- 同期コマンドは `CompletableFuture.completedFuture(null)` を返すため、即座にコミットされる

#### スレッド安全性

- `transactionBuffer` への読み書きを `synchronized` で保護する
- 非同期トランザクション中に worker スレッドから future が完了するケースに対応

### 2. `TransactionalCommand` の導入

トランザクションの責務を CommandLoop からコマンド自身に移す。

```java
public interface TransactionalCommand extends Command {
    CompletableFuture<Void> executeInTransaction(CommandContext context);

    @Override
    default CompletableFuture<Void> execute(CommandContext context) {
        var buffer = context.activeWindow().getBuffer();
        return buffer.getUndoManager().withTransaction(() -> executeInTransaction(context));
    }
}
```

- 複数バッファ編集を行うコマンドは `TransactionalCommand` を実装する
- CommandLoop はトランザクションを一切管理しない。`command.execute(context)` を呼ぶだけ
- M-x 経由でも直接キーバインドでも、コマンド自身がトランザクションを管理するため
  呼び出し経路に依存しない

### 対象コマンド

以下のコマンドを `TransactionalCommand` に変更:

- 矩形系: KillRectangle, DeleteRectangle, OpenRectangle, ClearRectangle, YankRectangle, StringRectangle
- コメント系: CommentRegion, UncommentRegion, CommentOrUncommentRegion, CommentDwim
- インデント系: IndentRegion, DedentRegion

## 帰結

- M-x 経由のコマンドが自動的に 1 undo 単位にまとまる
- 各コマンドが `TransactionalCommand` を実装するだけでトランザクション管理が完了する
- 新規コマンド作成時の書き忘れリスクはあるが、型として明示されるため発見しやすい
- 将来の query-replace セッション全体を 1 undo にまとめる対応にもこの仕組みが利用可能
