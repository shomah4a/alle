package io.github.shomah4a.alle.core.mode.modes.markdown;

import io.github.shomah4a.alle.core.styling.FaceName;
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
            new StylingRule.RegionMatch(Pattern.compile("^```"), Pattern.compile("^```"), FaceName.CODE),
            // HTMLコメント（<!-- --> 複数行リージョン）
            new StylingRule.RegionMatch(Pattern.compile("<!--"), Pattern.compile("-->"), FaceName.COMMENT),
            // 見出し（行全体）
            new StylingRule.LineMatch(Pattern.compile("^#{1,6}\\s.*"), FaceName.HEADING),
            // 水平線（---, ***, ___）
            new StylingRule.LineMatch(
                    Pattern.compile("^\\s*(-\\s*-\\s*-[\\s-]*|\\*\\s*\\*\\s*\\*[\\s*]*|_\\s*_\\s*_[\\s_]*)$"),
                    FaceName.COMMENT),
            // 参照リンク定義（[ref]: url）
            new StylingRule.LineMatch(Pattern.compile("^\\s*\\[[^]]+]:.*"), FaceName.LINK),
            // 引用（> で始まる行全体）
            new StylingRule.LineMatch(Pattern.compile("^>\\s?.*"), FaceName.STRING),
            // インラインコード
            new StylingRule.PatternMatch(Pattern.compile("`[^`]+`"), FaceName.CODE),
            // 太字（**text** または __text__）
            new StylingRule.PatternMatch(Pattern.compile("\\*\\*[^*]+\\*\\*|__[^_]+__"), FaceName.STRONG),
            // 斜体（*text* または _text_）
            new StylingRule.PatternMatch(
                    Pattern.compile("(?<!\\*)\\*(?!\\*)[^*]+\\*(?!\\*)|(?<!_)_(?!_)[^_]+_(?!_)"), FaceName.EMPHASIS),
            // 取り消し線（~~text~~）
            new StylingRule.PatternMatch(Pattern.compile("~~[^~]+~~"), FaceName.DELETION),
            // 画像リンク（![alt](url)）
            new StylingRule.PatternMatch(Pattern.compile("!\\[[^]]*]\\([^)]+\\)"), FaceName.LINK),
            // リンク（[text](url)）
            new StylingRule.PatternMatch(Pattern.compile("\\[[^]]+]\\([^)]+\\)"), FaceName.LINK),
            // 参照リンク（[text][ref]）
            new StylingRule.PatternMatch(Pattern.compile("\\[[^]]+]\\[[^]]*]"), FaceName.LINK),
            // タスクリストチェックボックス（- [ ] または - [x]）
            new StylingRule.PatternMatch(Pattern.compile("^\\s*[-*+]\\s\\[[ xX]]\\s"), FaceName.LIST_MARKER),
            // リストマーカー（行頭の - * + または数字.）
            new StylingRule.PatternMatch(Pattern.compile("^\\s*[-*+]\\s|^\\s*\\d+\\.\\s"), FaceName.LIST_MARKER),
            // テーブル区切り行（|---|---|）
            new StylingRule.LineMatch(
                    Pattern.compile("^\\|?[\\s:]*-{3,}[\\s:]*(?:\\|[\\s:]*-{3,}[\\s:]*)*\\|?$"), FaceName.TABLE),
            // テーブルのパイプ記号
            new StylingRule.PatternMatch(Pattern.compile("\\|"), FaceName.TABLE));

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
