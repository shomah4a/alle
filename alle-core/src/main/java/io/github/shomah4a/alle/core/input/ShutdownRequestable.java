package io.github.shomah4a.alle.core.input;

/**
 * 終了要求を受け付けるインターフェース。
 * InputSourceとは責務を分離し、終了要求のみを扱う。
 */
public interface ShutdownRequestable {

    /**
     * 終了を要求する。
     */
    void requestShutdown();
}
