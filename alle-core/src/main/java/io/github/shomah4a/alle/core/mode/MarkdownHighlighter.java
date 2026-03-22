package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.highlight.Face;
import io.github.shomah4a.alle.core.highlight.HighlightResult;
import io.github.shomah4a.alle.core.highlight.HighlightRule;
import io.github.shomah4a.alle.core.highlight.HighlightState;
import io.github.shomah4a.alle.core.highlight.RegexHighlighter;
import io.github.shomah4a.alle.core.highlight.StyledSpan;
import io.github.shomah4a.alle.core.highlight.SyntaxHighlighter;
import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * Markdownのシンタックスハイライター。
 * HighlightRuleのリストを宣言的に定義し、RegexHighlighterに委譲する。
 */
public class MarkdownHighlighter implements SyntaxHighlighter {

    private static final ListIterable<HighlightRule> RULES = Lists.immutable.of(
            // コードブロック（``` で囲まれた複数行リージョン）
            new HighlightRule.RegionMatch(Pattern.compile("^```"), Pattern.compile("^```"), Face.CODE),
            // 見出し（行全体）
            new HighlightRule.LineMatch(Pattern.compile("^#{1,6}\\s.*"), Face.HEADING),
            // 水平線（---, ***, ___）
            new HighlightRule.LineMatch(
                    Pattern.compile("^\\s*(-\\s*-\\s*-[\\s-]*|\\*\\s*\\*\\s*\\*[\\s*]*|_\\s*_\\s*_[\\s_]*)$"),
                    Face.COMMENT),
            // 引用（> で始まる行全体）
            new HighlightRule.LineMatch(Pattern.compile("^>\\s?.*"), Face.STRING),
            // インラインコード
            new HighlightRule.PatternMatch(Pattern.compile("`[^`]+`"), Face.CODE),
            // 太字（**text** または __text__）
            new HighlightRule.PatternMatch(Pattern.compile("\\*\\*[^*]+\\*\\*|__[^_]+__"), Face.BOLD_FACE),
            // 斜体（*text* または _text_）
            new HighlightRule.PatternMatch(
                    Pattern.compile("(?<!\\*)\\*(?!\\*)[^*]+\\*(?!\\*)|(?<!_)_(?!_)[^_]+_(?!_)"), Face.ITALIC_FACE),
            // 画像リンク（![alt](url)）
            new HighlightRule.PatternMatch(Pattern.compile("!\\[[^]]*]\\([^)]+\\)"), Face.LINK),
            // リンク（[text](url)）
            new HighlightRule.PatternMatch(Pattern.compile("\\[[^]]+]\\([^)]+\\)"), Face.LINK),
            // リストマーカー（行頭の - * + または数字.）
            new HighlightRule.PatternMatch(Pattern.compile("^\\s*[-*+]\\s|^\\s*\\d+\\.\\s"), Face.LIST_MARKER),
            // テーブル区切り行（|---|---|）
            new HighlightRule.LineMatch(
                    Pattern.compile("^\\|?[\\s:]*-{3,}[\\s:]*(?:\\|[\\s:]*-{3,}[\\s:]*)*\\|?$"), Face.TABLE),
            // テーブルのパイプ記号
            new HighlightRule.PatternMatch(Pattern.compile("\\|"), Face.TABLE));

    private final RegexHighlighter delegate;

    public MarkdownHighlighter() {
        this.delegate = new RegexHighlighter(RULES);
    }

    @Override
    public ListIterable<StyledSpan> highlight(String lineText) {
        return delegate.highlight(lineText);
    }

    @Override
    public HighlightResult highlightLine(String lineText, HighlightState state) {
        return delegate.highlightLine(lineText, state);
    }

    @Override
    public HighlightState initialState() {
        return delegate.initialState();
    }
}
