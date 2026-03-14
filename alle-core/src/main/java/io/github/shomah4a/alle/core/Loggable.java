package io.github.shomah4a.alle.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4Jロガーを提供するインターフェース。
 * 実装クラスのFQCNに基づくロガーを返す。
 */
public interface Loggable {

    default Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
