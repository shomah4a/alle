package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.list.ListIterable;

/**
 * Bufferのファサード。
 * 書き込み系メソッドの呼び出し時に{@link Buffer#isReadOnly()}をチェックし、
 * 読み取り専用の場合は{@link ReadOnlyBufferException}をスローする。
 * WindowがBufferにアクセスする際の窓口として使用する。
 */
public class BufferFacade implements Buffer {

    private final Buffer buffer;

    /**
     * 指定Bufferをラップするファサードを作成する。
     * 渡されたBufferが既にBufferFacadeの場合は二重ラップを避けるためアンラップする。
     */
    public BufferFacade(Buffer buffer) {
        if (buffer instanceof BufferFacade facade) {
            this.buffer = facade.buffer;
        } else {
            this.buffer = buffer;
        }
    }

    private void checkReadOnly() {
        if (buffer.isReadOnly()) {
            throw new ReadOnlyBufferException(buffer.getName());
        }
    }

    // ── テキスト読み取り（そのまま委譲） ──

    @Override
    public int length() {
        return buffer.length();
    }

    @Override
    public int codePointAt(int index) {
        return buffer.codePointAt(index);
    }

    @Override
    public String substring(int start, int end) {
        return buffer.substring(start, end);
    }

    @Override
    public int lineCount() {
        return buffer.lineCount();
    }

    @Override
    public int lineIndexForOffset(int offset) {
        return buffer.lineIndexForOffset(offset);
    }

    @Override
    public int lineStartOffset(int lineIndex) {
        return buffer.lineStartOffset(lineIndex);
    }

    @Override
    public String lineText(int lineIndex) {
        return buffer.lineText(lineIndex);
    }

    @Override
    public String getText() {
        return buffer.getText();
    }

    // ── テキスト書き込み（readOnlyチェック付き） ──

    @Override
    public TextChange insertText(int index, String text) {
        checkReadOnly();
        return buffer.insertText(index, text);
    }

    @Override
    public TextChange deleteText(int index, int count) {
        checkReadOnly();
        return buffer.deleteText(index, count);
    }

    @Override
    public TextChange apply(TextChange change) {
        checkReadOnly();
        return buffer.apply(change);
    }

    // ── メタデータ ──

    @Override
    public String getName() {
        return buffer.getName();
    }

    @Override
    public Optional<Path> getFilePath() {
        return buffer.getFilePath();
    }

    @Override
    public void setFilePath(Path filePath) {
        checkReadOnly();
        buffer.setFilePath(filePath);
    }

    @Override
    public LineEnding getLineEnding() {
        return buffer.getLineEnding();
    }

    @Override
    public void setLineEnding(LineEnding lineEnding) {
        checkReadOnly();
        buffer.setLineEnding(lineEnding);
    }

    @Override
    public boolean isDirty() {
        return buffer.isDirty();
    }

    @Override
    public void markDirty() {
        checkReadOnly();
        buffer.markDirty();
    }

    @Override
    public void markClean() {
        checkReadOnly();
        buffer.markClean();
    }

    @Override
    public boolean isReadOnly() {
        return buffer.isReadOnly();
    }

    @Override
    public boolean isSystemBuffer() {
        return buffer.isSystemBuffer();
    }

    // ── モード（そのまま委譲） ──

    @Override
    public MajorMode getMajorMode() {
        return buffer.getMajorMode();
    }

    @Override
    public void setMajorMode(MajorMode majorMode) {
        buffer.setMajorMode(majorMode);
    }

    @Override
    public ListIterable<MinorMode> getMinorModes() {
        return buffer.getMinorModes();
    }

    @Override
    public void enableMinorMode(MinorMode mode) {
        buffer.enableMinorMode(mode);
    }

    @Override
    public void disableMinorMode(MinorMode mode) {
        buffer.disableMinorMode(mode);
    }

    // ── キーマップ（そのまま委譲） ──

    @Override
    public Optional<Keymap> getLocalKeymap() {
        return buffer.getLocalKeymap();
    }

    @Override
    public void setLocalKeymap(Keymap keymap) {
        buffer.setLocalKeymap(keymap);
    }

    @Override
    public void clearLocalKeymap() {
        buffer.clearLocalKeymap();
    }

    // ── テキストプロパティ ──

    @Override
    public void putReadOnly(int start, int end) {
        buffer.putReadOnly(start, end);
    }

    @Override
    public void removeReadOnly(int start, int end) {
        buffer.removeReadOnly(start, end);
    }

    @Override
    public boolean isReadOnlyAt(int index) {
        return buffer.isReadOnlyAt(index);
    }

    // ── Undo ──

    @Override
    public UndoManager getUndoManager() {
        return buffer.getUndoManager();
    }

    // ── 同一性 ──

    /**
     * ラップしている素のBufferに委譲する。
     * BufferFacade同士、またはBufferFacadeと素のBufferの比較で
     * 同一のバッファを指していれば等しいと判定する。
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BufferFacade other) {
            return buffer.equals(other.buffer);
        }
        return buffer.equals(obj);
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }
}
