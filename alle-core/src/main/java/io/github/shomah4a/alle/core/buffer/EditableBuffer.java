package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.TextMode;
import io.github.shomah4a.alle.core.textmodel.TextModel;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;

/**
 * GapBuffer (TextModel) ベースの編集可能バッファ。
 * テキストデータ、ファイルパス、変更フラグを管理する。
 * カーソル位置等のビュー固有の状態はWindowが持つ。
 * 全操作はReentrantLockで保護され、スレッドセーフ。
 */
public class EditableBuffer implements Buffer {

    private final String name;
    private final TextModel textModel;
    private final ReentrantLock lock;
    private @Nullable Path filePath;
    private LineEnding lineEnding;
    private @Nullable Keymap localKeymap;
    private MajorMode majorMode;
    private final MutableList<MinorMode> minorModes;
    private final UndoManager undoManager;
    private final TextPropertyStore textPropertyStore;
    private boolean dirty;

    public EditableBuffer(String name, TextModel textModel) {
        this.name = name;
        this.textModel = textModel;
        this.lock = new ReentrantLock();
        this.lineEnding = LineEnding.LF;
        this.majorMode = new TextMode();
        this.minorModes = Lists.mutable.empty();
        this.undoManager = new UndoManager();
        this.textPropertyStore = new TextPropertyStore();
        this.dirty = false;
    }

    public EditableBuffer(String name, TextModel textModel, Path filePath) {
        this(name, textModel);
        this.filePath = filePath;
    }

    private <T> T locked(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private void lockedVoid(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    // ── メタデータ ──

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<Path> getFilePath() {
        return locked(() -> Optional.ofNullable(filePath));
    }

    @Override
    public void setFilePath(Path filePath) {
        lockedVoid(() -> this.filePath = filePath);
    }

    @Override
    public LineEnding getLineEnding() {
        return locked(() -> lineEnding);
    }

    @Override
    public void setLineEnding(LineEnding lineEnding) {
        lockedVoid(() -> this.lineEnding = lineEnding);
    }

    @Override
    public boolean isDirty() {
        return locked(() -> dirty);
    }

    @Override
    public void markDirty() {
        lockedVoid(() -> this.dirty = true);
    }

    @Override
    public void markClean() {
        lockedVoid(() -> this.dirty = false);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    // ── キーマップ ──

    @Override
    public Optional<Keymap> getLocalKeymap() {
        return locked(() -> Optional.ofNullable(localKeymap));
    }

    @Override
    public void setLocalKeymap(Keymap keymap) {
        lockedVoid(() -> this.localKeymap = keymap);
    }

    @Override
    public void clearLocalKeymap() {
        lockedVoid(() -> this.localKeymap = null);
    }

    // ── モード ──

    @Override
    public MajorMode getMajorMode() {
        return locked(() -> majorMode);
    }

    @Override
    public void setMajorMode(MajorMode majorMode) {
        lockedVoid(() -> this.majorMode = majorMode);
    }

    @Override
    public ListIterable<MinorMode> getMinorModes() {
        return locked(() -> minorModes.toImmutable());
    }

    @Override
    public void enableMinorMode(MinorMode mode) {
        lockedVoid(() -> {
            if (!minorModes.contains(mode)) {
                minorModes.add(mode);
            }
        });
    }

    @Override
    public void disableMinorMode(MinorMode mode) {
        lockedVoid(() -> minorModes.remove(mode));
    }

    // ── Undo ──

    @Override
    public UndoManager getUndoManager() {
        return undoManager;
    }

    // ── テキスト読み取り ──

    @Override
    public int length() {
        return locked(textModel::length);
    }

    @Override
    public int codePointAt(int index) {
        return locked(() -> textModel.codePointAt(index));
    }

    @Override
    public String substring(int start, int end) {
        return locked(() -> textModel.substring(start, end));
    }

    @Override
    public int lineCount() {
        return locked(textModel::lineCount);
    }

    @Override
    public int lineIndexForOffset(int offset) {
        return locked(() -> textModel.lineIndexForOffset(offset));
    }

    @Override
    public int lineStartOffset(int lineIndex) {
        return locked(() -> textModel.lineStartOffset(lineIndex));
    }

    @Override
    public String lineText(int lineIndex) {
        return locked(() -> textModel.lineText(lineIndex));
    }

    @Override
    public String getText() {
        return locked(textModel::getText);
    }

    // ── テキスト書き込み ──

    @Override
    public TextChange insertText(int index, String text) {
        return locked(() -> {
            if (textPropertyStore.isReadOnly(index)) {
                throw new ReadOnlyBufferException(name);
            }
            int length = (int) text.codePoints().count();
            textModel.insert(index, text);
            textPropertyStore.adjustForInsert(index, length);
            return new TextChange.Delete(index, text);
        });
    }

    @Override
    public TextChange deleteText(int index, int count) {
        return locked(() -> {
            if (textPropertyStore.hasReadOnly(index, count)) {
                throw new ReadOnlyBufferException(name);
            }
            String deleted = textModel.substring(index, index + count);
            textModel.delete(index, count);
            textPropertyStore.adjustForDelete(index, count);
            return new TextChange.Insert(index, deleted);
        });
    }

    @Override
    public TextChange apply(TextChange change) {
        return locked(() -> switch (change) {
            case TextChange.Insert(var offset, var text) -> insertText(offset, text);
            case TextChange.Delete(var offset, var text) -> {
                int count = (int) text.codePoints().count();
                yield deleteText(offset, count);
            }
        });
    }

    // ── テキストプロパティ ──

    @Override
    public void putReadOnly(int start, int end) {
        lockedVoid(() -> textPropertyStore.putReadOnly(start, end));
    }

    @Override
    public void removeReadOnly(int start, int end) {
        lockedVoid(() -> textPropertyStore.removeReadOnly(start, end));
    }

    @Override
    public boolean isReadOnlyAt(int index) {
        return locked(() -> textPropertyStore.isReadOnly(index));
    }

    // ── アトミック操作 ──

    @Override
    public <T> T atomicOperation(Function<Buffer, T> handler) {
        lock.lock();
        try {
            return handler.apply(this);
        } finally {
            lock.unlock();
        }
    }

    // ── 同一性 ──

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
