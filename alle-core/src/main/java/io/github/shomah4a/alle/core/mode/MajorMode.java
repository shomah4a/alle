package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import java.util.Optional;

/**
 * メジャーモード。バッファのファイルタイプに応じて1つだけ有効になる。
 * モード固有のキーマップとシンタックススタイリングを提供する。
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

    /**
     * シンタックススタイラーを返す。
     * スタイリング不要の場合はemptyを返す。
     */
    default Optional<SyntaxStyler> styler() {
        return Optional.empty();
    }
}
