package io.github.shomah4a.alle.core.mode.modes.java;

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
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JavaModeTest {

    private LanguageSupport lang;
    private JavaMode mode;

    @BeforeEach
    void setUp() {
        // テストごとに独立した LanguageSupport を生成する。
        // TreeSitterSession/Analyzer/Styler はキャッシュ状態を持つため、
        // テスト間で共有するとテスト順序に依存したフレーキー挙動になりうる。
        lang = SyntaxAnalyzerRegistry.createWithBuiltins().create("java").orElseThrow();
        mode = new JavaMode(lang);
    }

    @Test
    void モード名がjavaである() {
        assertEquals("java", mode.name());
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
    void インデント幅のデフォルトが4である() {
        assertEquals(4, mode.settingDefaults().get(EditorSettings.INDENT_WIDTH).orElseThrow());
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
            var spans = styler.styleLine("String s = \"hello world\";");
            var stringSpans = spans.select(s -> s.faceName().equals(FaceName.STRING));
            assertFalse(stringSpans.isEmpty());
        }

        @Test
        void 数値リテラルにNUMBER_Faceが適用される() {
            var spans = styler.styleLine("int count = 42;");
            var numSpans = spans.select(s -> s.faceName().equals(FaceName.NUMBER));
            assertFalse(numSpans.isEmpty());
        }

        @Test
        void publicキーワードにKEYWORD_Faceが適用される() {
            var spans = styler.styleLine("public class Foo {}");
            var keywordSpans = spans.select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(keywordSpans.isEmpty());
        }

        @Test
        void classキーワードにKEYWORD_Faceが適用される() {
            var spans = styler.styleLine("public class Foo {}");
            // "public" と "class" の2つの KEYWORD スパンが存在するはず
            var keywordSpans = spans.select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertEquals(2, keywordSpans.size());
        }

        @Test
        void クラス宣言の名前にTYPE_Faceが適用される() {
            var spans = styler.styleLine("public class Foo {}");
            var typeSpans = spans.select(s -> s.faceName().equals(FaceName.TYPE));
            assertFalse(typeSpans.isEmpty());
        }

        @Test
        void 組み込み型にBUILTIN_Faceが適用される() {
            // integral_type (int 等) は @type.builtin -> BUILTIN
            var spans = styler.styleLine("int n = 0;");
            var builtinSpans = spans.select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertFalse(builtinSpans.isEmpty());
        }

        @Test
        void メソッド宣言の名前にFUNCTION_NAME_Faceが適用される() {
            var spans = styler.styleLine("void greet(String name) {}");
            var funcSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertFalse(funcSpans.isEmpty());
        }

        @Test
        void メソッド呼び出しの名前にFUNCTION_NAME_Faceが適用される() {
            var spans = styler.styleLine("foo.bar();");
            var funcSpans = spans.select(s -> s.faceName().equals(FaceName.FUNCTION_NAME));
            assertFalse(funcSpans.isEmpty());
        }

        @Test
        void 文字列エスケープシーケンスにSTRING_Faceが適用される() {
            // escape_sequence は @string.escape -> STRING (DefaultCaptureMapping に追加したマッピング)
            var spans = styler.styleLine("String s = \"a\\nb\";");
            var stringSpans = spans.select(s -> s.faceName().equals(FaceName.STRING));
            assertFalse(stringSpans.isEmpty());
        }

        @Test
        void マーカーアノテーションにANNOTATION_Faceが適用される() {
            // @Override は marker_annotation -> @attribute -> ANNOTATION
            var spans = styler.styleLine("@Override");
            var annotationSpans = spans.select(s -> s.faceName().equals(FaceName.ANNOTATION));
            assertFalse(annotationSpans.isEmpty());
        }

        @Test
        void 引数付きアノテーションにANNOTATION_Faceが適用される() {
            // @SuppressWarnings("unchecked") は annotation -> @attribute -> ANNOTATION
            var spans = styler.styleLine("@SuppressWarnings(\"unchecked\")");
            var annotationSpans = spans.select(s -> s.faceName().equals(FaceName.ANNOTATION));
            assertFalse(annotationSpans.isEmpty());
        }

        @Test
        void 複数行のクラス宣言でも各キャプチャが行ごとに適用される() {
            var lines = Lists.immutable.of("class User {", "    private String name;", "    private int age;", "}");
            var result = styler.styleDocument(lines);
            assertEquals(4, result.size());
            // 1 行目: class キーワードが KEYWORD
            var line0Keywords = result.get(0).select(s -> s.faceName().equals(FaceName.KEYWORD));
            assertFalse(line0Keywords.isEmpty(), "1 行目に KEYWORD (class)");
            // 2 行目: String 型識別子が TYPE
            var line1Types = result.get(1).select(s -> s.faceName().equals(FaceName.TYPE));
            assertFalse(line1Types.isEmpty(), "2 行目に TYPE (String)");
            // 3 行目: int 組み込み型が BUILTIN
            var line2Builtin = result.get(2).select(s -> s.faceName().equals(FaceName.BUILTIN));
            assertFalse(line2Builtin.isEmpty(), "3 行目に BUILTIN (int)");
        }

        /**
         * characterization テスト: {@code #match?} プレディケート未評価の影響確認（ADR 0142 参照）。
         *
         * <p>tree-sitter-java の highlights.scm には
         * {@code ((method_invocation object: (identifier) @type) (#match? @type "^[A-Z]"))} という
         * パターンが存在するが、{@code TreeSitterStyler} は {@code #match?} を評価しない。
         * 実際に実行して観測した挙動: レシーバの大文字・小文字によらず、より汎用的な
         * {@code (identifier) @variable} パターンが優先され、レシーバは常に VARIABLE face になる
         * （TYPE face に誤って昇格することはない）。
         */
        @Test
        void 小文字レシーバのメソッド呼び出しでレシーバがVARIABLE_Faceになる() {
            var spans = styler.styleLine("foo.bar();");
            var receiverSpan = spans.detect(s -> s.start() == 0 && s.end() == 3);
            assertEquals(FaceName.VARIABLE, receiverSpan.faceName());
        }

        @Test
        void 大文字始まりレシーバのメソッド呼び出しでもレシーバがVARIABLE_Faceになる() {
            // #match? が "^[A-Z]" を要求する @type パターンは、プレディケート未評価により
            // 常に@variableパターンに敗れるため、大文字始まりでも TYPE には昇格しない。
            var spans = styler.styleLine("Foo.bar();");
            var receiverSpan = spans.detect(s -> s.start() == 0 && s.end() == 3);
            assertEquals(FaceName.VARIABLE, receiverSpan.faceName());
        }

        @Test
        void 数百行規模のJavaソースをスタイリングしても完走する() {
            // bonede/tree-sitter-ng Issue #64 (クエリマッチ反復中の SIGSEGV 報告) の検証。
            // 本テストが完走すること自体が確認事項であり、クラッシュすればテストプロセスごと落ちる。
            var lines = buildLargeJavaSource(300);
            var result = styler.styleDocument(lines);
            assertEquals(lines.size(), result.size());
            // 何らかのハイライトが生成されていることも確認する（空振りで完走しただけではない保証）
            long totalSpans = result.sumOfInt(spans -> spans.size());
            assertTrue(totalSpans > 0, "スタイリング結果に何らかのスパンが含まれる");
        }

        private org.eclipse.collections.api.list.ListIterable<String> buildLargeJavaSource(int methodCount) {
            MutableList<String> lines = Lists.mutable.empty();
            lines.add("package io.github.shomah4a.alle.core.mode.modes.java;");
            lines.add("");
            lines.add("import java.util.List;");
            lines.add("");
            lines.add("public class GeneratedSample {");
            lines.add("    private final List<String> names;");
            lines.add("");
            lines.add("    public GeneratedSample(List<String> names) {");
            lines.add("        this.names = names;");
            lines.add("    }");
            lines.add("");
            for (int i = 0; i < methodCount; i++) {
                lines.add("    @Override");
                lines.add("    public String method" + i + "(int value) {");
                lines.add("        if (value > " + i + ") {");
                lines.add("            String message = \"value is \" + value + \"\\n\";");
                lines.add("            return message.trim();");
                lines.add("        } else {");
                lines.add("            return \"default-" + i + "\";");
                lines.add("        }");
                lines.add("    }");
                lines.add("");
            }
            lines.add("}");
            return lines.toImmutable();
        }
    }

    @Nested
    class オートインデント {

        private Window createWindow(String text) {
            var buffer = new TextBuffer("test.java", new GapTextModel(), new SettingsRegistry());
            var facade = new BufferFacade(buffer);
            var window = new Window(facade);
            if (!text.isEmpty()) {
                window.insert(text);
            }
            return window;
        }

        private CStyleIndentState createState() {
            // 本番コードの INDENT_CONFIG を共有してテストで再定義しない
            return new CStyleIndentState(JavaMode.INDENT_CONFIG, lang.analyzer());
        }

        @Test
        void クラス開き波括弧の後にインデントが増加する() {
            var window = createWindow("public class Foo {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("public class Foo {\n    ", window.getBuffer().getText());
        }

        @Test
        void メソッド開き波括弧の後にインデントが増加する() {
            var window = createWindow("void foo() {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("void foo() {\n    ", window.getBuffer().getText());
        }

        @Test
        void インターフェース開き波括弧の後にインデントが増加する() {
            var window = createWindow("interface Foo {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("interface Foo {\n    ", window.getBuffer().getText());
        }

        @Test
        void enum開き波括弧の後にインデントが増加する() {
            var window = createWindow("enum Color {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("enum Color {\n    ", window.getBuffer().getText());
        }

        @Test
        void 配列初期化子開き波括弧の後にインデントが増加する() {
            var window = createWindow("int[] xs = {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("int[] xs = {\n    ", window.getBuffer().getText());
        }

        @Test
        void メソッド引数開き丸括弧の後にインデントが増加する() {
            var window = createWindow("foo(");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("foo(\n    ", window.getBuffer().getText());
        }

        @Test
        void 通常の文の後はインデントが継承される() {
            var window = createWindow("    int x = 42;");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("    int x = 42;\n    ", window.getBuffer().getText());
        }

        @Test
        void クラス本体の複数行入力で前行と同じインデントが入る() {
            // 前行 `    int age;` は `;` で終わり、文字ベース判定では +indent されない
            // 文字ベースのフォールバックではインデント継承のみが起きる
            var window = createWindow("class User {\n    int age;");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("class User {\n    int age;\n    ", window.getBuffer().getText());
        }

        @Test
        void 行末に行コメントがある開き波括弧の後でも改行時にコメント開始カラムへ整列せずインデント幅で整列する() {
            // 修正前は line_comment ノードがコメントとして認識されず、新行がコメント開始カラム
            // (12) に整列してしまっていた（実装安全性評価 MEDIUM 指摘）。修正後は他の言語と同様に
            // インデント幅 (4) で整列する。
            var window = createWindow("class Foo { // comment\n}");
            window.setPoint("class Foo { // comment".length());
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("class Foo { // comment\n    \n}", window.getBuffer().getText());
        }

        @Test
        void 行コメントを含まない開き波括弧の後の改行時のインデント幅整列は行コメントがある場合と変わらない() {
            var window = createWindow("class Foo {\n}");
            window.setPoint("class Foo {".length());
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("class Foo {\n    \n}", window.getBuffer().getText());
        }

        @Test
        void 行末に行コメントがある開き波括弧の次行でTabを押すとコメント開始カラムではなくインデント幅に整列する() {
            // 修正前はサイクル候補にコメント開始カラム (11) が混入し、その値に整列していた。
            var window = createWindow("void f() { // comment\nint x;");
            window.setPoint("void f() { // comment\n".length());
            var state = createState();
            state.cycleIndent(window, 1);
            assertEquals("void f() { // comment\n    int x;", window.getBuffer().getText());
        }

        @Test
        void アノテーション引数配列の開き波括弧の次行が最初の要素のカラムに整列する() {
            // element_value_array_initializer が JAVA_BRACKET_TYPES に登録されていることの機能検証。
            // enclosingBracket が element_value_array_initializer ({8,9}-{1,1}) を返すことで
            // findFirstContentChild が同一行内の最初の要素 (ElementType.METHOD, カラム9) を見つけ、
            // そのカラムに継続行が整列する。登録がないと enclosingBracket は
            // 親の annotation_argument_list まで遡り、その最初の子である
            // element_value_array_initializer 自体の開始カラム (8) に整列してしまい、値がずれる。
            var window = createWindow("@Target({ElementType.METHOD,\n})");
            window.setPoint("@Target({ElementType.METHOD,".length());
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals(
                    "@Target({ElementType.METHOD,\n         \n})",
                    window.getBuffer().getText());
        }

        @Test
        void module本体の開き波括弧の次行が同一行内の最初の要素のカラムに整列する() {
            // module_body が JAVA_BRACKET_TYPES に登録されていることの機能検証。
            // requires 宣言が module_body の開き波括弧と同一行にあるため、
            // findFirstContentChild 経由でその開始カラム (17) に継続行が整列する。
            // 登録がないと enclosingBracket は module_body の祖先を辿っても括弧系ノードに
            // 到達できず（module_declaration や program は括弧系ノードではない）、
            // 文字ベースのフォールバック (isOpenBracketBeforeColumn) に落ちる。
            // 行末の直前トークンは ";" であり開き括弧ではないため、
            // フォールバックではインデントが増加せず0カラムになり、値がずれる。
            var window = createWindow("module foo.bar { requires baz.qux;\n}");
            window.setPoint("module foo.bar { requires baz.qux;".length());
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals(
                    "module foo.bar { requires baz.qux;\n                 \n}",
                    window.getBuffer().getText());
        }
    }
}
