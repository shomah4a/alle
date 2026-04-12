package io.github.shomah4a.alle.core.statusline;

/**
 * ステータスラインフォーマット定義の要素。
 * リテラル文字列とスロット名参照を型レベルで区別する。
 */
public sealed interface StatusLineFormatEntry {

    /**
     * リテラル文字列。そのまま表示される。
     */
    record Literal(String text) implements StatusLineFormatEntry {}

    /**
     * スロット名参照。レジストリからStatusLineElementを引いてrenderする。
     */
    record SlotRef(String name) implements StatusLineFormatEntry {}

    /**
     * リテラル文字列のエントリを生成する。
     */
    static StatusLineFormatEntry literal(String text) {
        return new Literal(text);
    }

    /**
     * スロット名参照のエントリを生成する。
     */
    static StatusLineFormatEntry slotRef(String name) {
        return new SlotRef(name);
    }
}
