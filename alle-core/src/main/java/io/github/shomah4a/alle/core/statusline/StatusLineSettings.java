package io.github.shomah4a.alle.core.statusline;

import io.github.shomah4a.alle.core.setting.Setting;

/**
 * ステータスライン関連の設定定数。
 */
public final class StatusLineSettings {

    private StatusLineSettings() {}

    /**
     * ステータスラインのフォーマット定義。
     * バッファローカルに差し替え可能。
     */
    public static final Setting<StatusLineFormat> FORMAT =
            Setting.of("status-line-format", StatusLineFormat.class, StatusLineFormat.defaultFormat());
}
