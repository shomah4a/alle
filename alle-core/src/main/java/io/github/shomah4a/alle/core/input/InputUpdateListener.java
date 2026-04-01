package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.buffer.BufferFacade;

/**
 * ミニバッファのユーザー入力が変化したときに呼ばれるコールバック。
 * プロンプト利用側がテキスト変更に反応して face 適用等を行うために使用する。
 */
@FunctionalInterface
public interface InputUpdateListener {

    /**
     * ユーザー入力が変化した際に呼ばれる。
     *
     * @param minibuffer    ミニバッファのバッファ
     * @param promptLength  プロンプト文字列の長さ（ユーザー入力の開始位置）
     * @param currentInput  現在のユーザー入力文字列（プロンプト部分を除く）
     */
    void onUpdate(BufferFacade minibuffer, int promptLength, String currentInput);
}
