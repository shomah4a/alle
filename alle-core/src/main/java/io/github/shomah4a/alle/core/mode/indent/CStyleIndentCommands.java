package io.github.shomah4a.alle.core.mode.indent;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * Cスタイルインデントのコマンド群を生成するファクトリ。
 */
public final class CStyleIndentCommands {

    private CStyleIndentCommands() {}

    /**
     * コマンド群を生成する。
     *
     * @param modePrefix モード名プレフィックス（例: "javascript"）
     * @param state インデント状態
     * @return コマンド群
     */
    public static Commands create(String modePrefix, CStyleIndentState state) {
        return new Commands(
                new IndentLineCommand(modePrefix, state),
                new DedentLineCommand(modePrefix, state),
                new NewlineAndIndentCommand(modePrefix, state));
    }

    /**
     * インデント系コマンドの組。
     */
    public record Commands(Command indentLine, Command dedentLine, Command newlineAndIndent) {}

    private static final class IndentLineCommand implements Command {
        private final String commandName;
        private final CStyleIndentState state;

        IndentLineCommand(String modePrefix, CStyleIndentState state) {
            this.commandName = modePrefix + "-indent-line";
            this.state = state;
        }

        @Override
        public String name() {
            return commandName;
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.cycleIndent(context.activeWindow(), 1);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class DedentLineCommand implements Command {
        private final String commandName;
        private final CStyleIndentState state;

        DedentLineCommand(String modePrefix, CStyleIndentState state) {
            this.commandName = modePrefix + "-dedent-line";
            this.state = state;
        }

        @Override
        public String name() {
            return commandName;
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.cycleIndent(context.activeWindow(), -1);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NewlineAndIndentCommand implements Command {
        private final String commandName;
        private final CStyleIndentState state;

        NewlineAndIndentCommand(String modePrefix, CStyleIndentState state) {
            this.commandName = modePrefix + "-newline-and-indent";
            this.state = state;
        }

        @Override
        public String name() {
            return commandName;
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            state.newlineAndIndent(context.activeWindow());
            return CompletableFuture.completedFuture(null);
        }
    }
}
