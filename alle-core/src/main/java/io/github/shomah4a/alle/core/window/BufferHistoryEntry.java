package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import java.nio.file.Path;

/**
 * バッファ履歴のエントリ。
 * ファイルパスを持つバッファはパスで、持たないバッファは名前で識別する。
 */
public sealed interface BufferHistoryEntry {

    /**
     * BufferFacade から履歴エントリを生成する。
     * ファイルパスがあれば ByPath、なければ ByName を返す。
     */
    static BufferHistoryEntry of(BufferFacade buffer) {
        return buffer.getFilePath().<BufferHistoryEntry>map(ByPath::new).orElseGet(() -> new ByName(buffer.getName()));
    }

    /**
     * ファイルパスによるバッファ識別。
     * displayName の変更に影響されない。
     */
    record ByPath(Path path) implements BufferHistoryEntry {}

    /**
     * バッファ名による識別。
     * ファイルパスを持たないバッファ（*scratch* 等）に使用する。
     */
    record ByName(String name) implements BufferHistoryEntry {}
}
