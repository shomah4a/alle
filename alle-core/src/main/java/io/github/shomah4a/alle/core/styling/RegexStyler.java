package io.github.shomah4a.alle.core.styling;

import java.util.Optional;
import java.util.regex.Matcher;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * StylingRuleのリストに基づいて行単位でスタイリングを行う汎用SyntaxStyler実装。
 * ルールは定義順に評価され、先にマッチしたルールが優先される（重複範囲は後のルールが無視される）。
 * RegionMatchルールにより複数行にまたがるリージョンのスタイリングにも対応する。
 */
public class RegexStyler implements SyntaxStyler {

    private final ListIterable<StylingRule> rules;

    public RegexStyler(ListIterable<StylingRule> rules) {
        this.rules = rules;
    }

    @Override
    public ListIterable<StyledSpan> styleLine(String lineText) {
        return styleLineWithState(lineText, StylingState.NONE).spans();
    }

    @Override
    public StylingResult styleLineWithState(String lineText, StylingState state) {
        if (lineText.isEmpty()) {
            return new StylingResult(Lists.immutable.empty(), state);
        }

        int codePointCount = (int) lineText.codePoints().count();
        MutableList<StyledSpan> spans = Lists.mutable.empty();
        boolean[] covered = new boolean[codePointCount];

        // リージョン継続中の場合、close パターンを探す
        StylingState nextState = StylingState.NONE;
        int regionEndCharOffset = 0;

        if (state.isInRegion()) {
            var region = state.activeRegion().get();
            Matcher closeMatcher = region.close().matcher(lineText);
            if (closeMatcher.find()) {
                // close が見つかった: 行頭〜close終了までリージョン Face を適用
                int closeEndChar = closeMatcher.end();
                int cpEnd = charOffsetToCodePointOffset(lineText, closeEndChar);
                addSpanIfNotCovered(spans, covered, 0, cpEnd, region.faceName());
                regionEndCharOffset = closeEndChar;
                nextState = StylingState.NONE;
            } else {
                // close が見つからない: 行全体にリージョン Face を適用し、リージョン継続
                addSpanIfNotCovered(spans, covered, 0, codePointCount, region.faceName());
                spans.sortThis((a, b) -> Integer.compare(a.start(), b.start()));
                return new StylingResult(spans, state);
            }
        }

        // 通常ルール処理（リージョン終了後の残り部分も含む）
        // NOTE: 本来は switch + レコードデコンストラクションで書くべきだが、
        // JDK 22以前の javac バグ (JDK-8332725) により ErrorProne の AlreadyChecked が
        // クラッシュするため、instanceof パターンマッチで代替している。
        // JDK 23以上に移行した際にはswitch式に戻すこと。
        Optional<StylingRule.RegionMatch> openedRegion = Optional.empty();
        for (var rule : rules) {
            if (rule instanceof StylingRule.LineMatch lineMatch) {
                if (lineMatch.pattern().matcher(lineText).matches()) {
                    addSpanIfNotCovered(spans, covered, 0, codePointCount, lineMatch.faceName());
                }
            } else if (rule instanceof StylingRule.PatternMatch patternMatch) {
                var matcher = patternMatch.pattern().matcher(lineText);
                while (matcher.find()) {
                    int charStart = matcher.start();
                    int charEnd = matcher.end();
                    int cpStart = charOffsetToCodePointOffset(lineText, charStart);
                    int cpEnd = charOffsetToCodePointOffset(lineText, charEnd);
                    addSpanIfNotCovered(spans, covered, cpStart, cpEnd, patternMatch.faceName());
                }
            } else if (rule instanceof StylingRule.RegionMatch regionMatch) {
                var regionResult =
                        processRegionMatch(lineText, regionMatch, spans, covered, codePointCount, regionEndCharOffset);
                if (regionResult.isPresent() && openedRegion.isEmpty()) {
                    openedRegion = regionResult;
                }
            }
        }

        if (openedRegion.isPresent()) {
            nextState = new StylingState(openedRegion);
        }

        spans.sortThis((a, b) -> Integer.compare(a.start(), b.start()));
        return new StylingResult(spans, nextState);
    }

    /**
     * RegionMatch の open パターンを探し、同一行内で close が見つかればその範囲を適用する。
     * close が見つからなければ open 位置〜行末を適用し、リージョン継続として返す。
     *
     * @return close が見つからなかった場合に、継続する RegionMatch を返す
     */
    private Optional<StylingRule.RegionMatch> processRegionMatch(
            String lineText,
            StylingRule.RegionMatch regionMatch,
            MutableList<StyledSpan> spans,
            boolean[] covered,
            int codePointCount,
            int searchFromCharOffset) {
        Matcher openMatcher = regionMatch.open().matcher(lineText);
        int searchFrom = searchFromCharOffset;
        Optional<StylingRule.RegionMatch> pendingRegion = Optional.empty();

        while (openMatcher.find(searchFrom)) {
            int openStartChar = openMatcher.start();
            int openStartCp = charOffsetToCodePointOffset(lineText, openStartChar);

            // open位置が既にカバーされていればスキップ
            if (openStartCp < codePointCount && covered[openStartCp]) {
                searchFrom = openMatcher.end();
                continue;
            }

            // open の後から close を探す
            int afterOpenChar = openMatcher.end();
            Matcher closeMatcher = regionMatch.close().matcher(lineText);
            if (closeMatcher.find(afterOpenChar)) {
                // 同一行内で close が見つかった
                int closeEndChar = closeMatcher.end();
                int closeEndCp = charOffsetToCodePointOffset(lineText, closeEndChar);
                addSpanIfNotCovered(spans, covered, openStartCp, closeEndCp, regionMatch.faceName());
                searchFrom = closeEndChar;
            } else {
                // close が見つからない: open 位置〜行末をリージョン Face で適用
                addSpanIfNotCovered(spans, covered, openStartCp, codePointCount, regionMatch.faceName());
                pendingRegion = Optional.of(regionMatch);
                break;
            }
        }

        return pendingRegion;
    }

    static void addSpanIfNotCovered(
            MutableList<StyledSpan> spans, boolean[] covered, int start, int end, FaceName faceName) {
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
                spans.add(new StyledSpan(spanStart, i, faceName));
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
