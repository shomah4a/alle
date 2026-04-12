package io.github.shomah4a.alle.core.statusline;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 現在時刻を表示するステータスラインスロット。
 * 時刻取得はClockを経由し、テスタビリティを確保する。
 */
public final class CurrentTimeSlot implements StatusLineElement {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        return " " + ZonedDateTime.now(clock).format(FORMATTER);
    }
}
