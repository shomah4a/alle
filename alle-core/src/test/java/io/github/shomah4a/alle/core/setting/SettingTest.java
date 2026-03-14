package io.github.shomah4a.alle.core.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SettingTest {

    @Nested
    class 設定定義 {

        @Test
        void キーと型とデフォルト値を保持する() {
            var setting = Setting.of("tab-width", Integer.class, 4);

            assertEquals("tab-width", setting.key());
            assertEquals(Integer.class, setting.type());
            assertEquals(4, setting.defaultValue());
        }

        @Test
        void ファクトリメソッドでBoolean設定を生成できる() {
            var setting = Setting.of("truncate-lines", Boolean.class, true);

            assertEquals(Boolean.class, setting.type());
            assertEquals(true, setting.defaultValue());
        }

        @Test
        void ファクトリメソッドでString設定を生成できる() {
            var setting = Setting.of("indent-style", String.class, "spaces");

            assertEquals(String.class, setting.type());
            assertEquals("spaces", setting.defaultValue());
        }
    }

    @Nested
    class 型キャスト {

        @Test
        void 互換型の値をキャストできる() {
            var setting = Setting.of("tab-width", Integer.class, 4);

            Integer result = setting.cast(8);

            assertEquals(8, result);
        }

        @Test
        void 非互換型の値はClassCastExceptionを投げる() {
            var setting = Setting.of("tab-width", Integer.class, 4);

            assertThrows(ClassCastException.class, () -> setting.cast("not-a-number"));
        }
    }
}
