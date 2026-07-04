package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.setting.Setting;

/**
 * git-mode / git-log 用の設定キー。
 */
public final class GitSettings {

    /**
     * git-log で 1 ページあたり取得する件数。
     */
    public static final Setting<Integer> LOG_PAGE_SIZE = Setting.of("git-log-page-size", Integer.class, 30);

    /**
     * git-log 結果の subject 表示幅上限 (超過分は末尾に "…" で truncate)。
     */
    public static final Setting<Integer> LOG_SUBJECT_MAX_WIDTH =
            Setting.of("git-log-subject-max-width", Integer.class, 80);

    /**
     * git-log 結果の body 表示行数上限 (超過分は "…" 1 行で省略)。
     */
    public static final Setting<Integer> LOG_BODY_MAX_LINES = Setting.of("git-log-body-max-lines", Integer.class, 3);

    private GitSettings() {}
}
