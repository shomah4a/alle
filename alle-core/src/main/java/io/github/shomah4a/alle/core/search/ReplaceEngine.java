package io.github.shomah4a.alle.core.search;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * query-replace / query-replace-regexp 用の置換検索と TO テンプレート展開を行うエンジン。
 * コードポイント単位のオフセットで結果を返す。
 */
public final class ReplaceEngine {

    private ReplaceEngine() {}

    /**
     * リテラル文字列 {@code from} を {@code [searchFromCp, rangeEndCp]} の範囲で前方検索する。
     *
     * @param text           対象テキスト
     * @param from           検索文字列（空のときは empty を返す）
     * @param searchFromCp   検索開始位置（コードポイント単位）
     * @param rangeEndCp     検索上限（コードポイント単位、排他的）
     * @return マッチがあれば {@link ReplaceMatch.Literal}
     */
    public static Optional<ReplaceMatch.Literal> findLiteralNext(
            String text, String from, int searchFromCp, int rangeEndCp) {
        if (from.isEmpty() || searchFromCp >= rangeEndCp) {
            return Optional.empty();
        }
        int fromChar = text.offsetByCodePoints(0, searchFromCp);
        int endChar = text.offsetByCodePoints(0, rangeEndCp);

        int idx = text.indexOf(from, fromChar);
        if (idx < 0) {
            return Optional.empty();
        }
        int matchEndChar = idx + from.length();
        if (matchEndChar > endChar) {
            return Optional.empty();
        }
        int startCp = text.codePointCount(0, idx);
        int endCp = text.codePointCount(0, matchEndChar);
        return Optional.of(new ReplaceMatch.Literal(startCp, endCp));
    }

    /**
     * 正規表現 {@code pattern} を {@code [searchFromCp, rangeEndCp]} の範囲で前方検索する。
     * region 指定で範囲を限定するが、anchoringBounds / transparentBounds は
     * どちらも false にし、{@code ^} / {@code $} がバッファ先頭/行末/末尾に一致する
     * Java 標準挙動を維持する。
     *
     * @return マッチがあれば {@link ReplaceMatch.Regex}（グループは空文字列正規化済み）
     */
    public static Optional<ReplaceMatch.Regex> findRegexpNext(
            String text, Pattern pattern, int searchFromCp, int rangeEndCp) {
        if (searchFromCp >= rangeEndCp) {
            return Optional.empty();
        }
        int fromChar = text.offsetByCodePoints(0, searchFromCp);
        int endChar = text.offsetByCodePoints(0, rangeEndCp);
        Matcher matcher = pattern.matcher(text);
        matcher.region(fromChar, endChar);
        matcher.useAnchoringBounds(false);
        matcher.useTransparentBounds(false);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int startCp = text.codePointCount(0, matcher.start());
        int endCp = text.codePointCount(0, matcher.end());

        MutableList<String> groups = Lists.mutable.empty();
        int count = matcher.groupCount();
        for (int i = 0; i <= count; i++) {
            String g = matcher.group(i);
            groups.add(g == null ? "" : g);
        }
        return Optional.of(new ReplaceMatch.Regex(startCp, endCp, groups.toImmutable()));
    }

    /**
     * Emacs 互換の後方参照展開を行う。
     * 解釈規則:
     * <ul>
     *   <li>{@code \&} — マッチ全体</li>
     *   <li>{@code \0} — マッチ全体</li>
     *   <li>{@code \1}..{@code \9} — 対応するキャプチャグループ（存在しなければ空文字列）</li>
     *   <li>{@code \\} — バックスラッシュ 1 文字</li>
     *   <li>その他 {@code \X} — リテラル {@code X}（Emacs の寛容解釈）</li>
     * </ul>
     * {@code $} は特殊扱いしない。
     *
     * @param template  TO テンプレート文字列
     * @param match     正規表現マッチ
     * @return 展開後の置換文字列
     */
    public static String expandEmacsReplacement(String template, ReplaceMatch.Regex match) {
        var sb = new StringBuilder();
        int len = template.length();
        int i = 0;
        while (i < len) {
            char c = template.charAt(i);
            if (c != '\\' || i + 1 >= len) {
                sb.append(c);
                i++;
                continue;
            }
            char next = template.charAt(i + 1);
            if (next == '\\') {
                sb.append('\\');
                i += 2;
            } else if (next == '&' || next == '0') {
                sb.append(match.groups().get(0));
                i += 2;
            } else if (next >= '1' && next <= '9') {
                int groupIdx = next - '0';
                if (groupIdx < match.groups().size()) {
                    sb.append(match.groups().get(groupIdx));
                }
                // 存在しないグループは空文字列扱い（何も追加しない）
                i += 2;
            } else {
                // 他の \X はリテラル X
                sb.append(next);
                i += 2;
            }
        }
        return sb.toString();
    }
}
