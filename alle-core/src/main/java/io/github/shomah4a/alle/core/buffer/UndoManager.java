package io.github.shomah4a.alle.core.buffer;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;

/**
 * バッファのundo/redo履歴を管理する。
 * テキスト変更の逆操作をスタックに記録し、undo/redoを提供する。
 */
public class UndoManager {

    private final MutableList<TextChange> undoStack;
    private final MutableList<TextChange> redoStack;
    private boolean recording;
    private @Nullable MutableList<TextChange> transactionBuffer;

    public UndoManager() {
        this.undoStack = Lists.mutable.empty();
        this.redoStack = Lists.mutable.empty();
        this.recording = true;
    }

    /**
     * テキスト変更を記録する。記録抑制中は何もしない。
     * トランザクション中はバッファに溜め、トランザクション外ではundoスタックに直接積む。
     * 通常の編集操作ではredoスタックをクリアする。
     */
    public void record(TextChange change) {
        if (!recording) {
            return;
        }
        if (transactionBuffer != null) {
            transactionBuffer.add(change);
        } else {
            undoStack.add(change);
            redoStack.clear();
        }
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
     * 記録を抑制した状態でactionを実行する。
     * undo/redo操作の適用時など、バッファ編集がundoスタックに積まれるべきでない場面で使用する。
     * action内で例外が発生しても記録状態は必ず復元される。
     */
    public void withoutRecording(Runnable action) {
        boolean wasRecording = this.recording;
        this.recording = false;
        try {
            action.run();
        } finally {
            this.recording = wasRecording;
        }
    }

    /**
     * 複数のバッファ編集を1つのundo単位にまとめるトランザクション内でactionを実行する。
     * action内で発生するrecord()はバッファリングされ、action完了時にCompoundとして
     * 1つのエントリにまとめてundoスタックに積まれる。
     * ネストは禁止（IllegalStateException）。
     * action内で例外が発生した場合、バッファリングされた記録は破棄される。
     *
     * @throws IllegalStateException トランザクションがネストされた場合
     */
    public void withTransaction(Runnable action) {
        if (transactionBuffer != null) {
            throw new IllegalStateException("トランザクションのネストは許可されていない");
        }
        transactionBuffer = Lists.mutable.empty();
        try {
            action.run();
            if (!transactionBuffer.isEmpty()) {
                var compound =
                        new TextChange.Compound(transactionBuffer.toReversed().toImmutable());
                undoStack.add(compound);
                redoStack.clear();
            }
        } finally {
            transactionBuffer = null;
        }
    }

    /**
     * undo/redoスタックをすべてクリアする。
     * バッファの内容がファイルから再読み込みされた場合など、
     * 履歴が無効になった際に使用する。
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
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
