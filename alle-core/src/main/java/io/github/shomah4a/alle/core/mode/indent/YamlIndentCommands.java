package io.github.shomah4a.alle.core.mode.indent;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * YAMLインデントのコマンド群を生成するファクトリ。
 */
public final class YamlIndentCommands {

    private YamlIndentCommands() {}

    /**
     * コマンド群を生成する。
     *
     * @param state インデント状態
     * @return コマンド群
     */
    public static Commands create(YamlIndentState state) {
        return new Commands(
                new IndentLineCommand(state), new DedentLineCommand(state), new NewlineAndIndentCommand(state));
    }

    /**
     * インデント系コマンドの組。
     */
    public record Commands(Command indentLine, Command dedentLine, Command newlineAndIndent) {}

    private static final class IndentLineCommand implements Command {
        private final YamlIndentState state;

        IndentLineCommand(YamlIndentState state) {
            this.state = state;
        }

        @Override
        public String name() {
            return "yaml-indent-line";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.cycleIndent(context.activeWindow(), 1);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class DedentLineCommand implements Command {
        private final YamlIndentState state;

        DedentLineCommand(YamlIndentState state) {
            this.state = state;
        }

        @Override
        public String name() {
            return "yaml-dedent-line";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.cycleIndent(context.activeWindow(), -1);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NewlineAndIndentCommand implements Command {
        private final YamlIndentState state;

        NewlineAndIndentCommand(YamlIndentState state) {
            this.state = state;
        }

        @Override
        public String name() {
            return "yaml-newline-and-indent";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.newlineAndIndent(context.activeWindow());
            return CompletableFuture.completedFuture(null);
        }
    }
}
