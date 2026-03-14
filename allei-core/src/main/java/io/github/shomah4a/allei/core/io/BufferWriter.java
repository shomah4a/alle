package io.github.shomah4a.allei.core.io;

import java.io.IOException;
import java.io.Writer;

/**
 * テキストデータの書き込みを抽象化するインターフェース。
 * 副作用の外部化のため、ファイルシステムへの直接依存を避ける。
 */
public interface BufferWriter {

    /**
     * 指定された識別子へのWriterを生成する。
     *
     * @param destination データの出力先の識別子(ファイルパス等)
     * @return テキストデータを書き込むWriter
     * @throws IOException 書き込みに失敗した場合
     */
    Writer open(String destination) throws IOException;
}
