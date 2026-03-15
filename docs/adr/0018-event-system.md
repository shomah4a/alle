# ADR-0018: イベントシステムと非同期コマンド実行基盤

## ステータス

承認

## コンテキスト

エディタにfind-file、save-buffer、ミニバッファ入力などの対話的機能を追加するにあたり、
コマンドが非同期的に結果を返す仕組みが必要になった。

現在のCommand.execute()はvoidを返しており、コマンドの完了を呼び出し側が知る手段がない。
また、ファイルI/Oやミニバッファ入力のような非同期操作を含むコマンドを実装するには、
コマンド実行をVirtual Threadで行い、操作の完了をPromise（CompletableFuture）で
待機できる仕組みが必要である。

将来のスクリプトAPI（GraalVM Polyglot）への公開も見据え、
コンポーネントへの操作をメッセージパッシング形式で統一しておくことが望ましい。

## 決定

### 1. Command.execute()の返り値をCompletableFuture<Void>に変更する

コマンドの完了を呼び出し側が待機・チェーンできるようにする。
同期的なコマンドは `CompletableFuture.completedFuture(null)` を返す。

### 2. Buffer/Windowの操作をラップするアクター層を導入する

BufferActor / WindowActor を導入し、Buffer/Windowへの操作を
CompletableFutureを返すAPIとして提供する。

- 現在のBuffer/Windowの実装には手を入れない
- ラッパー層がキューを持ち、操作を逐次実行する
- 当面の内部実装は同期的（CompletableFuture.completedFuture()で即時返却）
- 将来的にキュー+処理スレッドに差し替え可能

### 3. CommandLoopをVirtual Threadでのコマンド実行に対応させる

コマンドはVirtual Thread上で実行される。
ミニバッファ入力等でCompletableFuture.get()を呼んでも、
Virtual Threadの特性によりキャリアスレッドはブロックされない。

## 却下した選択肢

### AsyncCommand extends Command による段階的導入

既存Commandを変更せず新インターフェースを追加する案。
instanceof判定が増え、APIの一貫性が損なわれるため却下。
個人プロジェクトであり、問題があればrevert可能。

### Twisted的リアクターパターン

デッドロックリスクが高いため却下。

### RxJava / Project Reactor

TUIエディタのユースケースに対して過剰であるため却下。

## 影響

- Command インターフェースの破壊的変更により全コマンド実装の更新が必要
- 既存テストのexecute()呼び出し箇所も更新が必要
- CommandContextにアクター層へのアクセス手段を追加する必要がある
