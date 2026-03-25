package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import org.eclipse.collections.api.list.ListIterable;

/**
 * バッファ名による補完を提供する。
 * BufferManagerに登録されたバッファ名から前方一致で候補を返す。
 */
public class BufferNameCompleter implements Completer {

    private final BufferManager bufferManager;

    public BufferNameCompleter(BufferManager bufferManager) {
        this.bufferManager = bufferManager;
    }

    @Override
    public ListIterable<String> complete(String input) {
        return bufferManager
                .getBuffers()
                .select(b -> b.getName().startsWith(input))
                .collect(BufferFacade::getName);
    }
}
