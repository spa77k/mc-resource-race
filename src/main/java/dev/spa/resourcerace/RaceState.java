package dev.spa.resourcerace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 進行中（または終了済み）のレース1本と、その順位。Bukkit に依存しない。
 *
 * 順位の確定と報酬の支払いを分けて持ち、paid が立っていれば二度払わない。
 */
final class RaceState {

    /** 1人ぶんの順位。 */
    record Standing(int rank, UUID uuid, String name, Instant at, boolean paid) {

        Standing asPaid() {
            return new Standing(rank, uuid, name, at, true);
        }
    }

    private final UUID raceId;
    private final String world;
    private final String item;
    private final int amount;
    private final Instant startedAt;
    private final Instant endsAt;
    private final List<Standing> standings = new ArrayList<>();
    private boolean finished;

    private RaceState(UUID raceId, String world, String item, int amount, Instant startedAt, Instant endsAt) {
        this.raceId = raceId;
        this.world = world;
        this.item = item;
        this.amount = amount;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    static RaceState start(String world, String item, int amount, Instant startedAt, int durationHours) {
        return new RaceState(UUID.randomUUID(), world, item, amount, startedAt,
                Deadline.endsAt(startedAt, durationHours));
    }

    static RaceState restored(UUID raceId, String world, String item, int amount,
                              Instant startedAt, Instant endsAt, boolean finished) {
        RaceState state = new RaceState(raceId, world, item, amount, startedAt, endsAt);
        state.finished = finished;
        return state;
    }

    UUID raceId() {
        return raceId;
    }

    String world() {
        return world;
    }

    String item() {
        return item;
    }

    int amount() {
        return amount;
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant endsAt() {
        return endsAt;
    }

    boolean finished() {
        return finished;
    }

    void finish() {
        finished = true;
    }

    List<Standing> standings() {
        return List.copyOf(standings);
    }

    boolean hasStanding(UUID uuid) {
        return standing(uuid) != null;
    }

    Standing standing(UUID uuid) {
        for (Standing standing : standings) {
            if (standing.uuid().equals(uuid)) {
                return standing;
            }
        }
        return null;
    }

    /** 順位がまだ埋まっていれば確定して順位を返す。埋まっている・確定済みなら0。 */
    int place(UUID uuid, String name, Instant at, int maxRanks) {
        if (hasStanding(uuid) || standings.size() >= maxRanks) {
            return 0;
        }
        int rank = standings.size() + 1;
        standings.add(new Standing(rank, uuid, name, at, false));
        return rank;
    }

    boolean isFull(int maxRanks) {
        return standings.size() >= maxRanks;
    }

    /** まだ払っていない順位。 */
    List<Standing> unpaid() {
        List<Standing> pending = new ArrayList<>();
        for (Standing standing : standings) {
            if (!standing.paid()) {
                pending.add(standing);
            }
        }
        return pending;
    }

    /** 支払い済みにする。すでに支払い済みなら false。 */
    boolean markPaid(UUID uuid) {
        for (int index = 0; index < standings.size(); index++) {
            Standing standing = standings.get(index);
            if (standing.uuid().equals(uuid)) {
                if (standing.paid()) {
                    return false;
                }
                standings.set(index, standing.asPaid());
                return true;
            }
        }
        return false;
    }

    /** state.yml から読み直すときだけ使う。 */
    void restoreStanding(Standing standing) {
        standings.add(standing);
    }
}
