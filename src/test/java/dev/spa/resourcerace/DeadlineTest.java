package dev.spa.resourcerace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeadlineTest {

    @Test
    void 開始から指定時間あとが期限になる() {
        Instant startedAt = Instant.parse("2026-10-30T18:00:00Z");
        assertEquals(Instant.parse("2026-11-06T17:00:00Z"), Deadline.endsAt(startedAt, 167));
    }

    @Test
    void 期限を日本時間で表示する() {
        // 2026-10-31 03:00 JST（土曜）
        assertEquals("10/31(土) 3:00まで", Deadline.format(Instant.parse("2026-10-30T18:00:00Z")));
        // 2026-11-07 02:00 JST（土曜）
        assertEquals("11/7(土) 2:00まで", Deadline.format(Instant.parse("2026-11-06T17:00:00Z")));
    }
}
