package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.ModeSettings;
import java.util.Optional;

/**
 * マイナーモード。メジャーモードと独立して複数有効にできる。
 * モード固有のキーマップを提供する。
 */
public interface MinorMode {

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
     * モード固有の設定デフォルト値を返す。
     * カスタムポイントを持たない場合は空のModeSettingsを返す。
     */
    default ModeSettings settingDefaults() {
        return ModeSettings.empty();
    }
}
