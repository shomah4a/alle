package io.github.shomah4a.alle.core.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModeSettingsTest {

    private static final Setting<Integer> TAB_WIDTH = Setting.of("tab-width", Integer.class, 4);
    private static final Setting<Boolean> TRUNCATE_LINES = Setting.of("truncate-lines", Boolean.class, true);

    @Nested
    class 空のModeSettings {

        @Test
        void emptyは設定を含まない() {
            var settings = ModeSettings.empty();

            assertTrue(settings.get(TAB_WIDTH).isEmpty());
            assertFalse(settings.contains(TAB_WIDTH));
        }

        @Test
        void ビルダーで何も設定しない場合もemptyと同等() {
            var settings = ModeSettings.builder().build();

            assertTrue(settings.get(TAB_WIDTH).isEmpty());
        }
    }

    @Nested
    class 値の取得 {

        @Test
        void ビルダーで設定した値を取得できる() {
            var settings = ModeSettings.builder().set(TAB_WIDTH, 8).build();

            var result = settings.get(TAB_WIDTH);

            assertTrue(result.isPresent());
            assertEquals(8, result.get());
        }

        @Test
        void 設定されていないキーはemptyを返す() {
            var settings = ModeSettings.builder().set(TAB_WIDTH, 8).build();

            assertTrue(settings.get(TRUNCATE_LINES).isEmpty());
        }

        @Test
        void 複数の設定を保持できる() {
            var settings = ModeSettings.builder()
                    .set(TAB_WIDTH, 2)
                    .set(TRUNCATE_LINES, false)
                    .build();

            assertEquals(2, settings.get(TAB_WIDTH).get());
            assertEquals(false, settings.get(TRUNCATE_LINES).get());
        }
    }

    @Nested
    class contains確認 {

        @Test
        void 設定が含まれている場合trueを返す() {
            var settings = ModeSettings.builder().set(TAB_WIDTH, 8).build();

            assertTrue(settings.contains(TAB_WIDTH));
        }

        @Test
        void 設定が含まれていない場合falseを返す() {
            var settings = ModeSettings.builder().set(TAB_WIDTH, 8).build();

            assertFalse(settings.contains(TRUNCATE_LINES));
        }
    }
}
