package io.github.shomah4a.alle.core.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Nested
    class グローバル値 {

        @Test
        void グローバル値を設定して取得できる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);
            registry.register(setting);
            registry.setGlobal(setting, 8);

            var result = registry.getGlobal(setting);

            assertTrue(result.isPresent());
            assertEquals(8, result.get());
        }

        @Test
        void グローバル値が未設定の場合emptyを返す() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);
            registry.register(setting);

            var result = registry.getGlobal(setting);

            assertTrue(result.isEmpty());
        }

        @Test
        void 未登録の設定にグローバル値を設定すると例外を投げる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);

            assertThrows(IllegalArgumentException.class, () -> registry.setGlobal(setting, 8));
        }

        @Test
        void 未登録の設定のグローバル値取得は例外を投げる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);

            assertThrows(IllegalArgumentException.class, () -> registry.getGlobal(setting));
        }

        @Test
        void グローバル値を設定するとhasGlobalがtrueを返す() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);
            registry.register(setting);
            registry.setGlobal(setting, 8);

            assertTrue(registry.hasGlobal(setting));
        }

        @Test
        void グローバル値が未設定の場合hasGlobalがfalseを返す() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);
            registry.register(setting);

            assertFalse(registry.hasGlobal(setting));
        }

        @Test
        void グローバル値を解除するとemptyに戻る() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);
            registry.register(setting);
            registry.setGlobal(setting, 8);
            registry.removeGlobal(setting);

            assertTrue(registry.getGlobal(setting).isEmpty());
            assertFalse(registry.hasGlobal(setting));
        }

        @Test
        void グローバル値はデフォルト値に影響しない() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("tab-width", Integer.class, 4);
            registry.register(setting);
            registry.setGlobal(setting, 8);

            assertEquals(4, registry.getDefault(setting));
        }
    }

    @Nested
    class 文字列キーアクセス {

        @Test
        void setGlobalByKeyで値を設定しgetEffectiveByKeyで取得できる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("completion-ignore-case", Boolean.class, false);
            registry.register(setting);

            registry.setGlobalByKey("completion-ignore-case", true);

            assertEquals(true, registry.getEffectiveByKey("completion-ignore-case"));
        }

        @Test
        void getEffectiveByKeyはグローバル未設定ならデフォルト値を返す() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("completion-ignore-case", Boolean.class, false);
            registry.register(setting);

            assertEquals(false, registry.getEffectiveByKey("completion-ignore-case"));
        }

        @Test
        void setGlobalByKeyは未登録キーで例外を投げる() {
            var registry = new SettingsRegistry();

            assertThrows(IllegalArgumentException.class, () -> registry.setGlobalByKey("unknown", true));
        }

        @Test
        void getEffectiveByKeyは未登録キーで例外を投げる() {
            var registry = new SettingsRegistry();

            assertThrows(IllegalArgumentException.class, () -> registry.getEffectiveByKey("unknown"));
        }

        @Test
        void setGlobalByKeyは型不一致で例外を投げる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("completion-ignore-case", Boolean.class, false);
            registry.register(setting);

            assertThrows(ClassCastException.class, () -> registry.setGlobalByKey("completion-ignore-case", "yes"));
        }

        @Test
        void removeGlobalByKeyで値を解除できる() {
            var registry = new SettingsRegistry();
            var setting = Setting.of("completion-ignore-case", Boolean.class, false);
            registry.register(setting);
            registry.setGlobalByKey("completion-ignore-case", true);

            registry.removeGlobalByKey("completion-ignore-case");

            assertEquals(false, registry.getEffectiveByKey("completion-ignore-case"));
        }

        @Test
        void removeGlobalByKeyは未登録キーで例外を投げる() {
            var registry = new SettingsRegistry();

            assertThrows(IllegalArgumentException.class, () -> registry.removeGlobalByKey("unknown"));
        }
    }
}
