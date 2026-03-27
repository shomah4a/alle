package io.github.shomah4a.alle.core.buffer;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * バッファのundo/redo履歴を管理する。
 * テキスト変更の逆操作をスタックに記録し、undo/redoを提供する。
 */
public class UndoManager {

    private final MutableList<TextChange> undoStack;
    private final MutableList<TextChange> redoStack;
    private boolean recording;

    public UndoManager() {
        this.undoStack = Lists.mutable.empty();
        this.redoStack = Lists.mutable.empty();
        this.recording = true;
    }

    /**
     * テキスト変更を記録する。記録抑制中は何もしない。
     * 通常の編集操作ではredoスタックをクリアする。
     */
    public void record(TextChange change) {
        if (!recording) {
            return;
        }
        undoStack.add(change);
        redoStack.clear();
    }

    /**
     * 最新の変更を取り消す。記録された逆操作を返す。
     * undoスタックが空の場合はemptyを返す。
     */
    public Optional<TextChange> undo() {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        var change = undoStack.removeLast();
        redoStack.add(change);
        return Optional.of(change);
    }

    /**
     * 直前のundoをやり直す。逆操作のさらに逆操作を返す。
     * redoスタックが空の場合はemptyを返す。
     */
    public Optional<TextChange> redo() {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        var change = redoStack.removeLast();
        undoStack.add(change);
        return Optional.of(change.inverse());
    }

    /**
     * 記録を抑制する。undo/redo操作の適用中に使用する。
     */
    public void suppressRecording() {
        this.recording = false;
    }

    /**
     * 記録の抑制を解除する。
     */
    public void resumeRecording() {
        this.recording = true;
    }

    /**
     * undoスタックのサイズを返す。
     */
    public int undoSize() {
        return undoStack.size();
    }

    /**
     * redoスタックのサイズを返す。
     */
    public int redoSize() {
        return redoStack.size();
    }
}
