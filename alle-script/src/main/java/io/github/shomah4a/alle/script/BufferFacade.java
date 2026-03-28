package io.github.shomah4a.alle.script;

/**
 * スクリプトに公開するバッファ操作のファサード。
 * 内部のBufferFacadeをラップし、スクリプトAPIの安定性を保つ。
 */
public class BufferFacade {

    private final io.github.shomah4a.alle.core.buffer.BufferFacade buffer;

    BufferFacade(io.github.shomah4a.alle.core.buffer.BufferFacade buffer) {
        this.buffer = buffer;
    }

    /**
     * バッファ名を返す。
     */
    public String name() {
        return buffer.getName();
    }

    /**
     * 全テキストを返す。
     */
    public String text() {
        return buffer.getText();
    }

    /**
     * テキスト長をコードポイント数で返す。
     */
    public int length() {
        return buffer.length();
    }

    /**
     * 行数を返す。
     */
    public int lineCount() {
        return buffer.lineCount();
    }

    /**
     * 指定行のテキストを返す。
     */
    public String lineText(int lineIndex) {
        return buffer.lineText(lineIndex);
    }

    /**
     * 指定位置に文字列を挿入する。
     */
    public void insertAt(int index, String text) {
        buffer.insertText(index, text);
    }

    /**
     * 指定位置から指定数のコードポイントを削除する。
     */
    public void deleteAt(int index, int count) {
        buffer.deleteText(index, count);
    }

    /**
     * 指定範囲の部分文字列を返す。
     */
    public String substring(int start, int end) {
        return buffer.substring(start, end);
    }

    /**
     * 指定オフセットが属する行のインデックスを返す。
     */
    public int lineIndexForOffset(int offset) {
        return buffer.lineIndexForOffset(offset);
    }

    /**
     * 指定行の先頭オフセットを返す。
     */
    public int lineStartOffset(int lineIndex) {
        return buffer.lineStartOffset(lineIndex);
    }

    /**
     * 変更済みかどうかを返す。
     */
    public boolean isDirty() {
        return buffer.isDirty();
    }

    /**
     * 読み取り専用かどうかを返す。
     */
    public boolean isReadOnly() {
        return buffer.isReadOnly();
    }

    /**
     * 複数の編集操作をundoの1単位としてまとめて実行する。
     *
     * @param action トランザクション内で実行する操作
     */
    public void withUndoTransaction(Runnable action) {
        buffer.getUndoManager().withTransaction(action);
    }
}
