package io.github.shomah4a.alle.core.command.commands;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.TransactionalCommand;
import io.github.shomah4a.alle.core.setting.BufferLocalSettings;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

/**
 * バッファまたは選択リージョンを JSON として整形するグローバルコマンド。
 * 対象範囲が複数の JSON 値を含む場合、各値を整形して改行で連結する。
 * インデントはバッファローカル設定の {@code INDENT_TABS_MODE} と {@code INDENT_WIDTH} を参照する。
 */
public class JsonPrettyPrintCommand implements TransactionalCommand {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
            .build();

    @Override
    public String name() {
        return "json-pretty-print";
    }

    @Override
    public CompletableFuture<Void> executeInTransaction(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();

        if (buffer.isReadOnly()) {
            context.messageBuffer().message("Buffer is read-only");
            return CompletableFuture.completedFuture(null);
        }

        var regionStart = window.getRegionStart();
        var regionEnd = window.getRegionEnd();
        boolean useRegion = regionStart.isPresent()
                && regionEnd.isPresent()
                && !regionStart.get().equals(regionEnd.get());
        int start = useRegion ? regionStart.get() : 0;
        int end = useRegion ? regionEnd.get() : buffer.length();

        if (start == end) {
            return CompletableFuture.completedFuture(null);
        }

        String source = buffer.substring(start, end);

        String formatted;
        try {
            formatted = formatJson(source, buffer.getSettings());
        } catch (IOException e) {
            context.messageBuffer().message("JSON parse error: " + firstLine(e.getMessage()));
            return CompletableFuture.completedFuture(null);
        }

        if (!useRegion && source.endsWith("\n") && !formatted.endsWith("\n")) {
            formatted = formatted + "\n";
        }

        if (formatted.equals(source)) {
            return CompletableFuture.completedFuture(null);
        }

        buffer.deleteText(start, end - start);
        buffer.insertText(start, formatted);
        buffer.markDirty();

        int newEnd = start + (int) formatted.codePoints().count();
        if (useRegion) {
            window.setMark(start);
            window.setPoint(newEnd);
        } else {
            window.clearMark();
            window.setPoint(0);
        }
        return CompletableFuture.completedFuture(null);
    }

    private static String formatJson(String source, BufferLocalSettings settings) throws IOException {
        String indent = computeIndent(settings);
        var indenter = new DefaultIndenter(indent, "\n");
        var printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        var writer = MAPPER.writer(printer);

        var sb = new StringBuilder();
        try (JsonParser parser = MAPPER.getFactory().createParser(source)) {
            boolean first = true;
            while (true) {
                JsonNode node = MAPPER.readTree(parser);
                if (node == null || node.isMissingNode()) {
                    break;
                }
                if (!first) {
                    sb.append('\n');
                }
                first = false;
                sb.append(writer.writeValueAsString(node));
            }
        }
        return sb.toString();
    }

    private static String computeIndent(BufferLocalSettings settings) {
        if (settings.get(EditorSettings.INDENT_TABS_MODE)) {
            return "\t";
        }
        int width = settings.get(EditorSettings.INDENT_WIDTH);
        return " ".repeat(Math.max(0, width));
    }

    private static String firstLine(@Nullable String message) {
        if (message == null) {
            return "";
        }
        int idx = message.indexOf('\n');
        return idx < 0 ? message : message.substring(0, idx);
    }
}
