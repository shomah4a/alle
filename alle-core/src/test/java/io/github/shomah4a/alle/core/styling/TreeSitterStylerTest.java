package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TreeSitterPython;

class TreeSitterStylerTest {

    private TreeSitterStyler styler;

    @BeforeEach
    void setUp() {
        String query = HighlightQueryLoader.load("python");
        styler = new TreeSitterStyler(new TreeSitterPython(), query, PythonHighlightQuery.MAPPING);
    }

    @Nested
    class キーワードのハイライト {

        @Test
        void defキーワードがハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("def foo():"));
            var spans = result.get(0);

            var defSpan = findSpan(spans, 0, 3);
            assertTrue(defSpan.isPresent(), "defキーワードのスパンが存在する");
            assertEquals(FaceName.KEYWORD, defSpan.get().faceName());
        }

        @Test
        void ifキーワードがハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("if x > 0:"));
            var spans = result.get(0);

            var ifSpan = findSpan(spans, 0, 2);
            assertTrue(ifSpan.isPresent(), "ifキーワードのスパンが存在する");
            assertEquals(FaceName.KEYWORD, ifSpan.get().faceName());
        }
    }

    @Nested
    class 文字列のハイライト {

        @Test
        void 文字列リテラルがハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("x = \"hello\""));
            var spans = result.get(0);

            assertTrue(spans.anySatisfy(s -> s.faceName().equals(FaceName.STRING)), "文字列スパンが存在する");
        }

        @Test
        void 複数行文字列がハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("x = \"\"\"", "multi", "line", "\"\"\""));

            // 全行に文字列スパンが存在する
            for (int i = 0; i < result.size(); i++) {
                assertTrue(
                        result.get(i).anySatisfy(s -> s.faceName().equals(FaceName.STRING)),
                        "行 " + i + " に文字列スパンが存在する");
            }
        }
    }

    @Nested
    class コメントのハイライト {

        @Test
        void コメントがハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("# this is a comment"));
            var spans = result.get(0);

            assertTrue(spans.anySatisfy(s -> s.faceName().equals(FaceName.COMMENT)), "コメントスパンが存在する");
        }
    }

    @Nested
    class 関数名のハイライト {

        @Test
        void 関数定義の名前がハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("def my_func():"));
            var spans = result.get(0);

            // "my_func" は位置4〜11
            var funcSpan = findSpan(spans, 4, 11);
            assertTrue(funcSpan.isPresent(), "関数名のスパンが存在する");
            assertEquals(FaceName.FUNCTION_NAME, funcSpan.get().faceName());
        }

        @Test
        void 関数呼び出しの名前がハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("print(x)"));
            var spans = result.get(0);

            var printSpan = findSpan(spans, 0, 5);
            assertTrue(printSpan.isPresent(), "関数呼び出しのスパンが存在する");
            assertEquals(FaceName.FUNCTION_NAME, printSpan.get().faceName());
        }
    }

    @Nested
    class 変数名のハイライト {

        @Test
        void 変数名がハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("x = 42"));
            var spans = result.get(0);

            var varSpan = findSpan(spans, 0, 1);
            assertTrue(varSpan.isPresent(), "変数名のスパンが存在する");
            assertEquals(FaceName.VARIABLE, varSpan.get().faceName());
        }
    }

    @Nested
    class 数値のハイライト {

        @Test
        void 整数がハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("x = 42"));
            var spans = result.get(0);

            var numSpan = findSpan(spans, 4, 6);
            assertTrue(numSpan.isPresent(), "数値スパンが存在する");
            assertEquals(FaceName.NUMBER, numSpan.get().faceName());
        }
    }

    @Nested
    class デコレータのハイライト {

        @Test
        void デコレータがハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("@staticmethod", "def foo():"));
            var spans = result.get(0);

            // 公式 highlights.scm ではデコレータは @function としてキャプチャされる
            assertTrue(spans.anySatisfy(s -> s.faceName().equals(FaceName.FUNCTION_NAME)), "デコレータスパンが存在する");
        }
    }

    @Nested
    class 組み込み定数のハイライト {

        @Test
        void TrueとFalseとNoneがハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("x = True", "y = False", "z = None"));

            assertTrue(result.get(0).anySatisfy(s -> s.faceName().equals(FaceName.BUILTIN)), "Trueがハイライトされる");
            assertTrue(result.get(1).anySatisfy(s -> s.faceName().equals(FaceName.BUILTIN)), "Falseがハイライトされる");
            assertTrue(result.get(2).anySatisfy(s -> s.faceName().equals(FaceName.BUILTIN)), "Noneがハイライトされる");
        }
    }

    @Nested
    class 空のドキュメント {

        @Test
        void 空リストでは空結果を返す() {
            var result = styler.styleDocument(Lists.immutable.empty());
            assertTrue(result.isEmpty());
        }

        @Test
        void 空行のドキュメントではスパンが生成されない() {
            var result = styler.styleDocument(Lists.immutable.of(""));
            assertEquals(1, result.size());
            assertTrue(result.get(0).isEmpty());
        }
    }

    @Nested
    class 日本語を含むコード {

        @Test
        void 日本語変数名の後の要素が正しいオフセットでハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("名前 = \"太郎\""));
            var spans = result.get(0);

            // 文字列 "太郎" のスパンが存在する
            assertTrue(spans.anySatisfy(s -> s.faceName().equals(FaceName.STRING)), "文字列スパンが存在する");

            // 変数名 名前 のスパンが存在する
            var varSpan = findSpan(spans, 0, 2);
            assertTrue(varSpan.isPresent(), "日本語変数名のスパンが存在する");
            assertEquals(FaceName.VARIABLE, varSpan.get().faceName());
        }
    }

    /**
     * 指定位置に一致するスパンを検索する。
     */
    private static java.util.Optional<StyledSpan> findSpan(ListIterable<StyledSpan> spans, int start, int end) {
        return spans.detectOptional(s -> s.start() == start && s.end() == end);
    }
}
