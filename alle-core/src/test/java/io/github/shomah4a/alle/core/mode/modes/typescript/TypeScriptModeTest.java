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
import org.eclipse.collections.api.factory.Lists;
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

        @Test
        void enumキーワードにKEYWORD_Faceが適用される() {
            var spans = styler.styleLine("enum Color {}");
            var keywordSpans = spans.select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(keywordSpans.isEmpty());
        }

        @Test
        void テンプレートリテラルにSTRING_Faceが適用される() {
            // template_string ノードは JavaScript の highlights.scm で @string にキャプチャされる
            var spans = styler.styleLine("const greeting = `hello world`;");
            var stringSpans = spans.select(s -> s.faceName().equals(FaceName.STRING));
            assertFalse(stringSpans.isEmpty());
        }

        @Test
        void ジェネリクス型パラメータを含む関数の関数名がハイライトされる() {
            // ジェネリクス <T> がパースされても他箇所のハイライトが壊れないことを確認
            var spans = styler.styleLine("function identity<T>(x: T): T { return x; }");
            var funcSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertFalse(funcSpans.isEmpty());
        }

        @Test
        void 関数パラメータにVARIABLE_Faceが適用される() {
            // tree-sitter-typescript は (required_parameter (identifier) @variable.parameter)
            // を定義しており、DefaultCaptureMapping で VARIABLE にマップされる
            var spans = styler.styleLine("function greet(name: string) {}");
            var varSpans = spans.select(s -> s.faceName().equals(FaceName.VARIABLE));
            assertFalse(varSpans.isEmpty());
        }

        @Test
        void 複数行のインターフェース宣言でも各キャプチャが行ごとに適用される() {
            var lines = Lists.immutable.of("interface User {", "  id: number;", "  name: string;", "}");
            var result = styler.styleDocument(lines);
            assertEquals(4, result.size());
            // 1 行目: interface キーワードが KEYWORD
            var line0Keywords = result.get(0).select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(line0Keywords.isEmpty(), "1 行目に KEYWORD (interface)");
            // 2 行目: number 組み込み型が BUILTIN
            var line1Builtin = result.get(1).select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertFalse(line1Builtin.isEmpty(), "2 行目に BUILTIN (number)");
            // 3 行目: string 組み込み型が BUILTIN
            var line2Builtin = result.get(2).select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertFalse(line2Builtin.isEmpty(), "3 行目に BUILTIN (string)");
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

        @Test
        void enum宣言の開き波括弧後にインデントが増加する() {
            var window = createWindow("enum Color {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("enum Color {\n  ", window.getBuffer().getText());
        }

        @Test
        void object型エイリアスの開き波括弧後にインデントが増加する() {
            // type Point = { ... } の `{` (object_type ノード)
            var window = createWindow("type Point = {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("type Point = {\n  ", window.getBuffer().getText());
        }

        @Test
        void tuple型の開き角括弧後にインデントが増加する() {
            // type Pair = [A, B] の `[` (tuple_type ノード)
            var window = createWindow("type Pair = [");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("type Pair = [\n  ", window.getBuffer().getText());
        }

        @Test
        void インターフェース本体の複数行入力で前行と同じインデントが入る() {
            // 前行 `  id: number;` は `;` で終わり、文字ベース判定では +indent されない
            // 文字ベースのフォールバックではインデント継承のみが起きる
            var window = createWindow("interface User {\n  id: number;");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals(
                    "interface User {\n  id: number;\n  ", window.getBuffer().getText());
        }

        @Test
        void enum本体の複数行入力で前行と同じインデントが入る() {
            var window = createWindow("enum Color {\n  RED,");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("enum Color {\n  RED,\n  ", window.getBuffer().getText());
        }
    }
}
