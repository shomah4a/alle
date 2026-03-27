package io.github.shomah4a.alle.core.buffer;

import org.eclipse.collections.api.list.ListIterable;

/**
 * テキスト変更操作。逆操作を導出できる。
 */
public sealed interface TextChange {

    /**
     * テキスト挿入。
     *
     * @param offset 挿入位置（コードポイント単位）
     * @param text   挿入テキスト
     */
    record Insert(int offset, String text) implements TextChange {}

    /**
     * テキスト削除。
     *
     * @param offset 削除開始位置（コードポイント単位）
     * @param text   削除されたテキスト
     */
    record Delete(int offset, String text) implements TextChange {}

    /**
     * 複合変更。複数のTextChangeを1つの操作としてまとめる。
     * withTransactionで記録された複数編集をundo/redoの1単位として扱う。
     *
     * @param changes 適用順に並んだ変更のリスト
     */
    record Compound(ListIterable<TextChange> changes) implements TextChange {}

    /**
     * 逆操作を返す。InsertならDelete、DeleteならInsert。
     * Compoundの場合は各changeのinverseを逆順に並べたCompoundを返す。
     */
    default TextChange inverse() {
        return switch (this) {
            case Insert(var o, var t) -> new Delete(o, t);
            case Delete(var o, var t) -> new Insert(o, t);
            case Compound(var changes) ->
                new Compound(changes.collect(TextChange::inverse).toReversed());
        };
    }

    /**
     * 変更の先頭オフセットを返す。
     * undo後のカーソル位置算出に使用する。
     */
    default int offset() {
        return switch (this) {
            case Insert(var o, var t) -> o;
            case Delete(var o, var t) -> o;
            case Compound(var changes) -> changes.getFirst().offset();
        };
    }

    /**
     * 変更適用後のカーソル位置を返す。
     * redo後のカーソル位置算出に使用する。
     * Insertの場合はoffset + テキスト長、Deleteの場合はoffset。
     * Compoundの場合は最後のchangeの適用後位置。
     */
    default int cursorAfterApply() {
        return switch (this) {
            case Insert(var o, var t) -> o + (int) t.codePoints().count();
            case Delete(var o, var t) -> o;
            case Compound(var changes) -> changes.getLast().cursorAfterApply();
        };
    }
}
