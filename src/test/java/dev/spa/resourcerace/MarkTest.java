package dev.spa.resourcerace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarkTest {

    private static final UUID RACE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RACE_OLD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PLAYER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTHER = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void 印を文字列にして読み直せる() {
        Mark mark = new Mark(RACE, PLAYER);
        assertEquals(RACE + ":" + PLAYER, mark.encode());
        assertEquals(mark, Mark.decode(mark.encode()));
    }

    @Test
    void 持ち主がいない印はアスタリスクになる() {
        Mark mark = Mark.wildcard(RACE);
        assertTrue(mark.isWildcard());
        assertEquals(RACE + ":*", mark.encode());
        assertTrue(Mark.decode(mark.encode()).isWildcard());
    }

    @Test
    void 壊れた文字列は読めない() {
        assertNull(Mark.decode(null));
        assertNull(Mark.decode(""));
        assertNull(Mark.decode("not-a-uuid:*"));
        assertNull(Mark.decode(RACE.toString()));
        assertNull(Mark.decode(RACE + ":not-a-uuid"));
    }

    @Test
    void 今のレースかどうかを判定する() {
        Mark mark = new Mark(RACE, PLAYER);
        assertTrue(mark.isCurrent(RACE));
        assertFalse(mark.isCurrent(RACE_OLD));
        assertFalse(mark.isCurrent(null));
    }

    @Test
    void 持ち主を書き換えられる() {
        Mark mark = Mark.wildcard(RACE).ownedBy(PLAYER);
        assertTrue(mark.isOwnedBy(PLAYER));
        assertFalse(mark.isOwnedBy(OTHER));
        assertFalse(mark.isWildcard());
        assertEquals(RACE, mark.raceId());
    }
}
