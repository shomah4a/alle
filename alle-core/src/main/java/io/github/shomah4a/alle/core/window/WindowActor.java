package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferActor;
import io.github.shomah4a.alle.core.buffer.TextChange;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Windowへの操作をCompletableFutureで返すアクター層。
 * 内部にキューを持ち、操作を逐次実行する構造を提供する。
 * 現時点では同期的に即時実行し、将来的にキュー+処理スレッドに差し替え可能。
 */
public class WindowActor {

    private final Window window;
    private BufferActor bufferActor;

    public WindowActor(Window window, BufferActor bufferActor) {
        this.window = window;
        this.bufferActor = bufferActor;
    }

    /**
     * BufferActor未指定のコンストラクタ。
     * 既存コードとの互換用。BufferActorはWindowのバッファから生成する。
     */
    public WindowActor(Window window) {
        this(window, new BufferActor(window.getBuffer()));
    }

    /**
     * 複数の操作をアトミックに実行する。
     * 将来的にはキュー経由で1つのメッセージとして逐次実行される。
     */
    public <T> CompletableFuture<T> atomicPerform(Function<Window, T> operation) {
        try {
            T result = operation.apply(window);
            return CompletableFuture.completedFuture(result);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    public CompletableFuture<Buffer> getBuffer() {
        return atomicPerform(Window::getBuffer);
    }

    /**
     * 保持中のBufferActorを返す。
     */
    public BufferActor getBufferActor() {
        return bufferActor;
    }

    public CompletableFuture<Void> setBuffer(Buffer buffer) {
        return atomicPerform(w -> {
            w.setBuffer(buffer);
            return null;
        });
    }

    /**
     * BufferActorごとバッファを差し替える。
     * 内部のWindowのバッファも同時に更新する。
     */
    public CompletableFuture<Void> setBuffer(BufferActor actor) {
        return atomicPerform(w -> {
            this.bufferActor = actor;
            w.setBuffer(actor.getBuffer());
            return null;
        });
    }

    public CompletableFuture<Integer> getPoint() {
        return atomicPerform(Window::getPoint);
    }

    public CompletableFuture<Void> setPoint(int point) {
        return atomicPerform(w -> {
            w.setPoint(point);
            return null;
        });
    }

    public CompletableFuture<Integer> getDisplayStartLine() {
        return atomicPerform(Window::getDisplayStartLine);
    }

    public CompletableFuture<Void> setDisplayStartLine(int line) {
        return atomicPerform(w -> {
            w.setDisplayStartLine(line);
            return null;
        });
    }

    public CompletableFuture<Void> insert(String text) {
        return atomicPerform(w -> {
            w.insert(text);
            return null;
        });
    }

    public CompletableFuture<Void> deleteBackward(int count) {
        return atomicPerform(w -> {
            w.deleteBackward(count);
            return null;
        });
    }

    public CompletableFuture<Void> deleteForward(int count) {
        return atomicPerform(w -> {
            w.deleteForward(count);
            return null;
        });
    }

    /**
     * BufferActorフィールドを直接更新する。
     * FrameActor内部でFrame.replaceBufferInAllWindows後の同期に使用する。
     * atomicPerformの外から呼ばないこと。
     */
    void updateBufferActor(BufferActor actor) {
        this.bufferActor = actor;
    }

    /**
     * ラップしているWindowを直接取得する。
     * レンダリング等の同期的なアクセスが必要な場合に使用する。
     */
    public Window getWindow() {
        return window;
    }

    // ── バッファ切り替え ──

    /**
     * 直前に表示していたバッファの名前を返す。
     */
    public CompletableFuture<Optional<String>> getPreviousBufferName() {
        return atomicPerform(w -> w.getPreviousBuffer().map(b -> b.getName()));
    }

    /**
     * バッファ名を返す。
     */
    public CompletableFuture<String> getBufferName() {
        return atomicPerform(w -> w.getBuffer().getName());
    }

    // ── カーソル移動 ──

    /**
     * カーソルを1文字前方に移動する。バッファ末尾では何もしない。
     */
    public CompletableFuture<Void> moveForward() {
        return atomicPerform(w -> {
            int point = w.getPoint();
            if (point < w.getBuffer().length()) {
                w.setPoint(point + 1);
            }
            return null;
        });
    }

    /**
     * カーソルを1文字後方に移動する。バッファ先頭では何もしない。
     */
    public CompletableFuture<Void> moveBackward() {
        return atomicPerform(w -> {
            int point = w.getPoint();
            if (point > 0) {
                w.setPoint(point - 1);
            }
            return null;
        });
    }

    /**
     * カーソルを次の行に移動する。最終行では何もしない。
     * 移動先の行が現在のカラム位置より短い場合は行末に移動する。
     */
    public CompletableFuture<Void> moveToNextLine() {
        return atomicPerform(w -> {
            var buffer = w.getBuffer();
            int point = w.getPoint();
            int currentLine = buffer.lineIndexForOffset(point);
            if (currentLine >= buffer.lineCount() - 1) {
                return null;
            }
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int column = point - currentLineStart;
            int nextLineStart = buffer.lineStartOffset(currentLine + 1);
            int nextLineLength =
                    (int) buffer.lineText(currentLine + 1).codePoints().count();
            w.setPoint(nextLineStart + Math.min(column, nextLineLength));
            return null;
        });
    }

    /**
     * カーソルを前の行に移動する。先頭行では何もしない。
     * 移動先の行が現在のカラム位置より短い場合は行末に移動する。
     */
    public CompletableFuture<Void> moveToPreviousLine() {
        return atomicPerform(w -> {
            var buffer = w.getBuffer();
            int point = w.getPoint();
            int currentLine = buffer.lineIndexForOffset(point);
            if (currentLine <= 0) {
                return null;
            }
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int column = point - currentLineStart;
            int prevLineStart = buffer.lineStartOffset(currentLine - 1);
            int prevLineLength =
                    (int) buffer.lineText(currentLine - 1).codePoints().count();
            w.setPoint(prevLineStart + Math.min(column, prevLineLength));
            return null;
        });
    }

    /**
     * カーソルを行頭に移動する。
     */
    public CompletableFuture<Void> moveToBeginningOfLine() {
        return atomicPerform(w -> {
            var buffer = w.getBuffer();
            int lineIndex = buffer.lineIndexForOffset(w.getPoint());
            w.setPoint(buffer.lineStartOffset(lineIndex));
            return null;
        });
    }

    /**
     * カーソルを行末に移動する。
     */
    public CompletableFuture<Void> moveToEndOfLine() {
        return atomicPerform(w -> {
            var buffer = w.getBuffer();
            int lineIndex = buffer.lineIndexForOffset(w.getPoint());
            int lineStart = buffer.lineStartOffset(lineIndex);
            int lineLength = (int) buffer.lineText(lineIndex).codePoints().count();
            w.setPoint(lineStart + lineLength);
            return null;
        });
    }

    // ── マーク操作 ──

    /**
     * 指定位置にmarkを設定する。
     */
    public CompletableFuture<Void> setMark(int position) {
        return atomicPerform(w -> {
            w.setMark(position);
            return null;
        });
    }

    /**
     * markをクリアする。
     */
    public CompletableFuture<Void> clearMark() {
        return atomicPerform(w -> {
            w.clearMark();
            return null;
        });
    }

    /**
     * markの位置を返す。
     */
    public CompletableFuture<Optional<Integer>> getMark() {
        return atomicPerform(Window::getMark);
    }

    /**
     * regionの開始位置を返す。markが未設定の場合はempty。
     */
    public CompletableFuture<Optional<Integer>> getRegionStart() {
        return atomicPerform(Window::getRegionStart);
    }

    /**
     * regionの終了位置を返す。markが未設定の場合はempty。
     */
    public CompletableFuture<Optional<Integer>> getRegionEnd() {
        return atomicPerform(Window::getRegionEnd);
    }

    // ── テキスト編集（ドメイン操作） ──

    /**
     * カーソルから行末まで削除し、削除テキストを返す。
     * 行末にいる場合は改行を削除。バッファ末尾では何もしない。
     */
    public CompletableFuture<Optional<String>> killLine() {
        return atomicPerform(w -> {
            var buffer = w.getBuffer();
            int point = w.getPoint();
            int bufferLength = buffer.length();
            if (point >= bufferLength) {
                return Optional.empty();
            }
            int lineIndex = buffer.lineIndexForOffset(point);
            int lineStart = buffer.lineStartOffset(lineIndex);
            int lineLength = (int) buffer.lineText(lineIndex).codePoints().count();
            int lineEnd = lineStart + lineLength;

            int deleteCount = (point < lineEnd) ? lineEnd - point : 1;
            String killedText = buffer.substring(point, point + deleteCount);
            w.deleteForward(deleteCount);
            return Optional.of(killedText);
        });
    }

    /**
     * mark〜point間のテキストを削除し、削除テキストを返す。
     * markが未設定の場合はempty。undo記録付き。
     */
    public CompletableFuture<Optional<String>> killRegion() {
        return atomicPerform(w -> {
            var regionStart = w.getRegionStart();
            var regionEnd = w.getRegionEnd();
            if (regionStart.isEmpty() || regionEnd.isEmpty()) {
                return Optional.empty();
            }
            int start = regionStart.get();
            int end = regionEnd.get();
            if (start == end) {
                return Optional.empty();
            }
            var buffer = w.getBuffer();
            int cursorBefore = w.getPoint();
            String killedText = buffer.substring(start, end);
            var inverseChange = buffer.deleteText(start, end - start);
            buffer.getUndoManager().record(inverseChange, cursorBefore);
            buffer.markDirty();
            w.setPoint(start);
            w.clearMark();
            return Optional.of(killedText);
        });
    }

    /**
     * mark〜point間のテキストを返す（削除しない）。markが未設定の場合はempty。
     */
    public CompletableFuture<Optional<String>> copyRegion() {
        return atomicPerform(w -> {
            var regionStart = w.getRegionStart();
            var regionEnd = w.getRegionEnd();
            if (regionStart.isEmpty() || regionEnd.isEmpty()) {
                return Optional.empty();
            }
            int start = regionStart.get();
            int end = regionEnd.get();
            if (start == end) {
                return Optional.empty();
            }
            String copiedText = w.getBuffer().substring(start, end);
            w.clearMark();
            return Optional.of(copiedText);
        });
    }

    // ── Undo / Redo ──

    /**
     * 直前の変更を取り消す。undoできた場合true。
     */
    public CompletableFuture<Boolean> undo() {
        return atomicPerform(w -> {
            var buffer = w.getBuffer();
            var undoManager = buffer.getUndoManager();
            var entryOpt = undoManager.undo();
            if (entryOpt.isEmpty()) {
                return false;
            }
            var entry = entryOpt.get();
            undoManager.suppressRecording();
            try {
                buffer.apply(entry.change());
                w.setPoint(entry.cursorPosition());
            } finally {
                undoManager.resumeRecording();
            }
            return true;
        });
    }

    /**
     * 直前のundoをやり直す。redoできた場合true。
     */
    public CompletableFuture<Boolean> redo() {
        return atomicPerform(w -> {
            var buffer = w.getBuffer();
            var undoManager = buffer.getUndoManager();
            var entryOpt = undoManager.redo();
            if (entryOpt.isEmpty()) {
                return false;
            }
            var entry = entryOpt.get();
            undoManager.suppressRecording();
            try {
                buffer.apply(entry.change());
                var change = entry.change();
                if (change instanceof TextChange.Insert insert) {
                    int insertedLen = (int) insert.text().codePoints().count();
                    w.setPoint(insert.offset() + insertedLen);
                } else if (change instanceof TextChange.Delete delete) {
                    w.setPoint(delete.offset());
                }
            } finally {
                undoManager.resumeRecording();
            }
            return true;
        });
    }
}
