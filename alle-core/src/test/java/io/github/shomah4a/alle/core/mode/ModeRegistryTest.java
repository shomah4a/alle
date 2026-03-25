package io.github.shomah4a.alle.core.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModeRegistryTest {

    private ModeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModeRegistry();
    }

    @Nested
    class メジャーモード {

        @Test
        void 登録したモードを名前で検索できる() {
            registry.registerMajorMode("test-mode", StubMajorMode::new);
            var result = registry.lookupMajorMode("test-mode");
            assertTrue(result.isPresent());
            assertEquals("StubMajor", result.get().get().name());
        }

        @Test
        void 未登録のモード名でemptyが返る() {
            var result = registry.lookupMajorMode("nonexistent");
            assertTrue(result.isEmpty());
        }

        @Test
        void 同名のモードを二重登録すると例外が発生する() {
            registry.registerMajorMode("test-mode", StubMajorMode::new);
            assertThrows(
                    IllegalStateException.class, () -> registry.registerMajorMode("test-mode", StubMajorMode::new));
        }

        @Test
        void registerOrReplaceで同名モードを上書きできる() {
            registry.registerMajorMode("test-mode", StubMajorMode::new);
            registry.registerOrReplaceMajorMode("test-mode", AnotherMajorMode::new);
            var result = registry.lookupMajorMode("test-mode");
            assertTrue(result.isPresent());
            assertEquals("AnotherMajor", result.get().get().name());
        }

        @Test
        void 登録済みモード名の一覧を取得できる() {
            registry.registerMajorMode("mode-a", StubMajorMode::new);
            registry.registerMajorMode("mode-b", AnotherMajorMode::new);
            var names = registry.registeredMajorModeNames();
            assertEquals(2, names.size());
            assertTrue(names.contains("mode-a"));
            assertTrue(names.contains("mode-b"));
        }

        @Test
        void 登録なしで空の一覧が返る() {
            var names = registry.registeredMajorModeNames();
            assertTrue(names.isEmpty());
        }
    }

    @Nested
    class マイナーモード {

        @Test
        void 登録したモードを名前で検索できる() {
            registry.registerMinorMode("test-minor", StubMinorMode::new);
            var result = registry.lookupMinorMode("test-minor");
            assertTrue(result.isPresent());
            assertEquals("StubMinor", result.get().get().name());
        }

        @Test
        void 未登録のモード名でemptyが返る() {
            var result = registry.lookupMinorMode("nonexistent");
            assertTrue(result.isEmpty());
        }

        @Test
        void 同名のモードを二重登録すると例外が発生する() {
            registry.registerMinorMode("test-minor", StubMinorMode::new);
            assertThrows(
                    IllegalStateException.class, () -> registry.registerMinorMode("test-minor", StubMinorMode::new));
        }

        @Test
        void registerOrReplaceで同名モードを上書きできる() {
            registry.registerMinorMode("test-minor", StubMinorMode::new);
            registry.registerOrReplaceMinorMode("test-minor", AnotherMinorMode::new);
            var result = registry.lookupMinorMode("test-minor");
            assertTrue(result.isPresent());
            assertEquals("AnotherMinor", result.get().get().name());
        }

        @Test
        void 登録済みモード名の一覧を取得できる() {
            registry.registerMinorMode("minor-a", StubMinorMode::new);
            registry.registerMinorMode("minor-b", AnotherMinorMode::new);
            var names = registry.registeredMinorModeNames();
            assertEquals(2, names.size());
            assertTrue(names.contains("minor-a"));
            assertTrue(names.contains("minor-b"));
        }
    }

    private static class StubMajorMode implements MajorMode {
        @Override
        public String name() {
            return "StubMajor";
        }

        @Override
        public Optional<Keymap> keymap() {
            return Optional.empty();
        }
    }

    private static class AnotherMajorMode implements MajorMode {
        @Override
        public String name() {
            return "AnotherMajor";
        }

        @Override
        public Optional<Keymap> keymap() {
            return Optional.empty();
        }
    }

    private static class StubMinorMode implements MinorMode {
        @Override
        public String name() {
            return "StubMinor";
        }

        @Override
        public Optional<Keymap> keymap() {
            return Optional.empty();
        }
    }

    private static class AnotherMinorMode implements MinorMode {
        @Override
        public String name() {
            return "AnotherMinor";
        }

        @Override
        public Optional<Keymap> keymap() {
            return Optional.empty();
        }
    }

    @Nested
    class コマンド自動登録 {

        @Test
        void メジャーモード登録時にモード切り替えコマンドが自動登録される() {
            var commandRegistry = new CommandRegistry();
            registry.setCommandRegistry(commandRegistry);
            registry.registerMajorMode("Python", StubMajorMode::new);
            assertTrue(commandRegistry.lookup("python-mode").isPresent());
        }

        @Test
        void マイナーモード登録時にトグルコマンドが自動登録される() {
            var commandRegistry = new CommandRegistry();
            registry.setCommandRegistry(commandRegistry);
            registry.registerMinorMode("ElectricPair", StubMinorMode::new);
            assertTrue(commandRegistry.lookup("electric-pair-mode").isPresent());
        }

        @Test
        void CommandRegistry未設定でもモード登録が正常に動作する() {
            registry.registerMajorMode("Test", StubMajorMode::new);
            assertTrue(registry.lookupMajorMode("Test").isPresent());
        }

        @Test
        void CamelCaseのモード名がkebabCaseのコマンド名に変換される() {
            assertEquals("python-mode", ModeRegistry.toCommandName("Python"));
            assertEquals("electric-pair-mode", ModeRegistry.toCommandName("ElectricPair"));
            assertEquals("text-mode", ModeRegistry.toCommandName("Text"));
            assertEquals("markdown-mode", ModeRegistry.toCommandName("Markdown"));
        }
    }

    @Nested
    class モードフック {

        private BufferFacade dummyBuffer() {
            return new BufferFacade(new EditableBuffer("test", new GapTextModel()));
        }

        @Test
        void メジャーモードフックにバッファとモード名が渡される() {
            var executed = Lists.mutable.<String>empty();
            registry.addMajorModeHook("TestMode", (buf, mode) -> {
                executed.add(buf.getName() + ":" + mode);
            });

            registry.runMajorModeHooks("TestMode", dummyBuffer());

            assertEquals(Lists.mutable.of("test:TestMode"), executed);
        }

        @Test
        void 複数のフックが登録順に実行される() {
            var executed = Lists.mutable.<String>empty();
            registry.addMajorModeHook("TestMode", (buf, mode) -> executed.add("hook1"));
            registry.addMajorModeHook("TestMode", (buf, mode) -> executed.add("hook2"));

            registry.runMajorModeHooks("TestMode", dummyBuffer());

            assertEquals(Lists.mutable.of("hook1", "hook2"), executed);
        }

        @Test
        void マイナーモードフックが実行される() {
            var executed = Lists.mutable.<String>empty();
            registry.addMinorModeHook("TestMinor", (buf, mode) -> executed.add("hook1"));

            registry.runMinorModeHooks("TestMinor", dummyBuffer());

            assertEquals(Lists.mutable.of("hook1"), executed);
        }

        @Test
        void フック未登録のモードではrunHooksが何もしない() {
            registry.runMajorModeHooks("Nonexistent", dummyBuffer());
            // 例外が発生しないことを確認
        }

        @Test
        void フック内の例外が他のフックの実行を妨げない() {
            var executed = Lists.mutable.<String>empty();
            registry.addMajorModeHook("TestMode", (buf, mode) -> {
                throw new RuntimeException("intentional error");
            });
            registry.addMajorModeHook("TestMode", (buf, mode) -> executed.add("after-error"));

            registry.runMajorModeHooks("TestMode", dummyBuffer());

            assertEquals(Lists.mutable.of("after-error"), executed);
        }

        @Test
        void モード登録前にフックを追加できる() {
            var executed = Lists.mutable.<String>empty();
            registry.addMajorModeHook("FutureMode", (buf, mode) -> executed.add("pre-registered"));

            registry.runMajorModeHooks("FutureMode", dummyBuffer());

            assertEquals(Lists.mutable.of("pre-registered"), executed);
        }
    }
}
