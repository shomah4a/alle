package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.styling.FaceName;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;

/**
 * ANSI エスケープシーケンスをパースして、プレーンテキストとスタイル情報に分解する。
 *
 * <p>SGR (Select Graphic Rendition) シーケンス {@code ESC[...m} を解釈し、
 * それ以外のエスケープシーケンスは除去する。
 * 行をまたぐ SGR 状態を保持するため、インスタンスは対話セッション全体で共有する。
 *
 * <p>このクラスはシェルモード内部でのみ使用する。
 */
final class AnsiParser {

    /** ESC[ で始まるすべての CSI シーケンスにマッチする正規表現 */
    private static final Pattern CSI_PATTERN = Pattern.compile("\u001b\\[([0-9;]*)([A-Za-z])");

    /** OSC シーケンス: ESC ] ... (BEL | ESC \) にマッチする正規表現 */
    private static final Pattern OSC_PATTERN = Pattern.compile("\u001b\\].*?(?:\u0007|\u001b\\\\)");

    /** ESC で始まるその他のエスケープシーケンス（2文字のもの）にマッチする正規表現 */
    private static final Pattern OTHER_ESC_PATTERN = Pattern.compile("\u001b[^\\[\\]]");

    private SgrAttributes currentAttributes;

    AnsiParser() {
        this.currentAttributes = SgrAttributes.reset();
    }

    /**
     * 生の出力行をパースし、スタイル付きセグメントのリストを返す。
     *
     * @param rawLine ANSI エスケープシーケンスを含む可能性のある行
     * @return パース結果のセグメントリスト。テキストが空のセグメントは含まない
     */
    ImmutableList<StyledSegment> parse(String rawLine) {
        MutableList<StyledSegment> segments = Lists.mutable.empty();
        Matcher matcher = CSI_PATTERN.matcher(rawLine);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String textBefore = rawLine.substring(lastEnd, matcher.start());
                String cleaned = removeNonCsiEscapes(textBefore);
                addSegment(segments, cleaned, currentAttributes.toFaceName());
            }

            String finalChar = matcher.group(2);
            if ("m".equals(finalChar)) {
                applySgrCodes(matcher.group(1));
            }
            lastEnd = matcher.end();
        }

        if (lastEnd < rawLine.length()) {
            String remaining = rawLine.substring(lastEnd);
            String cleaned = removeNonCsiEscapes(remaining);
            addSegment(segments, cleaned, currentAttributes.toFaceName());
        }

        return segments.toImmutable();
    }

    private void applySgrCodes(String paramString) {
        if (paramString.isEmpty()) {
            currentAttributes = currentAttributes.withSgrCode(0);
            return;
        }
        int start = 0;
        int sep;
        while (start <= paramString.length()) {
            sep = paramString.indexOf(';', start);
            if (sep < 0) {
                sep = paramString.length();
            }
            String param = paramString.substring(start, sep);
            start = sep + 1;
            if (param.isEmpty()) {
                continue;
            }
            try {
                int code = Integer.parseInt(param);
                currentAttributes = currentAttributes.withSgrCode(code);
            } catch (NumberFormatException ignored) {
                // 不正なパラメータは無視する
            }
        }
    }

    private static void addSegment(MutableList<StyledSegment> segments, String text, @Nullable FaceName face) {
        if (!text.isEmpty()) {
            segments.add(new StyledSegment(text, face));
        }
    }

    private static String removeNonCsiEscapes(String text) {
        String withoutOsc = OSC_PATTERN.matcher(text).replaceAll("");
        return OTHER_ESC_PATTERN.matcher(withoutOsc).replaceAll("");
    }

    /**
     * パース結果のスタイル付きセグメント。
     *
     * @param text プレーンテキスト（エスケープシーケンス除去済み）
     * @param face 適用する FaceName。デフォルトスタイルの場合は {@code null}
     */
    record StyledSegment(String text, @Nullable FaceName face) {}
}
