package io.github.shomah4a.alle.core.mode.modes.terraform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.mode.indent.CStyleIndentState;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry.LanguageSupport;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TerraformModeTest {

    private LanguageSupport lang;
    private TerraformMode mode;

    @BeforeEach
    void setUp() {
        // テストごとに独立した LanguageSupport を生成する。
        // TreeSitterSession/Analyzer/Styler はキャッシュ状態を持つため、
        // テスト間で共有するとテスト順序に依存したフレーキー挙動になりうる。
        lang = SyntaxAnalyzerRegistry.createWithBuiltins().create("hcl").orElseThrow();
        mode = new TerraformMode(lang);
    }

    @Test
    void モード名がterraformである() {
        assertEquals("terraform", mode.name());
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

        private SyntaxStyler styler;

        @BeforeEach
        void setUpStyler() {
            styler = lang.styler();
        }

        @Test
        void コメントにCOMMENT_Faceが適用される() {
            var spans = styler.styleLine("# this is a comment");
            var commentSpans = spans.select(s -> s.faceName().equals(FaceName.COMMENT));
            assertFalse(commentSpans.isEmpty());
        }

        @Test
        void 文字列にSTRING_Faceが適用される() {
            var spans = styler.styleLine("description = \"hello world\"");
            var stringSpans = spans.select(s -> s.faceName().equals(FaceName.STRING));
            assertFalse(stringSpans.isEmpty());
        }

        @Test
        void 数値にNUMBER_Faceが適用される() {
            var spans = styler.styleLine("count = 42");
            var numSpans = spans.select(s -> s.faceName().equals(FaceName.NUMBER));
            assertFalse(numSpans.isEmpty());
        }

        @Test
        void bool値にBUILTIN_Faceが適用される() {
            var spans = styler.styleLine("enabled = true");
            var boolSpans = spans.select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertFalse(boolSpans.isEmpty());
        }

        @Test
        void resourceブロックの種別識別子にKEYWORD_Faceが適用される() {
            // nvim-treesitter のクエリでは body > block > identifier が @keyword（resource など）
            var lines = Lists.immutable.of("resource \"aws_instance\" \"web\" {", "  ami = \"x\"", "}");
            var result = styler.styleDocument(lines);
            var keywordSpans = result.get(0).select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(keywordSpans.isEmpty(), "resource identifierがKEYWORD");
        }

        @Test
        void 関数呼び出しの識別子にFUNCTION_NAME_Faceが適用される() {
            var spans = styler.styleLine("name = lower(\"WEB\")");
            var funcSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertFalse(funcSpans.isEmpty());
        }

        @Test
        void heredocにSTRING_Faceが適用される() {
            var lines = Lists.immutable.of("script = <<EOF", "hello", "EOF");
            var result = styler.styleDocument(lines);
            // 文字列ハイライトがいずれかの行に含まれる
            boolean anyString = result.collect(
                            line -> line.anySatisfy(s -> s.faceName().equals(FaceName.STRING)))
                    .anySatisfy(Boolean::booleanValue);
            assertTrue(anyString, "heredoc にSTRINGが含まれる");
        }

        @Test
        void 複数行ドキュメントの行数が入力行数と一致する() {
            var lines = Lists.immutable.of("# top", "resource \"x\" \"y\" {", "  attr = 1", "}");
            var result = styler.styleDocument(lines);
            assertEquals(4, result.size());
        }
    }

    @Nested
    class オートインデント {

        private Window createWindow(String text) {
            var buffer = new TextBuffer("test.tf", new GapTextModel(), new SettingsRegistry());
            var facade = new BufferFacade(buffer);
            var window = new Window(facade);
            if (!text.isEmpty()) {
                window.insert(text);
            }
            return window;
        }

        private CStyleIndentState createState() {
            // 本番コードの INDENT_CONFIG を共有してテストで再定義しない
            return new CStyleIndentState(TerraformMode.INDENT_CONFIG, lang.analyzer());
        }

        @Test
        void resourceブロック開き波括弧の後にインデントが増加する() {
            var window = createWindow("resource \"aws_instance\" \"web\" {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals(
                    "resource \"aws_instance\" \"web\" {\n  ",
                    window.getBuffer().getText());
        }

        @Test
        void オブジェクトリテラル開き波括弧の後にインデントが増加する() {
            var window = createWindow("  tags = {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("  tags = {\n    ", window.getBuffer().getText());
        }

        @Test
        void リスト開き角括弧の後にインデントが増加する() {
            var window = createWindow("  network_ids = [");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("  network_ids = [\n    ", window.getBuffer().getText());
        }

        @Test
        void 関数呼び出し開き丸括弧の後にインデントが増加する() {
            var window = createWindow("  name = lower(");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("  name = lower(\n    ", window.getBuffer().getText());
        }

        @Test
        void 通常の属性行の後はインデントが継承される() {
            var window = createWindow("  ami = \"ami-123\"");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("  ami = \"ami-123\"\n  ", window.getBuffer().getText());
        }

        @Test
        void heredoc開始直後の改行ではインデントが増加しない() {
            // `<<EOF` の直後で Enter を押した場合、行末は `{`/`[`/`(` でないので +indent_width されない
            var window = createWindow("script = <<EOF");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("script = <<EOF\n", window.getBuffer().getText());
        }

        @Test
        void heredoc内の改行で異常な自動インデントが入らない() {
            // <<EOF の中の本文で改行しても、行末が `{`/`[`/`(` でなければ +indent_width されない
            var window = createWindow("script = <<EOF\nhello");
            var state = createState();
            state.newlineAndIndent(window);
            // hello の後で改行 → インデント無し（heredoc 内は実質的にプレーンテキスト扱い）
            assertEquals("script = <<EOF\nhello\n", window.getBuffer().getText());
        }
    }
}
