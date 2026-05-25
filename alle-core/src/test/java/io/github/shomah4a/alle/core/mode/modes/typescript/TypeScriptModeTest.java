package io.github.shomah4a.alle.core.mode.modes.typescript;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TypeScriptModeTest {

    private LanguageSupport lang;
    private TypeScriptMode mode;

    @BeforeEach
    void setUp() {
        // テストごとに独立した LanguageSupport を生成する。
        // TreeSitterSession/Analyzer/Styler はキャッシュ状態を持つため、
        // テスト間で共有するとテスト順序に依存したフレーキー挙動になりうる。
        lang = SyntaxAnalyzerRegistry.createWithBuiltins().create("typescript").orElseThrow();
        mode = new TypeScriptMode(lang);
    }

    @Test
    void モード名がtypescriptである() {
        assertEquals("typescript", mode.name());
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
    void コメント文字列のデフォルトがスラッシュスラッシュスペースである() {
        assertEquals(
                "// ", mode.settingDefaults().get(EditorSettings.COMMENT_STRING).orElseThrow());
    }

    @Nested
    class シンタックスハイライト {

        private SyntaxStyler styler;

        @BeforeEach
        void setUpStyler() {
            styler = lang.styler();
        }

        @Test
        void 行コメントにCOMMENT_Faceが適用される() {
            var spans = styler.styleLine("// this is a comment");
            var commentSpans = spans.select(s -> s.faceName().equals(FaceName.COMMENT));
            assertFalse(commentSpans.isEmpty());
        }

        @Test
        void 文字列リテラルにSTRING_Faceが適用される() {
            var spans = styler.styleLine("const name = \"hello world\";");
            var stringSpans = spans.select(s -> s.faceName().equals(FaceName.STRING));
            assertFalse(stringSpans.isEmpty());
        }

        @Test
        void 数値リテラルにNUMBER_Faceが適用される() {
            var spans = styler.styleLine("const count = 42;");
            var numSpans = spans.select(s -> s.faceName().equals(FaceName.NUMBER));
            assertFalse(numSpans.isEmpty());
        }

        @Test
        void typeキーワードにKEYWORD_Faceが適用される() {
            // type は TypeScript 固有のキーワード
            var spans = styler.styleLine("type Foo = string;");
            var keywordSpans = spans.select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(keywordSpans.isEmpty());
        }

        @Test
        void interfaceキーワードにKEYWORD_Faceが適用される() {
            var spans = styler.styleLine("interface Foo {}");
            var keywordSpans = spans.select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(keywordSpans.isEmpty());
        }

        @Test
        void 大文字始まりの型識別子にTYPE_Faceが適用される() {
            // tree-sitter-typescript の highlights.scm は ^[A-Z] にマッチする識別子を @type にする
            var spans = styler.styleLine("const x: MyType = null;");
            var typeSpans = spans.select(s -> s.faceName().equals(FaceName.TYPE));
            assertFalse(typeSpans.isEmpty());
        }

        @Test
        void 組み込み型にBUILTIN_Faceが適用される() {
            // predefined_type (number / string / boolean 等) は @type.builtin → BUILTIN
            var spans = styler.styleLine("const n: number = 0;");
            var builtinSpans = spans.select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertFalse(builtinSpans.isEmpty());
        }

        @Test
        void function宣言の関数名にFUNCTION_NAME_Faceが適用される() {
            // JavaScript の highlights.scm 継承により function 宣言の name が @function
            var spans = styler.styleLine("function greet(name: string) {}");
            var funcSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertFalse(funcSpans.isEmpty());
        }
    }

    @Nested
    class オートインデント {

        private Window createWindow(String text) {
            var buffer = new TextBuffer("test.ts", new GapTextModel(), new SettingsRegistry());
            var facade = new BufferFacade(buffer);
            var window = new Window(facade);
            if (!text.isEmpty()) {
                window.insert(text);
            }
            return window;
        }

        private CStyleIndentState createState() {
            // 本番コードの INDENT_CONFIG を共有してテストで再定義しない
            return new CStyleIndentState(TypeScriptMode.INDENT_CONFIG, lang.analyzer());
        }

        @Test
        void 関数開き波括弧の後にインデントが増加する() {
            var window = createWindow("function foo() {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("function foo() {\n  ", window.getBuffer().getText());
        }

        @Test
        void インターフェイス開き波括弧の後にインデントが増加する() {
            var window = createWindow("interface Foo {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("interface Foo {\n  ", window.getBuffer().getText());
        }

        @Test
        void 配列リテラル開き角括弧の後にインデントが増加する() {
            var window = createWindow("const xs = [");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("const xs = [\n  ", window.getBuffer().getText());
        }

        @Test
        void 関数引数開き丸括弧の後にインデントが増加する() {
            var window = createWindow("foo(");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("foo(\n  ", window.getBuffer().getText());
        }

        @Test
        void 通常の文の後はインデントが継承される() {
            var window = createWindow("  const x = 42;");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("  const x = 42;\n  ", window.getBuffer().getText());
        }
    }
}
