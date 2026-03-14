package io.github.shomah4a.alle.core.setting;

/**
 * 型安全な設定キー。
 * key（設定名）、type（型）、defaultValue（デフォルト値）を保持する。
 */
public record Setting<T>(String key, Class<T> type, T defaultValue) {

    /**
     * 設定キーを生成する。
     */
    public static <T> Setting<T> of(String key, Class<T> type, T defaultValue) {
        return new Setting<>(key, type, defaultValue);
    }

    /**
     * 値をこの設定の型に安全にキャストする。
     *
     * @throws ClassCastException 値がこの設定の型と互換でない場合
     */
    public T cast(Object value) {
        return type.cast(value);
    }
}
