package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.TextMode;
import io.github.shomah4a.alle.libs.ringbuffer.ArrayRingBuffer;
import io.github.shomah4a.alle.libs.ringbuffer.RingBuffer;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * メッセージバッファ（*Messages*）。
 * RingBuffer&lt;String&gt;をストレージとし、行単位でメッセージを保持する。
 * 読み取り専用バッファとして振る舞い、message()メソッドで行を追加する。
 * エコーエリアに最後のメッセージを表示するための機能も提供する。
 */
public class MessageBuffer implements Buffer {

    private final String name;
    private final RingBuffer<String> lines;
    private final UndoManager undoManager;
    private final MajorMode majorMode;
    private boolean showingMessage;

    /**
     * 指定した名前と最大行数でメッセージバッファを作成する。
     *
     * @param name バッファ名（通常 "*Messages*"）
     * @param maxLines 保持する最大行数
     */
    public MessageBuffer(String name, int maxLines) {
        this.name = name;
        this.lines = new ArrayRingBuffer<>(maxLines);
        this.undoManager = new UndoManager();
        this.majorMode = new TextMode();
        this.showingMessage = false;
    }

    /**
     * メッセージを追加する。
     * エコーエリア表示フラグもセットする。
     */
    public void message(String text) {
        lines.add(text);
        showingMessage = true;
    }

    /**
     * 最後のメッセージを返す。
     */
    public Optional<String> getLastMessage() {
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(lines.get(lines.size() - 1));
    }

    /**
     * エコーエリアにメッセージを表示中かどうかを返す。
     */
    public boolean isShowingMessage() {
        return showingMessage;
    }

    /**
     * エコーエリアのメッセージ表示フラグをクリアする。
     * 次のキー入力時にCommandLoopから呼ばれる。
     */
    public void clearShowingMessage() {
        showingMessage = false;
    }

    // ── テキスト読み取り ──

    @Override
    public int length() {
        if (lines.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < lines.size(); i++) {
            total += (int) lines.get(i).codePoints().count();
        }
        // 改行文字の分（行数 - 1）
        total += lines.size() - 1;
        return total;
    }

    @Override
    public int codePointAt(int index) {
        int remaining = index;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineLen = (int) line.codePoints().count();
            if (remaining < lineLen) {
                return line.codePointAt(line.offsetByCodePoints(0, remaining));
            }
            remaining -= lineLen;
            if (i < lines.size() - 1) {
                if (remaining == 0) {
                    return '\n';
                }
                remaining--;
            }
        }
        throw new IndexOutOfBoundsException("index " + index + " is out of bounds [0, " + length() + ")");
    }

    @Override
    public String substring(int start, int end) {
        String text = getText();
        int cpStart = text.offsetByCodePoints(0, start);
        int cpEnd = text.offsetByCodePoints(0, end);
        return text.substring(cpStart, cpEnd);
    }

    @Override
    public int lineCount() {
        return Math.max(1, lines.size());
    }

    @Override
    public int lineIndexForOffset(int offset) {
        if (lines.isEmpty()) {
            return 0;
        }
        int remaining = offset;
        for (int i = 0; i < lines.size(); i++) {
            int lineLen = (int) lines.get(i).codePoints().count();
            if (remaining <= lineLen) {
                return i;
            }
            remaining -= lineLen + 1; // +1 for newline
        }
        return lines.size() - 1;
    }

    @Override
    public int lineStartOffset(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lineCount()) {
            throw new IndexOutOfBoundsException(
                    "lineIndex " + lineIndex + " is out of bounds [0, " + lineCount() + ")");
        }
        int offset = 0;
        for (int i = 0; i < lineIndex; i++) {
            offset += (int) lines.get(i).codePoints().count() + 1; // +1 for newline
        }
        return offset;
    }

    @Override
    public String lineText(int lineIndex) {
        if (lines.isEmpty() && lineIndex == 0) {
            return "";
        }
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            throw new IndexOutOfBoundsException(
                    "lineIndex " + lineIndex + " is out of bounds [0, " + lines.size() + ")");
        }
        return lines.get(lineIndex);
    }

    @Override
    public String getText() {
        if (lines.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    // ── テキスト書き込み（読み取り専用なので例外） ──

    @Override
    public TextChange insertText(int index, String text) {
        throw new ReadOnlyBufferException(name);
    }

    @Override
    public TextChange deleteText(int index, int count) {
        throw new ReadOnlyBufferException(name);
    }

    @Override
    public TextChange apply(TextChange change) {
        throw new ReadOnlyBufferException(name);
    }

    // ── メタデータ ──

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<Path> getFilePath() {
        return Optional.empty();
    }

    @Override
    public void setFilePath(Path filePath) {
        // メッセージバッファにファイルパスは設定しない
    }

    @Override
    public LineEnding getLineEnding() {
        return LineEnding.LF;
    }

    @Override
    public void setLineEnding(LineEnding lineEnding) {
        // メッセージバッファに改行コード設定は不要
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void markDirty() {
        // メッセージバッファはdirtyにならない
    }

    @Override
    public void markClean() {
        // メッセージバッファはdirtyにならない
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isSystemBuffer() {
        return true;
    }

    // ── モード ──

    @Override
    public MajorMode getMajorMode() {
        return majorMode;
    }

    @Override
    public void setMajorMode(MajorMode majorMode) {
        // メッセージバッファのモードは変更しない
    }

    @Override
    public ListIterable<MinorMode> getMinorModes() {
        return Lists.mutable.empty();
    }

    @Override
    public void enableMinorMode(MinorMode mode) {
        // メッセージバッファにマイナーモードは不要
    }

    @Override
    public void disableMinorMode(MinorMode mode) {
        // メッセージバッファにマイナーモードは不要
    }

    // ── キーマップ ──

    @Override
    public Optional<Keymap> getLocalKeymap() {
        return Optional.empty();
    }

    @Override
    public void setLocalKeymap(Keymap keymap) {
        // メッセージバッファにローカルキーマップは不要
    }

    @Override
    public void clearLocalKeymap() {
        // メッセージバッファにローカルキーマップは不要
    }

    // ── テキストプロパティ（メッセージバッファでは不要） ──

    @Override
    public void putReadOnly(int start, int end) {
        // メッセージバッファにテキストプロパティは不要
    }

    @Override
    public void removeReadOnly(int start, int end) {
        // メッセージバッファにテキストプロパティは不要
    }

    @Override
    public boolean isReadOnlyAt(int index) {
        return false;
    }

    // ── Undo ──

    @Override
    public UndoManager getUndoManager() {
        return undoManager;
    }

    // ── 同一性 ──

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
