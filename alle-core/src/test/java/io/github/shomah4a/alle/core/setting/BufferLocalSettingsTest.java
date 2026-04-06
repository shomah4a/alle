package io.github.shomah4a.alle.core.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferLocalSettingsTest {

    private static final Setting<Integer> TAB_WIDTH = Setting.of("tab-width", Integer.class, 4);
    private static final Setting<Boolean> TRUNCATE_LINES = Setting.of("truncate-lines", Boolean.class, true);

    private SettingsRegistry registry;
    private MajorMode majorMode;
    private MutableList<MinorMode> minorModes;

    private BufferLocalSettings createSettings() {
        return new BufferLocalSettings(registry, () -> majorMode, () -> minorModes);
    }

    @BeforeEach
    void setUp() {
        registry = new SettingsRegistry();
        registry.register(TAB_WIDTH);
        registry.register(TRUNCATE_LINES);
        majorMode = new TextMode();
        minorModes = Lists.mutable.empty();
    }

    @Nested
    class デフォルト値フォールバック {

        @Test
        void すべての層に値がない場合はSettingのデフォルト値を返す() {
            var local = createSettings();

            assertEquals(4, local.get(TAB_WIDTH));
            assertEquals(true, local.get(TRUNCATE_LINES));
        }
    }

    @Nested
    class ローカル値設定 {

        @Test
        void ローカル値を設定するとデフォルト値より優先される() {
            var local = createSettings();
            local.setLocal(TAB_WIDTH, 8);

            assertEquals(8, local.get(TAB_WIDTH));
        }

        @Test
        void ローカル値を設定しても他の設定には影響しない() {
            var local = createSettings();
            local.setLocal(TAB_WIDTH, 8);

            assertEquals(true, local.get(TRUNCATE_LINES));
        }

        @Test
        void 異なるBufferLocalSettingsインスタンスは独立している() {
            var local1 = createSettings();
            var local2 = createSettings();
            local1.setLocal(TAB_WIDTH, 8);

            assertEquals(8, local1.get(TAB_WIDTH));
            assertEquals(4, local2.get(TAB_WIDTH));
        }
    }

    @Nested
    class ローカル値解除 {

        @Test
        void ローカル値を解除するとデフォルト値にフォールバックする() {
            var local = createSettings();
            local.setLocal(TAB_WIDTH, 8);
            local.removeLocal(TAB_WIDTH);

            assertEquals(4, local.get(TAB_WIDTH));
        }
    }

    @Nested
    class ローカル値存在確認 {

        @Test
        void ローカル値が設定されている場合trueを返す() {
            var local = createSettings();
            local.setLocal(TAB_WIDTH, 8);

            assertTrue(local.hasLocal(TAB_WIDTH));
        }

        @Test
        void ローカル値が設定されていない場合falseを返す() {
            var local = createSettings();

            assertFalse(local.hasLocal(TAB_WIDTH));
        }
    }

    @Nested
    class 五層優先順位 {

        @Test
        void バッファローカルが全層より優先される() {
            var modeSettings = ModeSettings.builder().set(TAB_WIDTH, 2).build();
            majorMode = createMajorModeWithSettings(modeSettings);
            registry.setGlobal(TAB_WIDTH, 6);
            var local = createSettings();
            local.setLocal(TAB_WIDTH, 10);

            assertEquals(10, local.get(TAB_WIDTH));
        }

        @Test
        void マイナーモードデフォルトがメジャーモードデフォルトより優先される() {
            var majorSettings = ModeSettings.builder().set(TAB_WIDTH, 2).build();
            majorMode = createMajorModeWithSettings(majorSettings);
            var minorSettings = ModeSettings.builder().set(TAB_WIDTH, 3).build();
            minorModes.add(createMinorModeWithSettings("minor1", minorSettings));

            var local = createSettings();

            assertEquals(3, local.get(TAB_WIDTH));
        }

        @Test
        void 後から有効にしたマイナーモードが先に有効にしたものより優先される() {
            var settings1 = ModeSettings.builder().set(TAB_WIDTH, 3).build();
            var settings2 = ModeSettings.builder().set(TAB_WIDTH, 5).build();
            minorModes.add(createMinorModeWithSettings("minor1", settings1));
            minorModes.add(createMinorModeWithSettings("minor2", settings2));

            var local = createSettings();

            assertEquals(5, local.get(TAB_WIDTH));
        }

        @Test
        void メジャーモードデフォルトがエディタグローバルより優先される() {
            var modeSettings = ModeSettings.builder().set(TAB_WIDTH, 2).build();
            majorMode = createMajorModeWithSettings(modeSettings);
            registry.setGlobal(TAB_WIDTH, 6);

            var local = createSettings();

            assertEquals(2, local.get(TAB_WIDTH));
        }

        @Test
        void エディタグローバルがSettingデフォルトより優先される() {
            registry.setGlobal(TAB_WIDTH, 6);

            var local = createSettings();

            assertEquals(6, local.get(TAB_WIDTH));
        }

        @Test
        void マイナーモードに該当設定がなければメジャーモードにフォールバックする() {
            var majorSettings = ModeSettings.builder().set(TAB_WIDTH, 2).build();
            majorMode = createMajorModeWithSettings(majorSettings);
            var minorSettings =
                    ModeSettings.builder().set(TRUNCATE_LINES, false).build();
            minorModes.add(createMinorModeWithSettings("minor1", minorSettings));

            var local = createSettings();

            assertEquals(2, local.get(TAB_WIDTH));
        }

        @Test
        void モードにもグローバルにも値がなければSettingデフォルトを返す() {
            var local = createSettings();

            assertEquals(4, local.get(TAB_WIDTH));
        }
    }

    private static MajorMode createMajorModeWithSettings(ModeSettings settings) {
        return new MajorMode() {
            @Override
            public String name() {
                return "TestMajor";
            }

            @Override
            public Optional<Keymap> keymap() {
                return Optional.empty();
            }

            @Override
            public ModeSettings settingDefaults() {
                return settings;
            }
        };
    }

    private static MinorMode createMinorModeWithSettings(String name, ModeSettings settings) {
        return new MinorMode() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Optional<Keymap> keymap() {
                return Optional.empty();
            }

            @Override
            public ModeSettings settingDefaults() {
                return settings;
            }
        };
    }
}
