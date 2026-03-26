package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompletionsModelTest {

    private static CompletionCandidate t(String value) {
        return CompletionCandidate.terminal(value);
    }

    @Nested
    class コンストラクタ {

        @Test
        void 空の候補リストでIllegalArgumentExceptionをスローする() {
            assertThrows(IllegalArgumentException.class, () -> new CompletionsModel(Lists.immutable.empty()));
        }
    }

    @Nested
    class selectNext {

        @Test
        void 未選択状態から先頭を選択する() {
            var model = new CompletionsModel(Lists.immutable.of(t("aaa"), t("bbb"), t("ccc")));
            model.selectNext();
            assertEquals(0, model.getSelectedIndex());
            assertEquals(t("aaa"), model.getSelectedCandidate());
        }

        @Test
        void 末尾から先頭に戻る() {
            var model = new CompletionsModel(Lists.immutable.of(t("aaa"), t("bbb")));
            model.selectNext(); // 0
            model.selectNext(); // 1
            model.selectNext(); // 0 (wrap)
            assertEquals(0, model.getSelectedIndex());
            assertEquals(t("aaa"), model.getSelectedCandidate());
        }

        @Test
        void 順次選択が進む() {
            var model = new CompletionsModel(Lists.immutable.of(t("aaa"), t("bbb"), t("ccc")));
            model.selectNext(); // 0
            model.selectNext(); // 1
            assertEquals(1, model.getSelectedIndex());
            assertEquals(t("bbb"), model.getSelectedCandidate());
        }
    }

    @Nested
    class selectPrevious {

        @Test
        void 未選択状態から末尾を選択する() {
            var model = new CompletionsModel(Lists.immutable.of(t("aaa"), t("bbb"), t("ccc")));
            model.selectPrevious();
            assertEquals(2, model.getSelectedIndex());
            assertEquals(t("ccc"), model.getSelectedCandidate());
        }

        @Test
        void 先頭から末尾に戻る() {
            var model = new CompletionsModel(Lists.immutable.of(t("aaa"), t("bbb"), t("ccc")));
            model.selectNext(); // 0
            model.selectPrevious(); // 2 (wrap)
            assertEquals(2, model.getSelectedIndex());
            assertEquals(t("ccc"), model.getSelectedCandidate());
        }
    }

    @Nested
    class getSelectedCandidate {

        @Test
        void 未選択状態でnullを返す() {
            var model = new CompletionsModel(Lists.immutable.of(t("aaa")));
            assertNull(model.getSelectedCandidate());
        }
    }

    @Nested
    class formatForDisplay {

        @Test
        void 未選択状態ですべてインデント付きで表示する() {
            var model = new CompletionsModel(Lists.immutable.of(t("aaa"), t("bbb")));
            assertEquals("  aaa\n  bbb", model.formatForDisplay());
        }

        @Test
        void 選択中の候補にマーカーを表示する() {
            var model = new CompletionsModel(Lists.immutable.of(t("aaa"), t("bbb"), t("ccc")));
            model.selectNext(); // 0
            model.selectNext(); // 1
            assertEquals("  aaa\n> bbb\n  ccc", model.formatForDisplay());
        }

        @Test
        void 候補が1件の場合() {
            var model = new CompletionsModel(Lists.immutable.of(t("only")));
            model.selectNext();
            assertEquals("> only", model.formatForDisplay());
        }
    }
}
