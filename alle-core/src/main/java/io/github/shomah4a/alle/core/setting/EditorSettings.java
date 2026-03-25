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
     * コメント文字列。デフォルト "# "。
     */
    public static final Setting<String> COMMENT_STRING = Setting.of("comment-string", String.class, "# ");
}
