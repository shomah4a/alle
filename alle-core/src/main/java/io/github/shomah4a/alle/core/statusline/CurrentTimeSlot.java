package io.github.shomah4a.alle.core.statusline;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 現在時刻を表示するステータスラインスロット。
 * 時刻取得はClockを経由し、テスタビリティを確保する。
 * フォーマット文字列は {@link StatusLineSettings#TIME_FORMAT} 設定で変更可能。
 */
public final class CurrentTimeSlot implements StatusLineElement {

    private final Clock clock;

    public CurrentTimeSlot(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String name() {
        return "current-time";
    }

    @Override
    public String render(StatusLineContext context) {
        String pattern = context.buffer().getSettings().get(StatusLineSettings.TIME_FORMAT);
        var formatter = DateTimeFormatter.ofPattern(pattern);
        return " " + ZonedDateTime.now(clock).format(formatter);
    }
}
