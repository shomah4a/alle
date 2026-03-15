package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.keybind.Keymap;
import java.util.Optional;

/**
 * メジャーモード。バッファのファイルタイプに応じて1つだけ有効になる。
 * モード固有のキーマップを提供する。
 */
public interface MajorMode {

    /**
     * モード名を返す。モードラインに表示される。
     */
    String name();

    /**
     * モード固有のキーマップを返す。
     * キーバインドを持たない場合はemptyを返す。
     */
    Optional<Keymap> keymap();
}
