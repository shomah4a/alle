# ADR-0058: Actor を唯一の公開操作 API とする設計改善

## ステータス

承認

## コンテキスト

ADR-0007 でバッファ単位のアクターモデルを採用し、BufferActor / WindowActor を導入した。
現状これらは CompletableFuture.completedFuture() でラップしているだけの薄いレイヤーであり、以下の問題がある。

- コマンドが atomicPerform 経由で生の Window / Buffer を直接操作している
- WindowActor.getWindow() / BufferActor.getBuffer() というエスケープハッチが存在し、アクター抽象化を貫通している
- CommandLoop で毎回 new WindowActor() しており、同一 Window に対して複数の Actor が存在し得る
- 外部モジュール（alle-tui, alle-script）から Window / Buffer に直接アクセスしている

将来のマルチスレッド化（ADR-0007 で計画されたキュー + 専用スレッド）を見据え、Actor を唯一の公開操作 API として位置づけ、非同期処理を強制するコルセットとして機能させたい。

また、設計レビューで指摘された以下の問題も同時に対応する。

- CommandLoop の責務過多: キーマップ4階層解決ロジックが CommandLoop に直接記述されている
- ScreenRenderer の責務過多: スナップショット取得ロジックが alle-tui にあり、core の Window / Buffer に直接アクセスしている

## 決定

### Actor を唯一の公開操作 API とする

- WindowActor / BufferActor の全メソッドが CompletableFuture を返す
- Window, Buffer（インターフェース）, BufferFacade, EditableBuffer はすべてパッケージプライベートにする
- atomicPerform, Actor コンストラクタもパッケージプライベートにする
- Actor のドメインメソッド内部で atomicPerform を使い、複合操作のアトミック性を保証する

### Actor インスタンスの管理

- BufferManager が Buffer → BufferActor のマッピングを管理し、同一 Buffer に対して1つの BufferActor を保証する
- Frame が Window → WindowActor のマッピングを管理する
- 外部は BufferManager / Frame 経由でのみ Actor を取得する

### レンダリング用スナップショット

- Frame.createSnapshot() メソッドを追加し、Frame がスナップショット生成の責務を持つ
- Frame は core.window パッケージ内なので Window / Buffer にパッケージプライベートでアクセス可能
- RenderSnapshot はイミュータブルなデータクラスとして公開
- ScreenRenderer（alle-tui）は Frame.createSnapshot() を呼び、描画に集中する

### キーマップ解決

- CommandLoop.resolveKey() の4階層走査ロジック（バッファローカル → マイナーモード → メジャーモード → グローバル）を KeyResolver に移譲する
- KeyResolver は core 内部で Buffer のキーマップ情報に直接アクセスする
- CommandLoop はキー入力 → KeyResolver.resolve() → コマンド実行の制御フローに集中する

### メッセージ出力

- CommandContext に message(String) / warning(String) メソッドを追加し CompletableFuture<Void> を返す
- MessageBuffer を外部に直接公開しない

## 帰結

- コマンド、スクリプト、TUI 層からの Window / Buffer 操作はすべて Actor 経由となり、将来のマルチスレッド化への移行が容易になる
- Actor のメソッド数が増加するが、ドメイン操作として意味のある粒度でメソッドを設計することで API の質を保つ
- レンダリングとキーマップ解決は core 内部に責務を持つため、パッケージプライベートな Window / Buffer に正当にアクセスできる
- テストコードは Actor 経由に書き換える必要があり、CompletableFuture の取り扱いが増える
