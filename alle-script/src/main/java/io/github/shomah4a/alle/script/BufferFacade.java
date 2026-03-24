package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.buffer.BufferActor;
import java.util.concurrent.CompletableFuture;

/**
 * スクリプトに公開するバッファ操作のファサード。
 * BufferActor経由で操作し、CompletableFutureを返す。
 * Python側ではJavaFutureラッパーでawait可能。
 */
public class BufferFacade {

    private final BufferActor actor;

    BufferFacade(BufferActor actor) {
        this.actor = actor;
    }

    /**
     * バッファ名を返す。
     */
    public CompletableFuture<String> name() {
        return actor.getName();
    }

    /**
     * 全テキストを返す。
     */
    public CompletableFuture<String> text() {
        return actor.getText();
    }

    /**
     * テキスト長をコードポイント数で返す。
     */
    public CompletableFuture<Integer> length() {
        return actor.length();
    }

    /**
     * 行数を返す。
     */
    public CompletableFuture<Integer> lineCount() {
        return actor.lineCount();
    }

    /**
     * 指定行のテキストを返す。
     */
    public CompletableFuture<String> lineText(int lineIndex) {
        return actor.lineText(lineIndex);
    }

    /**
     * 指定位置に文字列を挿入する。
     */
    public CompletableFuture<Void> insertAt(int index, String text) {
        return actor.insertText(index, text).thenApply(change -> null);
    }

    /**
     * 指定位置から指定数のコードポイントを削除する。
     */
    public CompletableFuture<Void> deleteAt(int index, int count) {
        return actor.deleteText(index, count).thenApply(change -> null);
    }

    /**
     * 指定範囲の部分文字列を返す。
     */
    public CompletableFuture<String> substring(int start, int end) {
        return actor.substring(start, end);
    }

    /**
     * 指定オフセットが属する行のインデックスを返す。
     */
    public CompletableFuture<Integer> lineIndexForOffset(int offset) {
        return actor.lineIndexForOffset(offset);
    }

    /**
     * 指定行の先頭オフセットを返す。
     */
    public CompletableFuture<Integer> lineStartOffset(int lineIndex) {
        return actor.lineStartOffset(lineIndex);
    }

    /**
     * 変更済みかどうかを返す。
     */
    public CompletableFuture<Boolean> isDirty() {
        return actor.isDirty();
    }

    /**
     * 読み取り専用かどうかを返す。
     */
    public CompletableFuture<Boolean> isReadOnly() {
        return actor.isReadOnly();
    }
}
