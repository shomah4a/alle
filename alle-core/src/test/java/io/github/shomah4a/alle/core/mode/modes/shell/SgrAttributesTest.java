package io.github.shomah4a.alle.core.mode.modes.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.styling.FaceName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SgrAttributesTest {

    private static FaceName requireFaceName(SgrAttributes attrs) {
        var face = attrs.toFaceName();
        assertNotNull(face);
        return face;
    }

    @Nested
    class reset {

        @Test
        void リセット状態はデフォルトである() {
            var attrs = SgrAttributes.reset();
            assertTrue(attrs.isDefault());
        }

        @Test
        void リセット状態のFaceNameはnullを返す() {
            var attrs = SgrAttributes.reset();
            assertNull(attrs.toFaceName());
        }
    }

    @Nested
    class withSgrCode {

        @Test
        void 前景色の赤を設定する() {
            var attrs = SgrAttributes.reset().withSgrCode(31);
            assertEquals("ansi-sgr:fg=red", requireFaceName(attrs).name());
        }

        @Test
        void 前景色の緑を設定する() {
            var attrs = SgrAttributes.reset().withSgrCode(32);
            assertEquals("ansi-sgr:fg=green", requireFaceName(attrs).name());
        }

        @Test
        void 明るい青の前景色を設定する() {
            var attrs = SgrAttributes.reset().withSgrCode(94);
            assertEquals("ansi-sgr:fg=blue_bright", requireFaceName(attrs).name());
        }

        @Test
        void 背景色を設定する() {
            var attrs = SgrAttributes.reset().withSgrCode(41);
            assertEquals("ansi-sgr:bg=red", requireFaceName(attrs).name());
        }

        @Test
        void 明るい背景色を設定する() {
            var attrs = SgrAttributes.reset().withSgrCode(102);
            assertEquals("ansi-sgr:bg=green_bright", requireFaceName(attrs).name());
        }

        @Test
        void boldを設定する() {
            var attrs = SgrAttributes.reset().withSgrCode(1);
            assertEquals("ansi-sgr:bold", requireFaceName(attrs).name());
        }

        @Test
        void underlineを設定する() {
            var attrs = SgrAttributes.reset().withSgrCode(4);
            assertEquals("ansi-sgr:underline", requireFaceName(attrs).name());
        }

        @Test
        void 前景色とboldの組み合わせ() {
            var attrs = SgrAttributes.reset().withSgrCode(1).withSgrCode(31);
            assertEquals("ansi-sgr:fg=red:bold", requireFaceName(attrs).name());
        }

        @Test
        void 前景色と背景色とboldの組み合わせ() {
            var attrs = SgrAttributes.reset().withSgrCode(1).withSgrCode(32).withSgrCode(44);
            assertEquals(
                    "ansi-sgr:fg=green:bg=blue:bold", requireFaceName(attrs).name());
        }

        @Test
        void コード0でリセットする() {
            var attrs = SgrAttributes.reset().withSgrCode(31).withSgrCode(1).withSgrCode(0);
            assertTrue(attrs.isDefault());
            assertNull(attrs.toFaceName());
        }

        @Test
        void コード22でboldのみ解除する() {
            var attrs = SgrAttributes.reset().withSgrCode(1).withSgrCode(31).withSgrCode(22);
            assertEquals("ansi-sgr:fg=red", requireFaceName(attrs).name());
        }

        @Test
        void コード24でunderlineのみ解除する() {
            var attrs = SgrAttributes.reset().withSgrCode(4).withSgrCode(31).withSgrCode(24);
            assertEquals("ansi-sgr:fg=red", requireFaceName(attrs).name());
        }

        @Test
        void コード39でデフォルト前景色にリセットする() {
            var attrs = SgrAttributes.reset().withSgrCode(31).withSgrCode(1).withSgrCode(39);
            assertEquals("ansi-sgr:bold", requireFaceName(attrs).name());
        }

        @Test
        void コード49でデフォルト背景色にリセットする() {
            var attrs = SgrAttributes.reset().withSgrCode(41).withSgrCode(49);
            assertTrue(attrs.isDefault());
        }

        @Test
        void 未知のコードは無視する() {
            var attrs = SgrAttributes.reset().withSgrCode(31).withSgrCode(999);
            assertEquals("ansi-sgr:fg=red", requireFaceName(attrs).name());
        }
    }
}
