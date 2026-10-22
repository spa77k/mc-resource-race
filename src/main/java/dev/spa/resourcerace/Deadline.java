package dev.spa.resourcerace;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** レースの期限。日本時間で「10/31(土) 3:00まで」のように表示する。 */
final class Deadline {

    static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("M/d(E) H:mm", Locale.JAPANESE).withZone(ZONE);

    private Deadline() {
    }

    static Instant endsAt(Instant startedAt, int durationHours) {
        return startedAt.plus(durationHours, ChronoUnit.HOURS);
    }

    static String format(Instant instant) {
        return FORMAT.format(instant) + "まで";
    }
}
