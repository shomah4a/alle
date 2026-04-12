package io.github.shomah4a.alle.core.mode.modes.makefile;

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
 * Makefileのシンタックススタイラー。
 * StylingRuleのリストを宣言的に定義し、RegexStylerに委譲する。
 */
public class MakefileStyler implements SyntaxStyler {

    /** ターゲット行の判定パターン。変数代入行を除外した上でコロンを検出する。 */
    static final Pattern TARGET_PATTERN = Pattern.compile("^[^\\t#=]*[^:=?+]:[^=].*$|^[^\\t#=]*[^:=?+]:$");

    private static final ListIterable<StylingRule> RULES = Lists.immutable.of(
            // コメント（# から行末まで）
            new StylingRule.PatternMatch(Pattern.compile("#.*"), FaceName.COMMENT),
            // ディレクティブ（行頭のキーワード）
            new StylingRule.PatternMatch(
                    Pattern.compile(
                            "^\\s*(?:include|sinclude|-include|ifeq|ifneq|ifdef|ifndef|else|endif|define|endef|export|unexport|override|vpath)\\b"),
                    FaceName.KEYWORD),
            // 特殊ターゲット（.PHONY, .DEFAULT 等）
            new StylingRule.PatternMatch(
                    Pattern.compile(
                            "^\\.(?:PHONY|SUFFIXES|DEFAULT|PRECIOUS|INTERMEDIATE|SECONDARY|SECONDEXPANSION|DELETE_ON_ERROR|IGNORE|LOW_RESOLUTION_TIME|SILENT|EXPORT_ALL_VARIABLES|NOTPARALLEL|ONESHELL|POSIX)\\b"),
                    FaceName.BUILTIN),
            // 変数定義（VAR =, VAR :=, VAR ?=, VAR +=）の変数名部分
            new StylingRule.PatternMatch(
                    Pattern.compile("^\\s*[A-Za-z_][A-Za-z0-9_]*\\s*(?::=|\\?=|\\+=|=)"), FaceName.VARIABLE),
            // ターゲット行（コロンまでの部分）
            new StylingRule.LineMatch(TARGET_PATTERN, FaceName.FUNCTION_NAME),
            // 変数参照・関数呼び出し（$(VAR), ${VAR}）
            new StylingRule.PatternMatch(Pattern.compile("\\$\\([^)]+\\)|\\$\\{[^}]+}"), FaceName.VARIABLE),
            // 自動変数（$@, $<, $^, $?, $*, $%, $+）
            new StylingRule.PatternMatch(Pattern.compile("\\$[@<^?*%+]"), FaceName.BUILTIN));

    private final RegexStyler delegate;

    public MakefileStyler() {
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
