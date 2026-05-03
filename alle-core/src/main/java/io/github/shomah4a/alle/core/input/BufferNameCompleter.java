package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import org.eclipse.collections.api.list.ListIterable;

/**
 * バッファ名による補完を提供する。
 * BufferManagerに登録されたバッファ名から前方一致で候補を返す。
 * バッファ名は確定可能（terminal）な候補として返す。
 */
public class BufferNameCompleter implements Completer {

    private final BufferManager bufferManager;
    private final boolean ignoreCase;

    public BufferNameCompleter(BufferManager bufferManager) {
        this(bufferManager, false);
    }

    public BufferNameCompleter(BufferManager bufferManager, boolean ignoreCase) {
        this.bufferManager = bufferManager;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public ListIterable<CompletionCandidate> complete(String input) {
        return bufferManager
                .getBuffers()
                .select(b -> CompletionMatching.startsWith(b.getName(), input, ignoreCase))
                .collect(b -> CompletionCandidate.terminal(b.getName()));
    }
}
