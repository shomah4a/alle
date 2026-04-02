package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import java.util.Objects;

/**
 * バッファ履歴のエントリ。
 * バッファの識別子と、そのバッファを表示していた時のビュー状態を保持する。
 * equals/hashCode は識別子ベースで判定する。
 */
public final class BufferHistoryEntry {

    private final BufferIdentifier identifier;
    private final ViewState viewState;

    public BufferHistoryEntry(BufferIdentifier identifier, ViewState viewState) {
        this.identifier = Objects.requireNonNull(identifier);
        this.viewState = Objects.requireNonNull(viewState);
    }

    /**
     * BufferFacade とビュー状態から履歴エントリを生成する。
     */
    public static BufferHistoryEntry of(BufferFacade buffer, ViewState viewState) {
        return new BufferHistoryEntry(BufferIdentifier.of(buffer), viewState);
    }

    public BufferIdentifier identifier() {
        return identifier;
    }

    public ViewState viewState() {
        return viewState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BufferHistoryEntry other)) return false;
        return identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return "BufferHistoryEntry{identifier=" + identifier + ", viewState=" + viewState + "}";
    }
}
