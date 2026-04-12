package io.github.shomah4a.alle.core.mode.modes.makefile;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * Makefileインデントのコマンド群を生成するファクトリ。
 */
public final class MakefileIndentCommands {

    private MakefileIndentCommands() {}

    /**
     * コマンド群を生成する。
     *
     * @param state インデント状態
     * @return コマンド群
     */
    public static Commands create(MakefileIndentState state) {
        return new Commands(
                new IndentLineCommand(state), new DedentLineCommand(state), new NewlineAndIndentCommand(state));
    }

    /**
     * インデント系コマンドの組。
     */
    public record Commands(Command indentLine, Command dedentLine, Command newlineAndIndent) {}

    private static final class IndentLineCommand implements Command {
        private final MakefileIndentState state;

        IndentLineCommand(MakefileIndentState state) {
            this.state = state;
        }

        @Override
        public String name() {
            return "makefile-indent-line";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.cycleIndent(context.activeWindow(), 1);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class DedentLineCommand implements Command {
        private final MakefileIndentState state;

        DedentLineCommand(MakefileIndentState state) {
            this.state = state;
        }

        @Override
        public String name() {
            return "makefile-dedent-line";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.cycleIndent(context.activeWindow(), -1);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NewlineAndIndentCommand implements Command {
        private final MakefileIndentState state;

        NewlineAndIndentCommand(MakefileIndentState state) {
            this.state = state;
        }

        @Override
        public String name() {
            return "makefile-newline-and-indent";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.newlineAndIndent(context.activeWindow());
            return CompletableFuture.completedFuture(null);
        }
    }
}
