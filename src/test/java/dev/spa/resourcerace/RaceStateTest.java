package dev.spa.resourcerace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RaceStateTest {

    private static final UUID ALICE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID CAROL = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID DAVE = UUID.fromString("dddddddd-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-10-30T18:00:00Z");

    @Test
    void 順位を確定して報酬の数を超えたら埋まる() {
        RaceState state = RaceState.start("resource2", "DIAMOND", 5, NOW, 167);
        assertEquals(1, state.place(ALICE, "Alice", NOW, 3));
        assertEquals(2, state.place(BOB, "Bob", NOW, 3));
        assertEquals(3, state.place(CAROL, "Carol", NOW, 3));
        assertTrue(state.isFull(3));
        assertEquals(0, state.place(DAVE, "Dave", NOW, 3));
        assertEquals(3, state.standings().size());
    }

    @Test
    void 同じ人を二度確定しない() {
        RaceState state = RaceState.start("resource2", "DIAMOND", 5, NOW, 167);
        assertEquals(1, state.place(ALICE, "Alice", NOW, 3));
        assertEquals(0, state.place(ALICE, "Alice", NOW, 3));
        assertTrue(state.hasStanding(ALICE));
        assertEquals(1, state.standings().size());
    }

    @Test
    void 報酬を二度払わない() {
        RaceState state = RaceState.start("resource2", "DIAMOND", 5, NOW, 167);
        state.place(ALICE, "Alice", NOW, 3);
        state.place(BOB, "Bob", NOW, 3);
        assertEquals(2, state.unpaid().size());
        assertTrue(state.markPaid(ALICE));
        assertFalse(state.markPaid(ALICE));
        assertEquals(1, state.unpaid().size());
        assertEquals(BOB, state.unpaid().get(0).uuid());
        assertFalse(state.markPaid(DAVE));
    }

    @Test
    void 期限は開始から計算する() {
        RaceState state = RaceState.start("resource2", "DIAMOND", 5, NOW, 167);
        assertEquals(Instant.parse("2026-11-06T17:00:00Z"), state.endsAt());
        assertFalse(state.finished());
        state.finish();
        assertTrue(state.finished());
    }
}
