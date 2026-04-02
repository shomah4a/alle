package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.syntax.TreeSitterSession;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.treesitter.TreeSitterJson;

class TreeSitterStylerJsonTest {

    private TreeSitterStyler styler;

    @BeforeEach
    void setUp() {
        String query = HighlightQueryLoader.load("json");
        var session = new TreeSitterSession(new TreeSitterJson());
        styler = new TreeSitterStyler(session, query, DefaultCaptureMapping.INSTANCE);
    }

    @Nested
    class JSONキーのハイライト {

        @Test
        void キーの文字列がSTRINGとしてハイライトされる() {
            // {"aaa": "bbb"} のキー "aaa" は文字列式なのでSTRINGであるべき
            var result = styler.styleDocument(Lists.immutable.of("{\"aaa\": \"bbb\"}"));
            var spans = result.get(0);

            // "aaa" は位置1〜6 ({"aaa"の部分)
            var keySpans = spans.select(s -> s.start() == 1 && s.end() == 6);
            assertTrue(keySpans.notEmpty(), "キー位置にスパンが存在する");

            // キーの位置にある式は文字列なので、STRINGとしてハイライトされるべき
            var stringSpan = keySpans.detect(s -> s.faceName().equals(FaceName.STRING));
            assertTrue(stringSpan != null, "キーの文字列がSTRINGとしてハイライトされる");
        }

        @Test
        void 値の文字列がSTRINGとしてハイライトされる() {
            var result = styler.styleDocument(Lists.immutable.of("{\"aaa\": \"bbb\"}"));
            var spans = result.get(0);

            // "bbb" は位置8〜13
            var valueSpans = spans.select(s -> s.start() == 8 && s.end() == 13);
            assertTrue(valueSpans.notEmpty(), "値の位置にスパンが存在する");
            assertTrue(valueSpans.anySatisfy(s -> s.faceName().equals(FaceName.STRING)), "値の文字列がSTRINGとしてハイライトされる");
        }
    }

    @Nested
    class JSONキーに生成されるスパンの調査 {

        @Test
        void キー位置にはSTRINGスパンのみが生成される() {
            var result = styler.styleDocument(Lists.immutable.of("{\"aaa\": \"bbb\"}"));
            var spans = result.get(0);

            // キー "aaa" の範囲(1-6)のスパンを取得
            var keySpans = spans.select(s -> s.start() == 1 && s.end() == 6);
            assertEquals(1, keySpans.size(), "キー位置のスパンは1つだけ生成される");
            assertEquals(FaceName.STRING, keySpans.get(0).faceName());
        }
    }
}
