package io.github.shomah4a.alle.core.buffer;

/**
 * 読み取り専用バッファへの書き込み操作が試みられた場合にスローされる。
 */
public class ReadOnlyBufferException extends RuntimeException {

    public ReadOnlyBufferException(String bufferName) {
        super("Buffer is read-only: " + bufferName);
    }
}
