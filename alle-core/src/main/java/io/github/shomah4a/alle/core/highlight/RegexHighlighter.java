package io.github.shomah4a.alle.core.highlight;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * HighlightRuleのリストに基づいて行単位でハイライトを行う汎用SyntaxHighlighter実装。
 * ルールは定義順に評価され、先にマッチしたルールが優先される（重複範囲は後のルールが無視される）。
 */
public class RegexHighlighter implements SyntaxHighlighter {

    private final ListIterable<HighlightRule> rules;

    public RegexHighlighter(ListIterable<HighlightRule> rules) {
        this.rules = rules;
    }

    @Override
    public ListIterable<StyledSpan> highlight(String lineText) {
        if (lineText.isEmpty()) {
            return Lists.immutable.empty();
        }

        int codePointCount = (int) lineText.codePoints().count();
        MutableList<StyledSpan> spans = Lists.mutable.empty();

        // 各コードポイント位置が既にスパンでカバーされているか追跡する
        boolean[] covered = new boolean[codePointCount];

        // NOTE: 本来は switch + レコードデコンストラクションで書くべきだが、
        // JDK 22以前の javac バグ (JDK-8332725) により ErrorProne の AlreadyChecked が
        // クラッシュするため、instanceof パターンマッチで代替している。
        // JDK 23以上に移行した際にはswitch式に戻すこと。
        for (var rule : rules) {
            if (rule instanceof HighlightRule.LineMatch lineMatch) {
                if (lineMatch.pattern().matcher(lineText).matches()) {
                    addSpanIfNotCovered(spans, covered, 0, codePointCount, lineMatch.face());
                }
            } else if (rule instanceof HighlightRule.PatternMatch patternMatch) {
                var matcher = patternMatch.pattern().matcher(lineText);
                while (matcher.find()) {
                    int charStart = matcher.start();
                    int charEnd = matcher.end();
                    int cpStart = charOffsetToCodePointOffset(lineText, charStart);
                    int cpEnd = charOffsetToCodePointOffset(lineText, charEnd);
                    addSpanIfNotCovered(spans, covered, cpStart, cpEnd, patternMatch.face());
                }
            }
        }

        spans.sortThis((a, b) -> Integer.compare(a.start(), b.start()));
        return spans;
    }

    private static void addSpanIfNotCovered(
            MutableList<StyledSpan> spans, boolean[] covered, int start, int end, Face face) {
        // 範囲内に未カバーの部分があるか確認
        boolean hasUncovered = false;
        for (int i = start; i < end; i++) {
            if (!covered[i]) {
                hasUncovered = true;
                break;
            }
        }
        if (!hasUncovered) {
            return;
        }

        // 未カバー部分の連続範囲をスパンとして追加
        int spanStart = -1;
        for (int i = start; i <= end; i++) {
            boolean isCovered = i == end || covered[i];
            if (!isCovered && spanStart < 0) {
                spanStart = i;
            } else if (isCovered && spanStart >= 0) {
                spans.add(new StyledSpan(spanStart, i, face));
                spanStart = -1;
            }
        }

        // カバー済みとしてマーク
        for (int i = start; i < end; i++) {
            covered[i] = true;
        }
    }

    /**
     * 文字列内のcharオフセットをコードポイントオフセットに変換する。
     */
    static int charOffsetToCodePointOffset(String text, int charOffset) {
        return text.codePointCount(0, charOffset);
    }
}
