# ADR-0059: スクリプト用 BufferFacade を BufferActor 依存に変更

## ステータス

承認

## コンテキスト

ADR-0058 で Actor を唯一の公開操作 API とする方針を決定した。
しかし alle-script の BufferFacade は依然として Buffer を直接参照し、同期的に値を返している。
WindowFacade は既に WindowActor 依存 + CompletableFuture 返却パターンに移行済みであり、BufferFacade だけが旧来の設計のまま残っている。

## 決定

### BufferManager で BufferActor を 1:1 管理する

- BufferManager.add(Buffer) 時に BufferActor も生成・保持する
- BufferManager.getActor(Buffer) で対応する BufferActor を取得できる
- BufferManager.remove() 時に BufferActor も削除する
- 同一 Buffer に対して常に同一の BufferActor が返されることを保証する

### WindowActor が BufferActor を保持する

- WindowActor のコンストラクタで初期 BufferActor を受け取る
- setBuffer(BufferActor) で BufferActor ごと差し替える（内部で Window.setBuffer も実行）
- getBufferActor() で保持中の BufferActor を返す
- 既存の setBuffer(Buffer) / getBuffer() は段階的に BufferActor 版に置き換える

### BufferFacade (alle-script) を BufferActor 依存に変更

- コンストラクタで BufferActor を受け取る
- 全メソッドが CompletableFuture を返す
- Python 側の buffer.py も JavaFuture を返すように統一する（window.py と同一パターン）

## 帰結

- スクリプト API から Buffer への直接参照が除去され、ADR-0058 の方針に整合する
- Python 側の buffer API は破壊的変更となるが、window API と一貫したパターンになる
- BufferActor の一元管理により、将来のキュー + 排他制御導入時にアクターの一意性が保証される
