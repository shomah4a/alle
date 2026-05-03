package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.CommandResolver;
import org.eclipse.collections.api.list.ListIterable;

/**
 * コマンド名の補完を提供する。
 * CommandResolverを通じてカレントバッファのモードスコープを考慮した候補を返す。
 * モード名プレフィックス（例: "Python."）は継続補完（partial）として返す。
 * プレフィックス入力後はFQCN形式の候補を返す。
 */
public class CommandNameCompleter implements Completer {

    private final CommandResolver commandResolver;
    private final BufferFacade buffer;
    private final boolean ignoreCase;

    public CommandNameCompleter(CommandResolver commandResolver, BufferFacade buffer) {
        this(commandResolver, buffer, false);
    }

    public CommandNameCompleter(CommandResolver commandResolver, BufferFacade buffer, boolean ignoreCase) {
        this.commandResolver = commandResolver;
        this.buffer = buffer;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public ListIterable<CompletionCandidate> complete(String input) {
        // プレフィックス入力後（ドットを含む入力）はFQCN補完
        if (input.contains(".")) {
            return commandResolver
                    .completeFqcn(input, ignoreCase)
                    .collect(CompletionCandidate::terminal)
                    .toList();
        }

        // 通常の補完
        return commandResolver
                .allCommandNames(buffer)
                .select(name -> CompletionMatching.startsWith(name, input, ignoreCase))
                .collect(name ->
                        name.endsWith(".") ? CompletionCandidate.partial(name) : CompletionCandidate.terminal(name))
                .toList();
    }
}
