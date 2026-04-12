package io.github.shomah4a.alle.core.mode.modes.shellscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry.LanguageSupport;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShellScriptModeTest {

    private static final LanguageSupport LANG =
            SyntaxAnalyzerRegistry.createWithBuiltins().create("bash").orElseThrow();

    private final ShellScriptMode mode = new ShellScriptMode(LANG);

    @Test
    void モード名がshellScriptである() {
        assertEquals("shell-script", mode.name());
    }

    @Test
    void スタイラーが設定されている() {
        assertTrue(mode.styler().isPresent());
    }

    @Test
    void 構文解析器が設定されている() {
        assertTrue(mode.syntaxAnalyzer().isPresent());
    }

    @Test
    void キーマップが設定されている() {
        assertTrue(mode.keymap().isPresent());
    }

    @Test
    void コマンドレジストリが設定されている() {
        assertTrue(mode.commandRegistry().isPresent());
    }

    @Test
    void インデント幅のデフォルトが2である() {
        assertEquals(2, mode.settingDefaults().get(EditorSettings.INDENT_WIDTH).orElseThrow());
    }

    @Test
    void コメント文字列のデフォルトがシャープスペースである() {
        assertEquals(
                "# ", mode.settingDefaults().get(EditorSettings.COMMENT_STRING).orElseThrow());
    }

    @Nested
    class シンタックスハイライト {

        private final SyntaxStyler styler = LANG.styler();

        @Test
        void コメントにCOMMENT_Faceが適用される() {
            var spans = styler.styleLine("# this is a comment");
            var commentSpans = spans.select(s -> s.faceName().equals(FaceName.COMMENT));
            assertFalse(commentSpans.isEmpty());
        }

        @Test
        void キーワードifにKEYWORD_Faceが適用される() {
            var lines = Lists.immutable.of("if [ -f file ]; then", "  echo ok", "fi");
            var result = styler.styleDocument(lines);
            // 1行目にキーワード(if, then)が含まれる
            var firstLineKeywords = result.get(0).select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(firstLineKeywords.isEmpty());
            // 3行目にキーワード(fi)が含まれる
            var thirdLineKeywords = result.get(2).select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(thirdLineKeywords.isEmpty());
        }

        @Test
        void 文字列にSTRING_Faceが適用される() {
            var spans = styler.styleLine("echo \"hello world\"");
            var stringSpans = spans.select(s -> s.faceName().equals(FaceName.STRING));
            assertFalse(stringSpans.isEmpty());
        }

        @Test
        void コマンド名にFUNCTION_NAME_Faceが適用される() {
            var spans = styler.styleLine("echo hello");
            var funcSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertFalse(funcSpans.isEmpty());
        }

        @Test
        void forループのキーワードが認識される() {
            var lines = Lists.immutable.of("for i in 1 2 3; do", "  echo $i", "done");
            var result = styler.styleDocument(lines);
            // 1行目にキーワード(for, in, do)が含まれる
            var firstLineKeywords = result.get(0).select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertTrue(firstLineKeywords.size() >= 2);
        }

        @Test
        void 変数参照にVARIABLE_Faceが適用される() {
            var lines = Lists.immutable.of("MY_VAR=hello", "echo $MY_VAR");
            var result = styler.styleDocument(lines);
            // 1行目に変数定義がある
            var firstLineVars = result.get(0).select(s -> s.faceName().equals(FaceName.VARIABLE));
            assertFalse(firstLineVars.isEmpty());
        }

        @Test
        void パイプ演算子にOPERATOR_Faceが適用される() {
            var spans = styler.styleLine("cat file | grep pattern");
            var opSpans = spans.select(s -> s.faceName().equals(FaceName.OPERATOR));
            assertFalse(opSpans.isEmpty());
        }

        @Test
        void 関数定義の関数名にFUNCTION_NAME_Faceが適用される() {
            var lines = Lists.immutable.of("function my_func {", "  echo hello", "}");
            var result = styler.styleDocument(lines);
            var funcSpans = result.get(0).select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertFalse(funcSpans.isEmpty());
        }

        @Test
        void 複数行ドキュメントの行数が入力行数と一致する() {
            var lines = Lists.immutable.of("#!/bin/bash", "echo hello", "exit 0");
            var result = styler.styleDocument(lines);
            assertEquals(3, result.size());
        }
    }
}
