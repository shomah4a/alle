package io.github.shomah4a.alle.core.setting;

/**
 * エディタのコア設定定数。
 */
public final class EditorSettings {

    private EditorSettings() {}

    /**
     * インデント幅（スペース数）。デフォルト4。
     */
    public static final Setting<Integer> INDENT_WIDTH = Setting.of("indent-width", Integer.class, 4);

    /**
     * タブ文字の表示幅（タブストップ間隔）。デフォルト8。
     * タブ文字は次の {@code TAB_WIDTH} の倍数カラムまで空白で展開される。
     */
    public static final Setting<Integer> TAB_WIDTH = Setting.of("tab-width", Integer.class, 8);

    /**
     * コメント文字列。デフォルト "# "。
     */
    public static final Setting<String> COMMENT_STRING = Setting.of("comment-string", String.class, "# ");
}
