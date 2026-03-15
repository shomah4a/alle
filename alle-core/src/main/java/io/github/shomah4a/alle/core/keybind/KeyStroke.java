package io.github.shomah4a.alle.core.keybind;

import java.util.Objects;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * 単一のキー入力。修飾キーの組み合わせとキーコードで構成される。
 * 例: Ctrl+X, Meta+F など。
 */
public record KeyStroke(ImmutableSet<Modifier> modifiers, int keyCode) {

    // 特殊キー用コードポイント（Unicode Private Use Area）
    public static final int ARROW_UP = 0xF700;
    public static final int ARROW_DOWN = 0xF701;
    public static final int ARROW_LEFT = 0xF702;
    public static final int ARROW_RIGHT = 0xF703;

    /**
     * 修飾キーなしのキーストロークを生成する。
     */
    public static KeyStroke of(int keyCode) {
        return new KeyStroke(Sets.immutable.empty(), keyCode);
    }

    /**
     * 修飾キー付きのキーストロークを生成する。
     */
    public static KeyStroke of(int keyCode, Modifier... modifiers) {
        return new KeyStroke(Sets.immutable.with(modifiers), keyCode);
    }

    /**
     * Ctrl+キーのキーストロークを生成する。
     */
    public static KeyStroke ctrl(int keyCode) {
        return new KeyStroke(Sets.immutable.of(Modifier.CTRL), keyCode);
    }

    /**
     * Meta+キーのキーストロークを生成する。
     */
    public static KeyStroke meta(int keyCode) {
        return new KeyStroke(Sets.immutable.of(Modifier.META), keyCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof KeyStroke other)) return false;
        return keyCode == other.keyCode && modifiers.equals(other.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifiers, keyCode);
    }
}
