package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import org.eclipse.collections.api.list.ListIterable;

/**
 * コマンド名の補完を提供する。
 * CommandRegistryの登録済みコマンド名から前方一致で候補を返す。
 * コマンド名は確定可能（terminal）な候補として返す。
 */
public class CommandNameCompleter implements Completer {

    private final CommandRegistry registry;

    public CommandNameCompleter(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ListIterable<CompletionCandidate> complete(String input) {
        return registry.registeredNames()
                .select(name -> name.startsWith(input))
                .collect(CompletionCandidate::terminal)
                .toSortedListBy(CompletionCandidate::value);
    }
}
