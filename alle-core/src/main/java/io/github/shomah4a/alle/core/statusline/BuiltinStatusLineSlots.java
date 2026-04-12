package io.github.shomah4a.alle.core.statusline;

/**
 * 標準のステータスラインスロットとグループを定義し、レジストリに登録するファクトリ。
 */
public final class BuiltinStatusLineSlots {

    private BuiltinStatusLineSlots() {}

    /**
     * 標準スロットとグループをレジストリに登録する。
     * スロットを先に登録してからグループの子要素に同一インスタンスを追加するため、
     * スロットの差し替えをグループに反映するにはグループの子要素も更新が必要。
     */
    public static void registerAll(StatusLineRegistry registry) {
        // 末端スロット
        var bufferStatus = new StatusLineSlot("buffer-status", BuiltinStatusLineSlots::renderBufferStatus);
        var truncateIndicator =
                new StatusLineSlot("truncate-indicator", BuiltinStatusLineSlots::renderTruncateIndicator);
        var bufferName = new StatusLineSlot("buffer-name", BuiltinStatusLineSlots::renderBufferName);
        var lineNumber = new StatusLineSlot("line-number", BuiltinStatusLineSlots::renderLineNumber);
        var columnNumber = new StatusLineSlot("column-number", BuiltinStatusLineSlots::renderColumnNumber);
        var majorMode = new StatusLineSlot("major-mode", BuiltinStatusLineSlots::renderMajorMode);
        var minorModes = new StatusLineSlot("minor-modes", BuiltinStatusLineSlots::renderMinorModes);

        registry.register(bufferStatus);
        registry.register(truncateIndicator);
        registry.register(bufferName);
        registry.register(lineNumber);
        registry.register(columnNumber);
        registry.register(majorMode);
        registry.register(minorModes);

        // グループ（レジストリに登録済みのスロットインスタンスを子要素として共有）
        var bufferInfo = new StatusLineGroup("buffer-info");
        bufferInfo.addChild(bufferStatus);
        bufferInfo.addChild(truncateIndicator);
        bufferInfo.addChild(new StatusLineSlot("_buffer-info-sep", ctx -> "  "));
        bufferInfo.addChild(bufferName);
        registry.register(bufferInfo);

        var position = new StatusLineGroup("position");
        position.addChild(new StatusLineSlot("_position-open", ctx -> "("));
        position.addChild(lineNumber);
        position.addChild(new StatusLineSlot("_position-sep", ctx -> ","));
        position.addChild(columnNumber);
        position.addChild(new StatusLineSlot("_position-close", ctx -> ")"));
        registry.register(position);

        var modeInfo = new StatusLineGroup("mode-info");
        modeInfo.addChild(new StatusLineSlot("_mode-info-open", ctx -> "("));
        modeInfo.addChild(majorMode);
        modeInfo.addChild(minorModes);
        modeInfo.addChild(new StatusLineSlot("_mode-info-close", ctx -> ")"));
        registry.register(modeInfo);

        var miscInfo = new StatusLineGroup("misc-info");
        registry.register(miscInfo);
    }

    private static String renderBufferStatus(StatusLineContext ctx) {
        return ctx.buffer().isDirty() ? "**" : "--";
    }

    private static String renderTruncateIndicator(StatusLineContext ctx) {
        return ctx.window().isTruncateLines() ? "$" : "\\";
    }

    private static String renderBufferName(StatusLineContext ctx) {
        return ctx.buffer().getName();
    }

    private static String renderLineNumber(StatusLineContext ctx) {
        int point = ctx.window().getPoint();
        int lineIndex = ctx.buffer().lineIndexForOffset(point);
        return String.valueOf(lineIndex + 1);
    }

    private static String renderColumnNumber(StatusLineContext ctx) {
        int point = ctx.window().getPoint();
        int lineIndex = ctx.buffer().lineIndexForOffset(point);
        int lineStart = ctx.buffer().lineStartOffset(lineIndex);
        return String.valueOf(point - lineStart);
    }

    private static String renderMajorMode(StatusLineContext ctx) {
        return ctx.buffer().getMajorMode().name();
    }

    private static String renderMinorModes(StatusLineContext ctx) {
        var minorModes = ctx.buffer().getMinorModes();
        if (minorModes.isEmpty()) {
            return "";
        }
        return " " + minorModes.collect(m -> m.name()).makeString(" ");
    }
}
