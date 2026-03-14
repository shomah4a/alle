package io.github.shomah4a.alle.core.buffer;

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
     * 逆操作を返す。InsertならDelete、DeleteならInsert。
     */
    default TextChange inverse() {
        return switch (this) {
            case Insert(var o, var t) -> new Delete(o, t);
            case Delete(var o, var t) -> new Insert(o, t);
        };
    }
}
