package dev.spa.resourcerace;

import java.util.UUID;

/**
 * アイテムに埋め込む印。どのレースで手に入れた物かを表す。
 *
 * owner が null のときは、まだ持ち主がいないことを表す "*"。
 * 持ち物に入れたときに、そのプレイヤーのUUIDへ書き換える。
 * 文字列は "&lt;race-id&gt;:&lt;owner&gt;" で、Bukkit に依存しない。
 */
record Mark(UUID raceId, UUID owner) {

    private static final String WILDCARD = "*";

    /** まだ持ち主がいない印。 */
    static Mark wildcard(UUID raceId) {
        return new Mark(raceId, null);
    }

    /** この印が今のレースのものか。今のレースが無い（null）ときは false。 */
    boolean isCurrent(UUID currentRaceId) {
        return raceId.equals(currentRaceId);
    }

    boolean isWildcard() {
        return owner == null;
    }

    boolean isOwnedBy(UUID uuid) {
        return owner != null && owner.equals(uuid);
    }

    /** 持ち主を uuid に書き換えた印を返す。 */
    Mark ownedBy(UUID uuid) {
        return new Mark(raceId, uuid);
    }

    String encode() {
        return raceId + ":" + (owner == null ? WILDCARD : owner.toString());
    }

    /** 文字列を読む。壊れているときは null。 */
    static Mark decode(String raw) {
        if (raw == null) {
            return null;
        }
        int separator = raw.indexOf(':');
        if (separator < 0) {
            return null;
        }
        try {
            UUID raceId = UUID.fromString(raw.substring(0, separator));
            String owner = raw.substring(separator + 1);
            return new Mark(raceId, WILDCARD.equals(owner) ? null : UUID.fromString(owner));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
