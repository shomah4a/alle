package io.github.shomah4a.alle.core.keybind;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * キーストロークの列。
 * 単一キー（C-f）や複合キー（C-x C-s）を表現する。
 */
public record KeySequence(ImmutableList<KeyStroke> keyStrokes) {

    /**
     * 単一キーストロークからキーシーケンスを生成する。
     */
    public static KeySequence of(KeyStroke keyStroke) {
        return new KeySequence(Lists.immutable.of(keyStroke));
    }

    /**
     * 複数キーストロークからキーシーケンスを生成する。
     */
    public static KeySequence of(KeyStroke... keyStrokes) {
        return new KeySequence(Lists.immutable.of(keyStrokes));
    }

    /**
     * キーシーケンスの長さを返す。
     */
    public int length() {
        return keyStrokes.size();
    }

    /**
     * 先頭のキーストロークを返す。
     */
    public KeyStroke first() {
        return keyStrokes.get(0);
    }

    /**
     * 先頭を除いた残りのキーシーケンスを返す。
     */
    public KeySequence rest() {
        return new KeySequence(keyStrokes.subList(1, keyStrokes.size()));
    }
}
