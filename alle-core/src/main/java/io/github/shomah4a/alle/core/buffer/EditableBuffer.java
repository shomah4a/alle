package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.TextMode;
import io.github.shomah4a.alle.core.textmodel.TextModel;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;

/**
 * GapBuffer (TextModel) ベースの編集可能バッファ。
 * テキストデータ、ファイルパス、変更フラグを管理する。
 * カーソル位置等のビュー固有の状態はWindowが持つ。
 */
public class EditableBuffer implements Buffer {

    private final String name;
    private final TextModel textModel;
    private @Nullable Path filePath;
    private LineEnding lineEnding;
    private @Nullable Keymap localKeymap;
    private MajorMode majorMode;
    private final MutableList<MinorMode> minorModes;
    private final UndoManager undoManager;
    private boolean dirty;

    public EditableBuffer(String name, TextModel textModel) {
        this.name = name;
        this.textModel = textModel;
        this.lineEnding = LineEnding.LF;
        this.majorMode = new TextMode();
        this.minorModes = Lists.mutable.empty();
        this.undoManager = new UndoManager();
        this.dirty = false;
    }

    public EditableBuffer(String name, TextModel textModel, Path filePath) {
        this(name, textModel);
        this.filePath = filePath;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<Path> getFilePath() {
        return Optional.ofNullable(filePath);
    }

    @Override
    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public LineEnding getLineEnding() {
        return lineEnding;
    }

    @Override
    public void setLineEnding(LineEnding lineEnding) {
        this.lineEnding = lineEnding;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public void markClean() {
        this.dirty = false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Optional<Keymap> getLocalKeymap() {
        return Optional.ofNullable(localKeymap);
    }

    @Override
    public void setLocalKeymap(Keymap keymap) {
        this.localKeymap = keymap;
    }

    @Override
    public void clearLocalKeymap() {
        this.localKeymap = null;
    }

    @Override
    public MajorMode getMajorMode() {
        return majorMode;
    }

    @Override
    public void setMajorMode(MajorMode majorMode) {
        this.majorMode = majorMode;
    }

    @Override
    public ListIterable<MinorMode> getMinorModes() {
        return minorModes;
    }

    @Override
    public void enableMinorMode(MinorMode mode) {
        if (!minorModes.contains(mode)) {
            minorModes.add(mode);
        }
    }

    @Override
    public void disableMinorMode(MinorMode mode) {
        minorModes.remove(mode);
    }

    @Override
    public UndoManager getUndoManager() {
        return undoManager;
    }

    @Override
    public int length() {
        return textModel.length();
    }

    @Override
    public int codePointAt(int index) {
        return textModel.codePointAt(index);
    }

    @Override
    public TextChange insertText(int index, String text) {
        textModel.insert(index, text);
        return new TextChange.Delete(index, text);
    }

    @Override
    public TextChange deleteText(int index, int count) {
        String deleted = textModel.substring(index, index + count);
        textModel.delete(index, count);
        return new TextChange.Insert(index, deleted);
    }

    @Override
    public TextChange apply(TextChange change) {
        return switch (change) {
            case TextChange.Insert(var offset, var text) -> insertText(offset, text);
            case TextChange.Delete(var offset, var text) -> {
                int count = (int) text.codePoints().count();
                yield deleteText(offset, count);
            }
        };
    }

    @Override
    public String substring(int start, int end) {
        return textModel.substring(start, end);
    }

    @Override
    public int lineCount() {
        return textModel.lineCount();
    }

    @Override
    public int lineIndexForOffset(int offset) {
        return textModel.lineIndexForOffset(offset);
    }

    @Override
    public int lineStartOffset(int lineIndex) {
        return textModel.lineStartOffset(lineIndex);
    }

    @Override
    public String lineText(int lineIndex) {
        return textModel.lineText(lineIndex);
    }

    @Override
    public String getText() {
        return textModel.getText();
    }

    /**
     * BufferFacadeとの比較時は順序を入れ替えてBufferFacade.equalsに委譲し、対称性を保つ。
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BufferFacade) {
            return obj.equals(this);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
