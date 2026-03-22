package io.github.shomah4a.alle.core.setting;

import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;

/**
 * バッファローカルな設定値を管理する。
 * 以下の5層の優先順位で設定値を解決する:
 * 1. バッファローカル変数
 * 2. マイナーモードデフォルト（後から有効にしたものが優先）
 * 3. メジャーモードデフォルト
 * 4. エディタグローバル変数
 * 5. エディタデフォルト値（Setting.defaultValue）
 */
public class BufferLocalSettings {

    private final SettingsRegistry registry;
    private final Supplier<MajorMode> majorModeSupplier;
    private final Supplier<ListIterable<MinorMode>> minorModesSupplier;
    private final MutableMap<String, Object> localValues = Maps.mutable.empty();

    public BufferLocalSettings(
            SettingsRegistry registry,
            Supplier<MajorMode> majorModeSupplier,
            Supplier<ListIterable<MinorMode>> minorModesSupplier) {
        this.registry = registry;
        this.majorModeSupplier = majorModeSupplier;
        this.minorModesSupplier = minorModesSupplier;
    }

    /**
     * 設定値を5層の優先順位で解決して返す。
     */
    public <T> T get(Setting<T> setting) {
        // 1. バッファローカル変数
        Object localValue = localValues.get(setting.key());
        if (localValue != null) {
            return setting.cast(localValue);
        }

        // 2. マイナーモードデフォルト（後入り優先: リスト末尾から逆順探索）
        ListIterable<MinorMode> minorModes = minorModesSupplier.get();
        for (int i = minorModes.size() - 1; i >= 0; i--) {
            Optional<T> minorValue = minorModes.get(i).settingDefaults().get(setting);
            if (minorValue.isPresent()) {
                return minorValue.get();
            }
        }

        // 3. メジャーモードデフォルト
        Optional<T> majorValue = majorModeSupplier.get().settingDefaults().get(setting);
        if (majorValue.isPresent()) {
            return majorValue.get();
        }

        // 4. エディタグローバル変数
        Optional<T> globalValue = registry.getGlobal(setting);
        if (globalValue.isPresent()) {
            return globalValue.get();
        }

        // 5. エディタデフォルト値
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
