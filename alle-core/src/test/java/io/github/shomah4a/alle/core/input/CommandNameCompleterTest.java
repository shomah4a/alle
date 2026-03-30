package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandNameCompleterTest {

    private CommandNameCompleter completer;

    @BeforeEach
    void setUp() {
        var registry = new CommandRegistry();
        registry.register(stubCommand("forward-char"));
        registry.register(stubCommand("forward-word"));
        registry.register(stubCommand("backward-char"));
        registry.register(stubCommand("find-file"));
        var resolver = new CommandResolver(registry);
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        completer = new CommandNameCompleter(resolver, buffer);
    }

    @Test
    void 前方一致する候補を返す() {
        var result = completer.complete("forward");
        assertEquals(2, result.size());
        assertTrue(result.anySatisfy(c -> c.value().equals("forward-char")));
        assertTrue(result.anySatisfy(c -> c.value().equals("forward-word")));
    }

    @Test
    void 一致する候補がない場合は空リストを返す() {
        var result = completer.complete("xyz");
        assertTrue(result.isEmpty());
    }

    @Test
    void 空文字列はグローバルコマンドとFQCN候補を返す() {
        var result = completer.complete("");
        // 4コマンド + global.プレフィックス + global.付き4コマンド = 9
        assertEquals(9, result.size());
    }

    @Test
    void sortedCompleteで候補がlabel順にソートされて返る() {
        var result = completer.sortedComplete("f");
        assertEquals("find-file", result.get(0).value());
        assertEquals("forward-char", result.get(1).value());
        assertEquals("forward-word", result.get(2).value());
    }

    @Test
    void 候補はすべてterminalである() {
        var result = completer.complete("f");
        assertTrue(result.allSatisfy(CompletionCandidate::terminal));
    }

    private static Command stubCommand(String name) {
        return new Command() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public CompletableFuture<Void> execute(CommandContext context) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
