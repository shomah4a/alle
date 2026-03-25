package io.github.shomah4a.alle.core.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * アクター用のコマンドキュー + VirtualThread 逐次処理基盤。
 * 各アクターインスタンスが専用の ActorThread を持ち、
 * submit されたコマンドをキューから取り出して逐次実行する。
 */
public class ActorThread {

    private static final Logger logger = Logger.getLogger(ActorThread.class.getName());

    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final Thread thread;

    private ActorThread(String name) {
        this.thread = Thread.ofVirtual().name(name).start(this::processLoop);
    }

    /**
     * 指定された名前で ActorThread を生成する。
     * VirtualThread が起動し、コマンドの受付を開始する。
     */
    public static ActorThread create(String name) {
        return new ActorThread(name);
    }

    /**
     * 操作をキューに投入し、結果を CompletableFuture で返す。
     * 操作は ActorThread の VirtualThread 上で逐次実行される。
     */
    public <T> CompletableFuture<T> submit(Supplier<T> operation) {
        var future = new CompletableFuture<T>();
        queue.add(() -> {
            try {
                T result = operation.get();
                future.complete(result);
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    /**
     * ActorThread を停止する。
     * 処理中のコマンドが完了した後、スレッドが終了する。
     */
    public void shutdown() {
        thread.interrupt();
    }

    private void processLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                var command = queue.take();
                command.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            drainRemainingCommands();
            logger.log(Level.FINE, "ActorThread {0} が終了しました", thread.getName());
        }
    }

    /**
     * キューに残っているコマンドを実行して完了させる。
     * shutdown後に未処理のfutureが永久に未完了のまま残ることを防ぐ。
     */
    private void drainRemainingCommands() {
        Runnable command;
        while ((command = queue.poll()) != null) {
            command.run();
        }
    }
}
