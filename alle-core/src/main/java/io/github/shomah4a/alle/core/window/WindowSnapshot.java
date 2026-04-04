package io.github.shomah4a.alle.core.window;

import java.util.Objects;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * ウィンドウ1つ分の状態スナップショット。
 * 現在表示中のバッファとビュー状態、バッファ履歴、表示設定を保持する。
 *
 * @param current 現在表示中のバッファの識別子とビュー状態
 * @param history バッファ履歴（先頭が最も最近表示したバッファ）
 * @param truncateLines 行切り詰めモードの状態
 */
public record WindowSnapshot(
        BufferHistoryEntry current, ImmutableList<BufferHistoryEntry> history, boolean truncateLines) {

    public WindowSnapshot {
        Objects.requireNonNull(current);
        Objects.requireNonNull(history);
    }
}
