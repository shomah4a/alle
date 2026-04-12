package io.github.shomah4a.alle.core.statusline;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

/**
 * ステータスラインのフォーマット定義。
 * StatusLineFormatEntryのリストをラップし、Setting&lt;T&gt;で型安全に扱えるようにする。
 */
public final class StatusLineFormat {

    private final ImmutableList<StatusLineFormatEntry> entries;

    public StatusLineFormat(ListIterable<StatusLineFormatEntry> entries) {
        this.entries = entries.toImmutableList();
    }

    /**
     * フォーマット定義のエントリリストを返す。
     */
    public ImmutableList<StatusLineFormatEntry> entries() {
        return entries;
    }

    /**
     * デフォルトのフォーマット定義を返す。
     * "--" buffer-info "    " position "  " mode-info " " misc-info
     */
    public static StatusLineFormat defaultFormat() {
        return new StatusLineFormat(Lists.immutable.of(
                StatusLineFormatEntry.literal("--"),
                StatusLineFormatEntry.slotRef("buffer-info"),
                StatusLineFormatEntry.literal("    "),
                StatusLineFormatEntry.slotRef("position"),
                StatusLineFormatEntry.literal("  "),
                StatusLineFormatEntry.slotRef("mode-info"),
                StatusLineFormatEntry.literal(" "),
                StatusLineFormatEntry.slotRef("misc-info")));
    }
}
