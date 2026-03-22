package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.styling.Face;
import io.github.shomah4a.alle.core.styling.RegexStyler;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import io.github.shomah4a.alle.core.styling.StylingResult;
import io.github.shomah4a.alle.core.styling.StylingRule;
import io.github.shomah4a.alle.core.styling.StylingState;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * Markdownのシンタックススタイラー。
 * StylingRuleのリストを宣言的に定義し、RegexStylerに委譲する。
 */
public class MarkdownStyler implements SyntaxStyler {

    private static final ListIterable<StylingRule> RULES = Lists.immutable.of(
            // コードブロック（``` で囲まれた複数行リージョン）
            new StylingRule.RegionMatch(Pattern.compile("^```"), Pattern.compile("^```"), Face.CODE),
            // 見出し（行全体）
            new StylingRule.LineMatch(Pattern.compile("^#{1,6}\\s.*"), Face.HEADING),
            // 水平線（---, ***, ___）
            new StylingRule.LineMatch(
                    Pattern.compile("^\\s*(-\\s*-\\s*-[\\s-]*|\\*\\s*\\*\\s*\\*[\\s*]*|_\\s*_\\s*_[\\s_]*)$"),
                    Face.COMMENT),
            // 引用（> で始まる行全体）
            new StylingRule.LineMatch(Pattern.compile("^>\\s?.*"), Face.STRING),
            // インラインコード
            new StylingRule.PatternMatch(Pattern.compile("`[^`]+`"), Face.CODE),
            // 太字（**text** または __text__）
            new StylingRule.PatternMatch(Pattern.compile("\\*\\*[^*]+\\*\\*|__[^_]+__"), Face.BOLD_FACE),
            // 斜体（*text* または _text_）
            new StylingRule.PatternMatch(
                    Pattern.compile("(?<!\\*)\\*(?!\\*)[^*]+\\*(?!\\*)|(?<!_)_(?!_)[^_]+_(?!_)"), Face.ITALIC_FACE),
            // 画像リンク（![alt](url)）
            new StylingRule.PatternMatch(Pattern.compile("!\\[[^]]*]\\([^)]+\\)"), Face.LINK),
            // リンク（[text](url)）
            new StylingRule.PatternMatch(Pattern.compile("\\[[^]]+]\\([^)]+\\)"), Face.LINK),
            // リストマーカー（行頭の - * + または数字.）
            new StylingRule.PatternMatch(Pattern.compile("^\\s*[-*+]\\s|^\\s*\\d+\\.\\s"), Face.LIST_MARKER),
            // テーブル区切り行（|---|---|）
            new StylingRule.LineMatch(
                    Pattern.compile("^\\|?[\\s:]*-{3,}[\\s:]*(?:\\|[\\s:]*-{3,}[\\s:]*)*\\|?$"), Face.TABLE),
            // テーブルのパイプ記号
            new StylingRule.PatternMatch(Pattern.compile("\\|"), Face.TABLE));

    private final RegexStyler delegate;

    public MarkdownStyler() {
        this.delegate = new RegexStyler(RULES);
    }

    @Override
    public ListIterable<StyledSpan> styleLine(String lineText) {
        return delegate.styleLine(lineText);
    }

    @Override
    public StylingResult styleLineWithState(String lineText, StylingState state) {
        return delegate.styleLineWithState(lineText, state);
    }

    @Override
    public StylingState initialState() {
        return delegate.initialState();
    }
}
