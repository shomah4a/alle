package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.setting.BufferLocalSettings;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.styling.StyledSpan;
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
 * @see TextBuffer GapBufferベースの標準実装
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
     * undo/redo等の履歴適用に使用する。適用中のundo記録は自動的に抑制される。
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
     * バッファの読み取り専用フラグを設定する。
     */
    void setReadOnly(boolean readOnly);

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

    /**
     * 指定範囲にpointGuard（カーソル進入禁止）プロパティを設定する。
     * 半開区間 [start, end) で管理される。
     */
    void putPointGuard(int start, int end);

    /**
     * 指定範囲のpointGuardプロパティを解除する。
     */
    void removePointGuard(int start, int end);

    /**
     * 指定位置がpointGuard（カーソル進入禁止）で保護されているかを返す。
     */
    boolean isPointGuardAt(int index);

    /**
     * 指定範囲にface（表示スタイル）プロパティを設定する。
     * 半開区間 [start, end) で管理される。
     */
    void putFace(int start, int end, FaceName faceName);

    /**
     * 指定範囲のfaceプロパティを除去する。
     */
    void removeFace(int start, int end);

    /**
     * 指定範囲内で指定FaceNameを持つfaceプロパティのみを除去する。
     * 他のFaceNameのプロパティには影響しない。
     */
    void removeFaceByName(int start, int end, FaceName faceName);

    /**
     * 指定範囲 [start, end) 内のface範囲をStyledSpanリストとして返す。
     */
    ListIterable<StyledSpan> getFaceSpans(int start, int end);

    /**
     * 指定位置がpointGuard範囲内の場合、侵入方向と逆方向に押し戻した位置を返す。
     * forward（前方移動）でガードに入った場合はガードのend位置に、
     * backward（後方移動）でガードに入った場合はガードのstart位置に押し戻す。
     * 範囲外の場合は元の位置をそのまま返す。
     *
     * @param index 解決対象の位置
     * @param forward trueなら前方移動、falseなら後方移動
     */
    int resolvePointGuard(int index, boolean forward);

    // ── 設定 ──

    /**
     * バッファローカル設定を返す。
     */
    BufferLocalSettings getSettings();

    // ── バッファ変数 ──

    /**
     * ��ッファ変数を取得する。
     * 型安全性は呼び出し側の責任とする。
     *
     * @param key 変数名
     * @return 変数値。未設定の場合はempty
     */
    Optional<Object> getVariable(String key);

    /**
     * バッファ変数を設定する。
     *
     * @param key 変数名
     * @param value 変数値
     */
    void setVariable(String key, Object value);

    /**
     * バッファ変数を削除する。
     *
     * @param key 変数名
     */
    void removeVariable(String key);

    // ── Undo ──

    /**
     * UndoManagerを返す。
     */
    UndoManager getUndoManager();

    // ── アトミック操作 ──

    /**
     * 複合操作をアトミックに実行する。
     * TextBufferではReentrantLockで保護される。
     */
    <T> T atomicOperation(Function<Buffer, T> handler);
}
