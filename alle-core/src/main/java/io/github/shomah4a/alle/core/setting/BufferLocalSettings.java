package io.github.shomah4a.alle.core.setting;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

/**
 * バッファローカルな設定値を管理する。
 * ローカル値が存在しない場合はSettingsRegistryのデフォルト値にフォールバックする。
 * Emacsのバッファローカル変数に相当する。
 */
public class BufferLocalSettings {

    private final SettingsRegistry registry;
    private final MutableMap<String, Object> localValues = Maps.mutable.empty();

    public BufferLocalSettings(SettingsRegistry registry) {
        this.registry = registry;
    }

    /**
     * 設定値を取得する。ローカル値があればそれを、なければデフォルト値を返す。
     */
    public <T> T get(Setting<T> setting) {
        Object localValue = localValues.get(setting.key());
        if (localValue != null) {
            return setting.cast(localValue);
        }
        return registry.getDefault(setting);
    }

    /**
     * バッファローカルな値を設定する。
     */
    public <T> void setLocal(Setting<T> setting, T value) {
        localValues.put(setting.key(), value);
    }

    /**
     * バッファローカルな値を解除し、デフォルト値にフォールバックするようにする。
     */
    public void removeLocal(Setting<?> setting) {
        localValues.remove(setting.key());
    }

    /**
     * バッファローカルな値が設定されているかを返す。
     */
    public boolean hasLocal(Setting<?> setting) {
        return localValues.containsKey(setting.key());
    }
}
