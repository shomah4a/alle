package io.github.shomah4a.alle.core.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferLocalSettingsTest {

    private static final Setting<Integer> TAB_WIDTH = Setting.of("tab-width", Integer.class, 4);
    private static final Setting<Boolean> TRUNCATE_LINES = Setting.of("truncate-lines", Boolean.class, true);

    private SettingsRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SettingsRegistry();
        registry.register(TAB_WIDTH);
        registry.register(TRUNCATE_LINES);
    }

    @Nested
    class デフォルト値フォールバック {

        @Test
        void ローカル値がない場合はデフォルト値を返す() {
            var local = new BufferLocalSettings(registry);

            assertEquals(4, local.get(TAB_WIDTH));
            assertEquals(true, local.get(TRUNCATE_LINES));
        }
    }

    @Nested
    class ローカル値設定 {

        @Test
        void ローカル値を設定するとデフォルト値より優先される() {
            var local = new BufferLocalSettings(registry);
            local.setLocal(TAB_WIDTH, 8);

            assertEquals(8, local.get(TAB_WIDTH));
        }

        @Test
        void ローカル値を設定しても他の設定には影響しない() {
            var local = new BufferLocalSettings(registry);
            local.setLocal(TAB_WIDTH, 8);

            assertEquals(true, local.get(TRUNCATE_LINES));
        }

        @Test
        void 異なるBufferLocalSettingsインスタンスは独立している() {
            var local1 = new BufferLocalSettings(registry);
            var local2 = new BufferLocalSettings(registry);
            local1.setLocal(TAB_WIDTH, 8);

            assertEquals(8, local1.get(TAB_WIDTH));
            assertEquals(4, local2.get(TAB_WIDTH));
        }
    }

    @Nested
    class ローカル値解除 {

        @Test
        void ローカル値を解除するとデフォルト値にフォールバックする() {
            var local = new BufferLocalSettings(registry);
            local.setLocal(TAB_WIDTH, 8);
            local.removeLocal(TAB_WIDTH);

            assertEquals(4, local.get(TAB_WIDTH));
        }
    }

    @Nested
    class ローカル値存在確認 {

        @Test
        void ローカル値が設定されている場合trueを返す() {
            var local = new BufferLocalSettings(registry);
            local.setLocal(TAB_WIDTH, 8);

            assertTrue(local.hasLocal(TAB_WIDTH));
        }

        @Test
        void ローカル値が設定されていない場合falseを返す() {
            var local = new BufferLocalSettings(registry);

            assertFalse(local.hasLocal(TAB_WIDTH));
        }
    }
}
