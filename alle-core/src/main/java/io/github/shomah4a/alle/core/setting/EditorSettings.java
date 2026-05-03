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

    /**
     * インデントにタブ文字を使用するかどうか。デフォルト false（スペースを使用）。
     * true の場合、インデント挿入時にスペースの代わりにタブ文字を使用する。
     */
    public static final Setting<Boolean> INDENT_TABS_MODE = Setting.of("indent-tabs-mode", Boolean.class, false);

    /**
     * ミニバッファ補完で前方一致をケース無視で行うかどうか。デフォルト false（ケース敏感）。
     * Emacs の {@code completion-ignore-case} 相当。
     * true の場合、find-file・switch-to-buffer・M-x 等の補完候補が大文字小文字を無視してマッチする。
     */
    public static final Setting<Boolean> COMPLETION_IGNORE_CASE =
            Setting.of("completion-ignore-case", Boolean.class, false);
}
