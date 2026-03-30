package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommandResolverTest {

    private static Command stubCommand(String name) {
        return new Command() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public CompletableFuture<Void> execute(CommandContext ctx) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    private static BufferFacade createBuffer() {
        return new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
    }

    private static BufferFacade createBufferWithMajorMode(MajorMode mode) {
        var textBuffer = new TextBuffer("test", new GapTextModel(), new SettingsRegistry());
        textBuffer.setMajorMode(mode);
        return new BufferFacade(textBuffer);
    }

    private static BufferFacade createBufferWithMinorMode(MinorMode mode) {
        var textBuffer = new TextBuffer("test", new GapTextModel(), new SettingsRegistry());
        textBuffer.enableMinorMode(mode);
        return new BufferFacade(textBuffer);
    }

    private static MajorMode majorModeWithCommands(String modeName, Command... commands) {
        var registry = new CommandRegistry();
        for (Command cmd : commands) {
            registry.register(cmd);
        }
        return new MajorMode() {
            @Override
            public String name() {
                return modeName;
            }

            @Override
            public Optional<Keymap> keymap() {
                return Optional.empty();
            }

            @Override
            public Optional<CommandRegistry> commandRegistry() {
                return Optional.of(registry);
            }
        };
    }

    private static MinorMode minorModeWithCommands(String modeName, Command... commands) {
        var registry = new CommandRegistry();
        for (Command cmd : commands) {
            registry.register(cmd);
        }
        return new MinorMode() {
            @Override
            public String name() {
                return modeName;
            }

            @Override
            public Optional<Keymap> keymap() {
                return Optional.empty();
            }

            @Override
            public Optional<CommandRegistry> commandRegistry() {
                return Optional.of(registry);
            }
        };
    }

    @Nested
    class グローバル解決 {

        @Test
        void グローバルレジストリからコマンドを解決できる() {
            var globalRegistry = new CommandRegistry();
            var command = stubCommand("next-line");
            globalRegistry.register(command);
            var resolver = new CommandResolver(globalRegistry);

            var result = resolver.resolve("next-line", createBuffer());

            assertTrue(result.isPresent());
            assertEquals(command, result.get());
        }

        @Test
        void バッファなしでグローバルレジストリから解決できる() {
            var globalRegistry = new CommandRegistry();
            var command = stubCommand("next-line");
            globalRegistry.register(command);
            var resolver = new CommandResolver(globalRegistry);

            var result = resolver.resolve("next-line");

            assertTrue(result.isPresent());
            assertEquals(command, result.get());
        }

        @Test
        void 存在しないコマンド名はemptyを返す() {
            var resolver = new CommandResolver(new CommandRegistry());

            var result = resolver.resolve("nonexistent", createBuffer());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class MajorMode解決 {

        @Test
        void MajorModeのコマンドを短い名前で解決できる() {
            var modeCommand = stubCommand("toggle");
            var mode = majorModeWithCommands("Dired", modeCommand);
            var buffer = createBufferWithMajorMode(mode);
            var resolver = new CommandResolver(new CommandRegistry());

            var result = resolver.resolve("toggle", buffer);

            assertTrue(result.isPresent());
            assertEquals(modeCommand, result.get());
        }

        @Test
        void MajorModeのコマンドがグローバルより優先される() {
            var globalCommand = stubCommand("toggle");
            var globalRegistry = new CommandRegistry();
            globalRegistry.register(globalCommand);

            var modeCommand = stubCommand("toggle");
            var mode = majorModeWithCommands("Dired", modeCommand);
            var buffer = createBufferWithMajorMode(mode);
            var resolver = new CommandResolver(globalRegistry);

            var result = resolver.resolve("toggle", buffer);

            assertTrue(result.isPresent());
            assertEquals(modeCommand, result.get());
        }
    }

    @Nested
    class MinorMode解決 {

        @Test
        void MinorModeのコマンドを短い名前で解決できる() {
            var modeCommand = stubCommand("toggle-pair");
            var mode = minorModeWithCommands("ElectricPair", modeCommand);
            var buffer = createBufferWithMinorMode(mode);
            var resolver = new CommandResolver(new CommandRegistry());

            var result = resolver.resolve("toggle-pair", buffer);

            assertTrue(result.isPresent());
            assertEquals(modeCommand, result.get());
        }

        @Test
        void MinorModeのコマンドがMajorModeより優先される() {
            var majorCommand = stubCommand("indent");
            var major = majorModeWithCommands("Python", majorCommand);

            var minorCommand = stubCommand("indent");
            var minor = minorModeWithCommands("SmartIndent", minorCommand);

            var textBuffer = new TextBuffer("test", new GapTextModel(), new SettingsRegistry());
            textBuffer.setMajorMode(major);
            textBuffer.enableMinorMode(minor);
            var buffer = new BufferFacade(textBuffer);
            var resolver = new CommandResolver(new CommandRegistry());

            var result = resolver.resolve("indent", buffer);

            assertTrue(result.isPresent());
            assertEquals(minorCommand, result.get());
        }

        @Test
        void 後から有効にしたMinorModeが先のMinorModeより優先される() {
            var firstCommand = stubCommand("format");
            var firstMode = minorModeWithCommands("FormatA", firstCommand);

            var secondCommand = stubCommand("format");
            var secondMode = minorModeWithCommands("FormatB", secondCommand);

            var textBuffer = new TextBuffer("test", new GapTextModel(), new SettingsRegistry());
            textBuffer.enableMinorMode(firstMode);
            textBuffer.enableMinorMode(secondMode);
            var buffer = new BufferFacade(textBuffer);
            var resolver = new CommandResolver(new CommandRegistry());

            var result = resolver.resolve("format", buffer);

            assertTrue(result.isPresent());
            assertEquals(secondCommand, result.get());
        }
    }

    @Nested
    class FQCN解決 {

        @Test
        void FQCNでMajorModeのコマンドを解決できる() {
            var modeCommand = stubCommand("toggle");
            var mode = majorModeWithCommands("Dired", modeCommand);
            var buffer = createBufferWithMajorMode(mode);
            var resolver = new CommandResolver(new CommandRegistry());
            resolver.registerModeCommands("Dired", mode.commandRegistry().get());

            var result = resolver.resolve("Dired.toggle", buffer);

            assertTrue(result.isPresent());
            assertEquals(modeCommand, result.get());
        }

        @Test
        void FQCNでMinorModeのコマンドを解決できる() {
            var modeCommand = stubCommand("toggle-pair");
            var mode = minorModeWithCommands("ElectricPair", modeCommand);
            var buffer = createBufferWithMinorMode(mode);
            var resolver = new CommandResolver(new CommandRegistry());
            resolver.registerModeCommands("ElectricPair", mode.commandRegistry().get());

            var result = resolver.resolve("ElectricPair.toggle-pair", buffer);

            assertTrue(result.isPresent());
            assertEquals(modeCommand, result.get());
        }

        @Test
        void globalプレフィックスでグローバルコマンドを明示的に解決できる() {
            var globalCommand = stubCommand("next-line");
            var globalRegistry = new CommandRegistry();
            globalRegistry.register(globalCommand);

            var modeCommand = stubCommand("next-line");
            var mode = majorModeWithCommands("Dired", modeCommand);
            var buffer = createBufferWithMajorMode(mode);
            var resolver = new CommandResolver(globalRegistry);

            var result = resolver.resolve("global.next-line", buffer);

            assertTrue(result.isPresent());
            assertEquals(globalCommand, result.get());
        }

        @Test
        void 存在しないモード名のFQCNはemptyを返す() {
            var resolver = new CommandResolver(new CommandRegistry());

            var result = resolver.resolve("NonExistent.toggle", createBuffer());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class コマンド名一覧 {

        @Test
        void グローバルとモードのコマンド名が統合される() {
            var globalRegistry = new CommandRegistry();
            globalRegistry.register(stubCommand("next-line"));

            var modeCommand = stubCommand("toggle");
            var mode = majorModeWithCommands("Dired", modeCommand);
            var buffer = createBufferWithMajorMode(mode);
            var resolver = new CommandResolver(globalRegistry);

            var names = resolver.allCommandNames(buffer);

            assertTrue(names.contains("next-line"));
            assertTrue(names.contains("toggle"));
        }

        @Test
        void モードのコマンドがグローバルと同名でも重複しない() {
            var globalRegistry = new CommandRegistry();
            globalRegistry.register(stubCommand("toggle"));

            var modeCommand = stubCommand("toggle");
            var mode = majorModeWithCommands("Dired", modeCommand);
            var buffer = createBufferWithMajorMode(mode);
            var resolver = new CommandResolver(globalRegistry);

            var names = resolver.allCommandNames(buffer);

            long toggleCount = names.select(n -> n.equals("toggle")).size();
            assertEquals(1, toggleCount);
        }
    }
}
