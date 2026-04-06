package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
import io.github.shomah4a.alle.core.setting.BufferLocalSettings;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import io.github.shomah4a.alle.libs.ringbuffer.ArrayRingBuffer;
import io.github.shomah4a.alle.libs.ringbuffer.RingBuffer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * メッセージバッファ（*Messages*）。
 * RingBuffer&lt;String&gt;をストレージとし、行単位でメッセージを保持する。
 * 読み取り専用バッファとして振る舞い、message()メソッドで行を追加する。
 * エコーエリアに最後のメッセージを表示するための機能も提供する。
 * 全操作はReentrantLockで保護され、スレッドセーフ。
 */
public class MessageBuffer implements Buffer {

    private final String name;
    private final RingBuffer<String> lines;
    private final ReentrantLock lock;
    private final UndoManager undoManager;
    private final MajorMode majorMode;
    private final BufferLocalSettings settings;
    private boolean showingMessage;

    /**
     * 指定した名前と最大行数でメッセージバッファを作成する。
     *
     * @param name バッファ名（通常 "*Messages*"）
     * @param maxLines 保持する最大行数
     * @param settingsRegistry 設定レジストリ
     */
    public MessageBuffer(String name, int maxLines, SettingsRegistry settingsRegistry) {
        this.name = name;
        this.lines = new ArrayRingBuffer<>(maxLines);
        this.lock = new ReentrantLock();
        this.undoManager = new UndoManager();
        this.majorMode = new TextMode();
        this.settings = new BufferLocalSettings(settingsRegistry, () -> majorMode, Lists.immutable::empty);
        this.showingMessage = false;
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

    /**
     * メッセージを追加する。
     * エコーエリア表示フラグもセットする。
     */
    public void message(String text) {
        lockedVoid(() -> {
            lines.add(text);
            showingMessage = true;
        });
    }

    /**
     * 最後のメッセージを返す。
     */
    public Optional<String> getLastMessage() {
        return locked(() -> {
            if (lines.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(lines.get(lines.size() - 1));
        });
    }

    /**
     * エコーエリアにメッセージを表示中かどうかを返す。
     */
    public boolean isShowingMessage() {
        return locked(() -> showingMessage);
    }

    /**
     * エコーエリアのメッセージ表示フラグをクリアする。
     * 次のキー入力時にCommandLoopから呼ばれる。
     */
    public void clearShowingMessage() {
        lockedVoid(() -> showingMessage = false);
    }

    // ── テキスト読み取り ──

    @Override
    public int length() {
        return locked(() -> {
            if (lines.isEmpty()) {
                return 0;
            }
            int total = 0;
            for (int i = 0; i < lines.size(); i++) {
                total += (int) lines.get(i).codePoints().count();
            }
            total += lines.size() - 1;
            return total;
        });
    }

    @Override
    public int codePointAt(int index) {
        return locked(() -> {
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
                        return (int) '\n';
                    }
                    remaining--;
                }
            }
            throw new IndexOutOfBoundsException("index " + index + " is out of bounds [0, " + length() + ")");
        });
    }

    @Override
    public String substring(int start, int end) {
        return locked(() -> {
            String text = getTextUnsafe();
            int cpStart = text.offsetByCodePoints(0, start);
            int cpEnd = text.offsetByCodePoints(0, end);
            return text.substring(cpStart, cpEnd);
        });
    }

    @Override
    public int lineCount() {
        return locked(() -> Math.max(1, lines.size()));
    }

    @Override
    public int lineIndexForOffset(int offset) {
        return locked(() -> {
            if (lines.isEmpty()) {
                return 0;
            }
            int remaining = offset;
            for (int i = 0; i < lines.size(); i++) {
                int lineLen = (int) lines.get(i).codePoints().count();
                if (remaining <= lineLen) {
                    return i;
                }
                remaining -= lineLen + 1;
            }
            return lines.size() - 1;
        });
    }

    @Override
    public int lineStartOffset(int lineIndex) {
        return locked(() -> {
            int lc = Math.max(1, lines.size());
            if (lineIndex < 0 || lineIndex >= lc) {
                throw new IndexOutOfBoundsException("lineIndex " + lineIndex + " is out of bounds [0, " + lc + ")");
            }
            int offset = 0;
            for (int i = 0; i < lineIndex; i++) {
                offset += (int) lines.get(i).codePoints().count() + 1;
            }
            return offset;
        });
    }

    @Override
    public String lineText(int lineIndex) {
        return locked(() -> {
            if (lines.isEmpty() && lineIndex == 0) {
                return "";
            }
            if (lineIndex < 0 || lineIndex >= lines.size()) {
                throw new IndexOutOfBoundsException(
                        "lineIndex " + lineIndex + " is out of bounds [0, " + lines.size() + ")");
            }
            return lines.get(lineIndex);
        });
    }

    @Override
    public String getText() {
        return locked(this::getTextUnsafe);
    }

    private String getTextUnsafe() {
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
    public void setReadOnly(boolean readOnly) {
        // MessageBufferは常にread-only
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

    @Override
    public void putPointGuard(int start, int end) {
        // メッセージバッファにテキストプロパティは不要
    }

    @Override
    public void removePointGuard(int start, int end) {
        // メッセージバッファにテキストプロパティは不要
    }

    @Override
    public boolean isPointGuardAt(int index) {
        return false;
    }

    @Override
    public int resolvePointGuard(int index, boolean forward) {
        return index;
    }

    @Override
    public void putFace(int start, int end, FaceName faceName) {
        // メッセージバッファにテキストプロパティは不要
    }

    @Override
    public void removeFace(int start, int end) {
        // メッセージバッファにテキストプロパティは不要
    }

    @Override
    public void removeFaceByName(int start, int end, FaceName faceName) {
        // メッセージバッファにテキストプロパティは不要
    }

    @Override
    public ListIterable<StyledSpan> getFaceSpans(int start, int end) {
        return Lists.immutable.empty();
    }

    // ── 設定 ──

    @Override
    public BufferLocalSettings getSettings() {
        return settings;
    }

    // ── Undo ──

    @Override
    public UndoManager getUndoManager() {
        return undoManager;
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
