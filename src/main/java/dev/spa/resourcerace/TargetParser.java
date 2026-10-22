package dev.spa.resourcerace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * config.yml の候補リストを読む。Bukkit に依存しない。
 *
 * 不正な Material 名・amount が1未満の候補は警告して除外する。
 * knownItem は「その Material 名が存在してアイテムか」を答える。
 */
final class TargetParser {

    private TargetParser() {
    }

    static List<TargetSpec> parse(List<?> rawTargets, Predicate<String> knownItem, Consumer<String> warning) {
        List<TargetSpec> targets = new ArrayList<>();
        if (rawTargets == null) {
            return targets;
        }
        for (Object raw : rawTargets) {
            if (!(raw instanceof Map<?, ?> map)) {
                warning.accept("候補の書き方が不正です: " + raw);
                continue;
            }
            Object item = map.get("item");
            Object amount = map.get("amount");
            if (!(item instanceof String name) || name.isBlank()) {
                warning.accept("候補に item がありません: " + map);
                continue;
            }
            String material = name.trim().toUpperCase(Locale.ROOT);
            if (!knownItem.test(material)) {
                warning.accept("知らないアイテムです: " + name);
                continue;
            }
            if (!(amount instanceof Number number) || number.intValue() < 1) {
                warning.accept("候補の amount が1未満か数値ではありません: " + name + " / " + amount);
                continue;
            }
            targets.add(new TargetSpec(material, number.intValue()));
        }
        return targets;
    }
}
