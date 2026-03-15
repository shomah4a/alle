package io.github.shomah4a.alle.core.buffer;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * バッファのundo/redo履歴を管理する。
 * テキスト変更とカーソル位置をペアで記録し、逆操作を提供する。
 */
public class UndoManager {

    private final MutableList<UndoEntry> undoStack;
    private final MutableList<UndoEntry> redoStack;
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
    public void record(TextChange change, int cursorPosition) {
        if (!recording) {
            return;
        }
        undoStack.add(new UndoEntry(change, cursorPosition));
        redoStack.clear();
    }

    /**
     * 最新の変更を取り消す。記録された逆操作とカーソル復元位置を返す。
     * Windowが記録するのは既に逆操作（insertText→Delete, deleteText→Insert）なので、
     * そのままBuffer.applyに渡すことで元に戻る。
     * undoスタックが空の場合はemptyを返す。
     */
    public Optional<UndoEntry> undo() {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        var entry = undoStack.removeLast();
        redoStack.add(entry);
        return Optional.of(entry);
    }

    /**
     * 直前のundoをやり直す。記録された逆操作のさらに逆操作を返す。
     * undo時に記録されたDelete→inverse→Insertで再挿入、
     * Insert→inverse→Deleteで再削除する。
     * redoスタックが空の場合はemptyを返す。
     */
    public Optional<UndoEntry> redo() {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        var entry = redoStack.removeLast();
        undoStack.add(entry);
        var redoChange = entry.change().inverse();
        return Optional.of(new UndoEntry(redoChange, entry.cursorPosition()));
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
