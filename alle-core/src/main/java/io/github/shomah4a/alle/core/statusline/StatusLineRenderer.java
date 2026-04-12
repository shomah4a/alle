package io.github.shomah4a.alle.core.statusline;

import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import org.eclipse.collections.api.list.ListIterable;

/**
 * フォーマット定義をもとにステータスラインの表示文字列を生成するレンダラー。
 * フォーマット定義を走査し、レジストリからハンドラを引いて結果を連結する。
 * 未登録のスロット名は空文字列として扱い、warningBufferに警告を出力する。
 */
public class StatusLineRenderer {

    private final StatusLineRegistry registry;
    private final MessageBuffer warningBuffer;

    public StatusLineRenderer(StatusLineRegistry registry, MessageBuffer warningBuffer) {
        this.registry = registry;
        this.warningBuffer = warningBuffer;
    }

    /**
     * フォーマット定義とコンテキストから表示文字列を生成する。
     */
    public String render(ListIterable<StatusLineFormatEntry> format, StatusLineContext context) {
        var sb = new StringBuilder();
        for (StatusLineFormatEntry entry : format) {
            switch (entry) {
                case StatusLineFormatEntry.Literal literal -> sb.append(literal.text());
                case StatusLineFormatEntry.SlotRef slotRef -> {
                    var element = registry.lookup(slotRef.name());
                    if (element.isPresent()) {
                        sb.append(element.get().render(context));
                    } else {
                        warningBuffer.message("ステータスライン: 未登録のスロット名 '" + slotRef.name() + "'");
                    }
                }
            }
        }
        return sb.toString();
    }
}
