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

    /**
     * 現在時刻スロットの表示フォーマット。
     * {@link java.time.format.DateTimeFormatter} のパターン文字列。
     * デフォルトは "yyyy-MM-dd HH:mm"。
     */
    public static final Setting<String> TIME_FORMAT =
            Setting.of("status-line-time-format", String.class, "yyyy-MM-dd HH:mm");
}
