# ADR-0058: Actor層廃止とReentrantLockによるスレッドセーフ化

## ステータス

承認・実装済み

## コンテキスト

ADR-0007 で「バッファ単位のアクターモデル」を採用し、BufferActor/WindowActorを導入した。
これらはBuffer/Windowへの操作をCompletableFutureでラップするが、
内部は同期的に即時実行しており、実質的にはCompletableFuture.completedFuture()で返すだけの薄いラッパーになっている。

アクターモデルのメリット（バッファごとの専用スレッド、メッセージキュー）は現時点で不要であり、
過剰な抽象化となっている。一方で、graalpyなどで別スレッドからBuffer/Windowにアクセスするケースが想定されるため、
スレッドセーフ性は確保したい。

## 決定

### Actor層の廃止

- BufferActor, WindowActorを削除する
- CommandContextはWindowActorの代わりにWindowを直接保持する（activeWindow）
- CommandLoop.handleEntry()でのWindowActor生成を除去する
- 全コマンドはactiveWindow()経由でWindowを直接操作する（CompletableFuture不要）

### BufferFacadeの独立化

- BufferFacade implements Buffer を除去し、独立したクラスとする
- BufferインターフェースはBufferFacade経由で委譲される内部APIとしてpackage privateにする
- 外部からのバッファアクセスはすべてBufferFacade経由とする
- Window.getBuffer()の戻り値をBufferFacadeに変更する
- BufferManagerの管理対象をBufferFacadeに変更する

### スレッドセーフ化

- EditableBufferにReentrantLockを追加し、全操作をprivateなlocked/lockedVoidヘルパーでロック保護する
- 読み取り操作もロック対象とする（GapBufferの内部状態不整合を防ぐため）
- MessageBufferにもReentrantLockを追加し、message()等の全操作をロック保護する
- Buffer.atomicOperation(Function<Buffer, T>)を抽象メソッドとして定義し、EditableBuffer/MessageBufferで実装する
- BufferFacade.atomicOperation(Function<BufferFacade, T>)を独自メソッドとして提供する
- Windowにはロックを持たせない（Buffer側のatomicOperationで複合操作のアトミック性を確保する）
- ReentrantLockのためlocked()内からatomicOperation()を呼んでもデッドロックしない

### スクリプトファサード層

- WindowFacadeをWindowActor経由からWindow直接操作に変更し、同期APIに変更する
- futures.py、internal/futures.pyを削除する（CompletableFutureラッパー不要）
- Python側window.pyのメソッドを同期呼び出しに変更する

### 維持するもの

- Command.execute()のCompletableFuture<Void>戻り値（InputPrompterの非同期性に必要）
- InputPrompterのCompletableFuture<PromptResult>戻り値
- ShutdownHandlerのCompletableFutureチェーン
- 3スレッド構成（入力/ロジック/描画、ADR-0039）

## 帰結

- Actor層の除去によりコードが簡素化される
- ReentrantLockにより別スレッドからのBuffer操作がスレッドセーフになる
- atomicOperationにより複合操作のアトミック性を確保できる
- Bufferインターフェースのpackage private化により、外部からBufferFacade経由のアクセスに統一される
- Command層のCompletableFutureはInputPrompterとの連携に引き続き有用
- スクリプトAPIの同期化により、Python側のコードが簡潔になる
