package io.github.shomah4a.alle.core.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SettingsRegistryTest {

    @Nested
    class 設定登録 {

        @Test
        void 設定を登録して検索できる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);
            registry.register(setting);

            var result = registry.lookup("tab-width");

            assertTrue(result.isPresent());
            assertEquals("tab-width", result.get().key());
        }

        @Test
        void 同名設定の二重登録は例外を投げる() {
            var registry = new SettingsRegistry();
            registry.register(Setting.of("tab-width", Integer.class, 4));

            assertThrows(
                    IllegalStateException.class, () -> registry.register(Setting.of("tab-width", Integer.class, 8)));
        }

        @Test
        void 未登録の設定名を検索するとemptyを返す() {
            var registry = new SettingsRegistry();

            var result = registry.lookup("nonexistent");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class デフォルト値 {

        @Test
        void 登録済み設定のデフォルト値を取得できる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);
            registry.register(setting);

            assertEquals(4, registry.getDefault(setting));
        }

        @Test
        void 未登録の設定のデフォルト値取得は例外を投げる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);

            assertThrows(IllegalArgumentException.class, () -> registry.getDefault(setting));
        }
    }

    @Nested
    class 設定一覧 {

        @Test
        void 登録済み設定名の一覧を取得できる() {
            var registry = new SettingsRegistry();
            registry.register(Setting.of("tab-width", Integer.class, 4));
            registry.register(Setting.of("truncate-lines", Boolean.class, true));

            var keys = registry.registeredKeys();

            assertEquals(2, keys.size());
            assertTrue(keys.contains("tab-width"));
            assertTrue(keys.contains("truncate-lines"));
        }
    }
}
