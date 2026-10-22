package dev.spa.resourcerace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** &コード付きの文字列を Adventure の Component へ直す。 */
final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    static Component of(String legacy) {
        return LEGACY.deserialize(legacy);
    }

    /** 本文の前に「[探索レース]」を付ける。 */
    static Component prefixed(String legacy) {
        return prefix().append(LEGACY.deserialize(legacy));
    }

    /** 本文の前に「[探索レース]」を付ける。アイテム名などを差し込むときに使う。 */
    static Component prefixed(Component body) {
        return prefix().append(body);
    }

    private static Component prefix() {
        return LEGACY.deserialize("&8[&b探索レース&8] &r");
    }
}
