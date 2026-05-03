package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.window.FrameLayoutStore;
import org.eclipse.collections.api.list.ListIterable;

/**
 * 保存済みフレームレイアウト名による補完を提供する。
 * FrameLayoutStoreに保存された名前から前方一致で候補を返す。
 */
public class FrameLayoutNameCompleter implements Completer {

    private final FrameLayoutStore layoutStore;
    private final boolean ignoreCase;

    public FrameLayoutNameCompleter(FrameLayoutStore layoutStore) {
        this(layoutStore, false);
    }

    public FrameLayoutNameCompleter(FrameLayoutStore layoutStore, boolean ignoreCase) {
        this.layoutStore = layoutStore;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public ListIterable<CompletionCandidate> complete(String input) {
        return layoutStore
                .names()
                .select(name -> CompletionMatching.startsWith(name, input, ignoreCase))
                .collect(CompletionCandidate::terminal)
                .toList();
    }
}
