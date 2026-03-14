package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.keybind.KeyStroke;
import java.util.Optional;

/**
 * キー入力の抽象化。
 * emptyを返すと入力終了を示す。
 */
public interface InputSource {

    /**
     * キーストロークを1つ読み取る。
     * 入力終了時はemptyを返す。
     */
    Optional<KeyStroke> readKeyStroke();
}
