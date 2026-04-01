package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.setting.BufferLocalSettings;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.eclipse.collections.api.list.ListIterable;

/**
 * Bufferのファサード。
 * 書き込み系メソッドの呼び出し時に{@link Buffer#isReadOnly()}をチェックし、
 * 読み取り専用の場合は{@link ReadOnlyBufferException}をスローする。
 * WindowがBufferにアクセスする際の窓口として使用する。
 * Bufferインターフェースはパッケージプライベートであり、外部からのバッファアクセスは
 * すべてこのBufferFacade経由で行う。
 */
public class BufferFacade {

    private final Buffer buffer;
    private final Predicate<Path> directoryChecker;

    /**
     * 指定Bufferをラップするファサードを作成する。
     * ディレクトリ判定にはFiles::isDirectoryを使用する。
     */
    public BufferFacade(Buffer buffer) {
        this(buffer, Files::isDirectory);
    }

    /**
     * ディレクトリ判定関数を指定してファサードを作成する。
     */
    public BufferFacade(Buffer buffer, Predicate<Path> directoryChecker) {
        this.buffer = buffer;
        this.directoryChecker = directoryChecker;
    }

    /**
     * パッケージプライベート: 内部のBufferを取得する。
     * buffer パッケージ内でのみ使用可能。
     */
    Buffer unwrap() {
        return buffer;
    }

    private void checkReadOnly() {
        if (buffer.isReadOnly()) {
            throw new ReadOnlyBufferException(buffer.getName());
        }
    }

    // ── アトミック操作 ──

    /**
     * BufferFacade経由での複合操作をアトミックに実行する。
     * 内部のBuffer.atomicOperationでロックを取得し、ハンドラにはこのBufferFacadeを渡す。
     */
    public <T> T atomicOperation(Function<BufferFacade, T> handler) {
        return buffer.atomicOperation(b -> handler.apply(this));
    }

    // ── テキスト読み取り ──

    public int length() {
        return buffer.length();
    }

    public int codePointAt(int index) {
        return buffer.codePointAt(index);
    }

    public String substring(int start, int end) {
        return buffer.substring(start, end);
    }

    public int lineCount() {
        return buffer.lineCount();
    }

    public int lineIndexForOffset(int offset) {
        return buffer.lineIndexForOffset(offset);
    }

    public int lineStartOffset(int lineIndex) {
        return buffer.lineStartOffset(lineIndex);
    }

    public String lineText(int lineIndex) {
        return buffer.lineText(lineIndex);
    }

    public String getText() {
        return buffer.getText();
    }

    // ── テキスト書き込み（readOnlyチェック付き） ──

    public TextChange insertText(int index, String text) {
        checkReadOnly();
        return buffer.insertText(index, text);
    }

    public TextChange deleteText(int index, int count) {
        checkReadOnly();
        return buffer.deleteText(index, count);
    }

    public TextChange apply(TextChange change) {
        checkReadOnly();
        return buffer.apply(change);
    }

    // ── メタデータ ──

    public String getName() {
        return buffer.getName();
    }

    public Optional<Path> getFilePath() {
        return buffer.getFilePath();
    }

    /**
     * バッファのデフォルトディレクトリを返す。
     * ファイルパスが設定されていれば、ディレクトリならそのまま、ファイルなら親ディレクトリを返す。
     * 未設定またはルートパスの場合はfallbackを返す。
     * 状態は持たず、既存のfilePathから導出する。
     */
    public Path getDefaultDirectory(Path fallback) {
        return buffer.getFilePath()
                .map(p -> directoryChecker.test(p) ? p : p.getParent())
                .orElse(fallback);
    }

    public void setFilePath(Path filePath) {
        checkReadOnly();
        buffer.setFilePath(filePath);
    }

    public LineEnding getLineEnding() {
        return buffer.getLineEnding();
    }

    public void setLineEnding(LineEnding lineEnding) {
        checkReadOnly();
        buffer.setLineEnding(lineEnding);
    }

    public boolean isDirty() {
        return buffer.isDirty();
    }

    public void markDirty() {
        checkReadOnly();
        buffer.markDirty();
    }

    public void markClean() {
        checkReadOnly();
        buffer.markClean();
    }

    public boolean isReadOnly() {
        return buffer.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        buffer.setReadOnly(readOnly);
    }

    public boolean isSystemBuffer() {
        return buffer.isSystemBuffer();
    }

    // ── 設定 ──

    public BufferLocalSettings getSettings() {
        return buffer.getSettings();
    }

    // ── モード ──

    public MajorMode getMajorMode() {
        return buffer.getMajorMode();
    }

    public void setMajorMode(MajorMode majorMode) {
        buffer.setMajorMode(majorMode);
    }

    public ListIterable<MinorMode> getMinorModes() {
        return buffer.getMinorModes();
    }

    public void enableMinorMode(MinorMode mode) {
        buffer.enableMinorMode(mode);
    }

    public void disableMinorMode(MinorMode mode) {
        buffer.disableMinorMode(mode);
    }

    // ── キーマップ ──

    public Optional<Keymap> getLocalKeymap() {
        return buffer.getLocalKeymap();
    }

    public void setLocalKeymap(Keymap keymap) {
        buffer.setLocalKeymap(keymap);
    }

    public void clearLocalKeymap() {
        buffer.clearLocalKeymap();
    }

    // ── テキストプロパティ ──

    public void putReadOnly(int start, int end) {
        buffer.putReadOnly(start, end);
    }

    public void removeReadOnly(int start, int end) {
        buffer.removeReadOnly(start, end);
    }

    public boolean isReadOnlyAt(int index) {
        return buffer.isReadOnlyAt(index);
    }

    public void putPointGuard(int start, int end) {
        buffer.putPointGuard(start, end);
    }

    public void removePointGuard(int start, int end) {
        buffer.removePointGuard(start, end);
    }

    public boolean isPointGuardAt(int index) {
        return buffer.isPointGuardAt(index);
    }

    public int resolvePointGuard(int index, boolean forward) {
        return buffer.resolvePointGuard(index, forward);
    }

    public void putFace(int start, int end, FaceName faceName) {
        buffer.putFace(start, end, faceName);
    }

    public void removeFace(int start, int end) {
        buffer.removeFace(start, end);
    }

    public void removeFaceByName(int start, int end, FaceName faceName) {
        buffer.removeFaceByName(start, end, faceName);
    }

    public ListIterable<StyledSpan> getFaceSpans(int start, int end) {
        return buffer.getFaceSpans(start, end);
    }

    // ── Undo ──

    public UndoManager getUndoManager() {
        return buffer.getUndoManager();
    }

    // ── 同一性 ──

    /**
     * ラップしている素のBufferに委譲する。
     * BufferFacade同士の比較で同一のバッファを指していれば等しいと判定する。
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BufferFacade other) {
            return buffer.equals(other.buffer);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }
}
