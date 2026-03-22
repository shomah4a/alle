package io.github.shomah4a.alle.core.setting;

import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;

/**
 * モード固有の設定デフォルト値を保持するイミュータブルなコンテナ。
 * ビルダーを使って構築する。
 */
public class ModeSettings {

    private static final ModeSettings EMPTY = new ModeSettings(Maps.immutable.empty());

    private final ImmutableMap<String, Object> defaults;

    private ModeSettings(ImmutableMap<String, Object> defaults) {
        this.defaults = defaults;
    }

    /**
     * 空のModeSettingsを返す。
     */
    public static ModeSettings empty() {
        return EMPTY;
    }

    /**
     * ビルダーを生成する。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 指定した設定のモードデフォルト値を取得する。
     * 設定されていない場合はemptyを返す。
     */
    public <T> Optional<T> get(Setting<T> setting) {
        Object value = defaults.get(setting.key());
        if (value != null) {
            return Optional.of(setting.cast(value));
        }
        return Optional.empty();
    }

    /**
     * 設定が含まれているかを返す。
     */
    public boolean contains(Setting<?> setting) {
        return defaults.containsKey(setting.key());
    }

    /**
     * ModeSettingsのビルダー。
     */
    public static class Builder {

        private final MutableMap<String, Object> values = Maps.mutable.empty();

        private Builder() {}

        /**
         * モードデフォルト値を追加する。
         */
        public <T> Builder set(Setting<T> setting, T value) {
            values.put(setting.key(), value);
            return this;
        }

        /**
         * イミュータブルなModeSettingsを構築する。
         */
        public ModeSettings build() {
            if (values.isEmpty()) {
                return EMPTY;
            }
            return new ModeSettings(values.toImmutable());
        }
    }
}
