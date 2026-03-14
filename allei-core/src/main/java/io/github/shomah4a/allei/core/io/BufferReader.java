package io.github.shomah4a.allei.core.io;

import java.io.IOException;
import java.io.Reader;

/**
 * テキストデータの読み込みを抽象化するインターフェース。
 * 副作用の外部化のため、ファイルシステムへの直接依存を避ける。
 */
public interface BufferReader {

    /**
     * 指定された識別子からReaderを生成する。
     *
     * @param source データソースの識別子(ファイルパス等)
     * @return テキストデータを読み込むReader
     * @throws IOException 読み込みに失敗した場合
     */
    Reader open(String source) throws IOException;
}
