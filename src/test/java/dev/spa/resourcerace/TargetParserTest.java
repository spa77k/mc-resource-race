package dev.spa.resourcerace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TargetParserTest {

    private static final Set<String> KNOWN = Set.of("DIAMOND", "TRIDENT", "EMERALD");

    private static List<TargetSpec> parse(List<?> raw, List<String> warnings) {
        return TargetParser.parse(raw, KNOWN::contains, warnings::add);
    }

    @Test
    void 正しい候補を読む() {
        List<String> warnings = new ArrayList<>();
        List<TargetSpec> targets = parse(List.of(
                Map.of("item", "DIAMOND", "amount", 5),
                Map.of("item", "emerald", "amount", 10)), warnings);
        assertEquals(List.of(new TargetSpec("DIAMOND", 5), new TargetSpec("EMERALD", 10)), targets);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void 知らないアイテムは警告して除外する() {
        List<String> warnings = new ArrayList<>();
        List<TargetSpec> targets = parse(List.of(
                Map.of("item", "DIAMOND", "amount", 1),
                Map.of("item", "UNOBTAINIUM", "amount", 1)), warnings);
        assertEquals(List.of(new TargetSpec("DIAMOND", 1)), targets);
        assertEquals(1, warnings.size());
    }

    @Test
    void amountが1未満や数値でない候補は除外する() {
        List<String> warnings = new ArrayList<>();
        List<TargetSpec> targets = parse(List.of(
                Map.of("item", "DIAMOND", "amount", 0),
                Map.of("item", "TRIDENT", "amount", -3),
                Map.of("item", "EMERALD", "amount", "たくさん"),
                Map.of("item", "DIAMOND", "amount", 2)), warnings);
        assertEquals(List.of(new TargetSpec("DIAMOND", 2)), targets);
        assertEquals(3, warnings.size());
    }

    @Test
    void itemが無いか形が違う候補は除外する() {
        List<String> warnings = new ArrayList<>();
        List<TargetSpec> targets = parse(List.of(
                Map.of("amount", 1),
                Map.of("item", "  ", "amount", 1),
                "DIAMOND",
                Map.of("item", "DIAMOND", "amount", 1)), warnings);
        assertEquals(List.of(new TargetSpec("DIAMOND", 1)), targets);
        assertEquals(3, warnings.size());
    }

    @Test
    void 候補が無いときは空になる() {
        assertEquals(List.of(), parse(null, new ArrayList<>()));
        assertEquals(List.of(), parse(List.of(), new ArrayList<>()));
    }
}
