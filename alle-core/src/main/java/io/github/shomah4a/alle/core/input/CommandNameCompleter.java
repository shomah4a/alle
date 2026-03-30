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

    public CommandNameCompleter(CommandResolver commandResolver, BufferFacade buffer) {
        this.commandResolver = commandResolver;
        this.buffer = buffer;
    }

    @Override
    public ListIterable<CompletionCandidate> complete(String input) {
        // プレフィックス入力後（ドットを含む入力）はFQCN補完
        if (input.contains(".")) {
            return commandResolver
                    .completeFqcn(input)
                    .collect(CompletionCandidate::terminal)
                    .toList();
        }

        // 通常の補完
        return commandResolver
                .allCommandNames(buffer)
                .select(name -> name.startsWith(input))
                .collect(name ->
                        name.endsWith(".") ? CompletionCandidate.partial(name) : CompletionCandidate.terminal(name))
                .toList();
    }
}
