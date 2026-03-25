package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import org.eclipse.collections.api.list.ListIterable;

/**
 * エディタのバッファ。
 * テキストデータ、メタデータ、モード情報を統一的に扱うインターフェース。
 * ストレージの実装（GapBuffer、RingBuffer等）に依存しない。
 * パッケージプライベート: 外部からのアクセスは{@link BufferFacade}経由で行う。
 *
 * @see EditableBuffer GapBufferベースの標準実装
 */
interface Buffer {

    // ── テキスト読み取り ──

    /**
     * テキストの長さをコードポイント数で返す。
     */
    int length();

    /**
     * 指定位置のコードポイントを返す。
     *
     * @throws IndexOutOfBoundsException indexが範囲外の場合
     */
    int codePointAt(int index);

    /**
     * 指定範囲の部分文字列を返す。
     *
     * @throws IndexOutOfBoundsException 範囲が不正な場合
     */
    String substring(int start, int end);

    /**
     * 行数を返す。
     */
    int lineCount();

    /**
     * 指定オフセットが属する行のインデックスを返す。
     *
     * @throws IndexOutOfBoundsException offsetが範囲外の場合
     */
    int lineIndexForOffset(int offset);

    /**
     * 指定行の先頭オフセットをコードポイント単位で返す。
     *
     * @throws IndexOutOfBoundsException lineIndexが範囲外の場合
     */
    int lineStartOffset(int lineIndex);

    /**
     * 指定行のテキストを返す(改行文字を含まない)。
     *
     * @throws IndexOutOfBoundsException lineIndexが範囲外の場合
     */
    String lineText(int lineIndex);

    /**
     * 全テキストを返す。
     */
    String getText();

    // ── テキスト書き込み ──

    /**
     * 指定位置に文字列を挿入し、逆操作（Delete）を返す。
     *
     * @throws IndexOutOfBoundsException indexが範囲外の場合
     * @throws ReadOnlyBufferException バッファが読み取り専用の場合
     */
    TextChange insertText(int index, String text);

    /**
     * 指定位置から指定コードポイント数を削除し、逆操作（Insert）を返す。
     *
     * @throws IndexOutOfBoundsException 範囲が不正な場合
     * @throws ReadOnlyBufferException バッファが読み取り専用の場合
     */
    TextChange deleteText(int index, int count);

    /**
     * TextChangeを適用し、逆操作を返す。
     *
     * @throws ReadOnlyBufferException バッファが読み取り専用の場合
     */
    TextChange apply(TextChange change);

    // ── メタデータ ──

    /**
     * バッファ名を返す。
     */
    String getName();

    /**
     * ファイルパスを返す。
     */
    Optional<Path> getFilePath();

    /**
     * ファイルパスを設定する。
     */
    void setFilePath(Path filePath);

    /**
     * 改行コードを返す。
     */
    LineEnding getLineEnding();

    /**
     * 改行コードを設定する。
     */
    void setLineEnding(LineEnding lineEnding);

    /**
     * 変更済みかどうかを返す。
     */
    boolean isDirty();

    /**
     * 変更済みとしてマークする。
     */
    void markDirty();

    /**
     * 未変更としてマークする。
     */
    void markClean();

    /**
     * バッファが読み取り専用かどうかを返す。
     */
    boolean isReadOnly();

    /**
     * エディタが内部管理するシステムバッファかどうかを返す。
     * システムバッファはkill-bufferで削除できない。
     */
    default boolean isSystemBuffer() {
        return false;
    }

    // ── モード ──

    /**
     * メジャーモードを返す。
     */
    MajorMode getMajorMode();

    /**
     * メジャーモードを設定する。
     */
    void setMajorMode(MajorMode majorMode);

    /**
     * 有効なマイナーモードの一覧を返す。
     */
    ListIterable<MinorMode> getMinorModes();

    /**
     * マイナーモードを有効にする。
     */
    void enableMinorMode(MinorMode mode);

    /**
     * マイナーモードを無効にする。
     */
    void disableMinorMode(MinorMode mode);

    // ── キーマップ ──

    /**
     * バッファローカルキーマップを返す。
     * 設定されていない場合はempty。
     */
    Optional<Keymap> getLocalKeymap();

    /**
     * バッファローカルキーマップを設定する。
     */
    void setLocalKeymap(Keymap keymap);

    /**
     * バッファローカルキーマップを解除する。
     */
    void clearLocalKeymap();

    // ── テキストプロパティ ──

    /**
     * 指定範囲にread-onlyプロパティを設定する。
     * 半開区間 [start, end) で管理される。
     */
    void putReadOnly(int start, int end);

    /**
     * 指定範囲のread-onlyプロパティを解除する。
     */
    void removeReadOnly(int start, int end);

    /**
     * 指定位置がread-onlyプロパティで保護されているかを返す。
     */
    boolean isReadOnlyAt(int index);

    // ── Undo ──

    /**
     * UndoManagerを返す。
     */
    UndoManager getUndoManager();

    // ── アトミック操作 ──

    /**
     * 複合操作をアトミックに実行する。
     * EditableBufferではReentrantLockで保護される。
     */
    <T> T atomicOperation(Function<Buffer, T> handler);
}
