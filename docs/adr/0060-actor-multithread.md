# ADR-0060: Actorマルチスレッド化（BufferActor）

## ステータス

承認

## コンテキスト

ADR-0007 でバッファ単位のアクターモデルを採用したが、実装は `CompletableFuture.completedFuture()` による同期的な即時実行のままだった。
ファイルI/O、シンタックスハイライト等の重い処理をBuffer側で非同期実行するための基盤が必要になった。

WindowActor/FrameActorはカーソル移動やキーマップ解決など軽量な処理が主であり、
別スレッド化の必要性は現時点では低い。
また、WindowActorはWindow経由でBufferに直接アクセスしているため、
BufferActorと別スレッドにするにはBuffer直接操作の解消が前提となる。

## 決定

### Phase 1: BufferActorのみスレッド化

- `ActorThread` クラスを導入し、コマンドキュー + VirtualThread による逐次処理基盤を提供する
- BufferActorのみ ActorThread を使用し、`atomicPerform` をキュー経由に変更する
- WindowActor/FrameActorは現状維持（同期的即時実行）
- VirtualThreadのキャリアスレッド数は `jdk.virtualThreadScheduler.parallelism` で制御（初期値: 2）

### Phase 2: WindowActorのBuffer直接操作をBufferActor経由に変更

- WindowActorの各メソッド（insert, deleteBackward, killLine, undo等）でWindow経由のBuffer直接操作を解消する
- BufferActor経由の非同期チェーンに書き換える
- これにより将来的にWindowActorを別スレッドにすることが可能になる

### ActorThread の設計

```
呼び出し元スレッド
  ↓ submit(operation) → CompletableFuture<T>
ActorThread (VirtualThread)
  └→ LinkedBlockingQueue<Command> → 逐次実行
```

- 各BufferActorインスタンスが専用のActorThreadを持つ
- 操作は `Supplier<T>` としてキューに投入され、VirtualThread上で逐次実行される
- 結果は `CompletableFuture<T>` で返却される

## 帰結

- BufferのテキストモデルはBufferActorのVirtualThread上でのみアクセスされ、スレッドセーフ
- WindowActor/FrameActorは引き続きロジックスレッド上で同期実行される
- WindowActorのBuffer直接操作は、ロジックスレッド上で実行されるため、Phase 2完了前はBufferActorスレッドとの競合リスクがある
  - Phase 1ではWindowActorのBuffer操作とBufferActorの操作が同時に実行されないことを呼び出し側で保証する必要がある
  - Phase 2でこの制約を解消する

## 備考

- ADR-0007 の具体実装にあたる
- WindowActor/FrameActorのスレッド化は現時点では不要と判断（将来的に必要になれば別ADRで検討）
