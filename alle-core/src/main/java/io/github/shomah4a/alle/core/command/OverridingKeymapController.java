package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.keybind.Keymap;

/**
 * CommandLoopのoverriding keymapを制御するインターフェース。
 * overriding keymapが設定されている間、通常のキーマップ解決より優先してキーが解決される。
 * overriding keymapにバインドされていないキーが来た場合、onUnboundKeyExitが呼ばれてから
 * 通常のキーマップ解決にフォールスルーする。
 */
public interface OverridingKeymapController {

    /**
     * overriding keymapを設定する。
     *
     * @param keymap 最上位キーマップ
     * @param onUnboundKeyExit バインドされていないキーが来た場合の終了コールバック
     */
    void set(Keymap keymap, Runnable onUnboundKeyExit);

    /**
     * overriding keymapをクリアする。
     */
    void clear();
}
