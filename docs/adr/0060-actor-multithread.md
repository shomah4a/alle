# ADR-0060: Actorマルチスレッド化（BufferActor）

## ステータス

実装済み

## コンテキスト

ADR-0007 でバッファ単位のアクターモデルを採用したが、実装は `CompletableFuture.completedFuture()` による同期的な即時実行のままだった。
ファイルI/O、シンタックスハイライト等の重い処理をBuffer側で非同期実行するための基盤が必要になった。

WindowActor/FrameActorはカーソル移動やキーマップ解決など軽量な処理が主であり、
別スレッド化の必要性は現時点では低い。

また、ADR-0039 のロジックスレッドはコマンド処理とスナップショット作成が密結合していたが、
BufferActorの非同期化により、コマンド処理後にスナップショットを同期的に作成する設計が成立しなくなった。
コマンド処理と描画を独立させる必要がある。

## 決定

### Phase 1: BufferActorのみスレッド化

- `ActorThread` クラスを導入し、コマンドキュー + VirtualThread による逐次処理基盤を提供する
- BufferActorのみ ActorThread を使用し、`atomicPerform` をキュー経由に変更する
- WindowActor/FrameActorは現状維持（同期的即時実行）
- VirtualThreadのキャリアスレッド数は `jdk.virtualThreadScheduler.parallelism` で制御（初期値: 2）

### Phase 2: WindowActorのBuffer直接操作をBufferActor経由に変更

- WindowActorの各メソッド（insert, deleteBackward, killLine, undo等）でWindow経由のBuffer直接操作を解消する
- `bufferActor.atomicPerform` 内でWindow操作とBuffer操作をまとめて実行する
- これによりBuffer操作がすべてBufferActorのVirtualThread上で逐次実行される

### Phase 3: コマンド処理と描画の分離（4スレッド構成）

ADR-0039 の3スレッド構成（入力・ロジック・描画）を4スレッドに拡張する。

- **入力スレッド（メインスレッド）**: キー入力の読み取りのみ
- **ロジックスレッド**: コマンド処理のみ。描画には関与しない
- **スナップショットスレッド（新規）**: 状態変更トリガーでスナップショットを作成
- **描画スレッド**: スナップショットの画面描画

スナップショットスレッドは以下のトリガーで動作する:
- BufferActorの操作完了（ActorThread.onCompleteコールバック経由）
- processKey処理完了（EditorThreadのrefreshCallback経由）

`CommandLoop.processKey` は `CompletableFuture<Void>` を返し、
コマンドの非同期実行結果を呼び出し側が追跡可能にする。

### ActorThread の設計

```
呼び出し元スレッド
  ↓ submit(operation) → CompletableFuture<T>
ActorThread (VirtualThread)
  └→ LinkedBlockingQueue<Command> → 逐次実行 → onCompleteコールバック
```

- 各BufferActorインスタンスが専用のActorThreadを持つ
- 操作は `Supplier<T>` としてキューに投入され、VirtualThread上で逐次実行される
- 結果は `CompletableFuture<T>` で返却される
- 操作完了時にonCompleteコールバックが呼ばれ、スナップショット更新をトリガーする

## 帰結

- BufferのテキストモデルはBufferActorのVirtualThread上でのみアクセスされ、スレッドセーフ
- WindowActorのBuffer操作は `bufferActor.atomicPerform` 内で実行されるため、BufferActorスレッド上で逐次処理される
- FrameActorは引き続き同期実行（completedFuture）
- コマンド処理と描画が完全に分離され、非同期コマンド（プロンプト等）でもデッドロックしない
- スナップショット作成はBufferActor操作完了をトリガーとするため、最新状態が反映される

## 備考

- ADR-0007 の具体実装にあたる
- ADR-0039 の3スレッド構成を4スレッドに拡張した
- WindowActor/FrameActorのスレッド化は現時点では不要と判断（将来的に必要になれば別ADRで検討）
