package dev.spa.resourcerace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 読み込み済みの設定。Bukkit に依存しない。 */
final class RaceConfig {

    /** 1ワールドぶんの候補。 */
    record WorldTargets(String displayName, List<TargetSpec> targets) {
    }

    private final Map<String, WorldTargets> worlds;
    private final List<Double> rewards;
    private final int durationHours;

    private RaceConfig(Map<String, WorldTargets> worlds, List<Double> rewards, int durationHours) {
        this.worlds = worlds;
        this.rewards = rewards;
        this.durationHours = durationHours;
    }

    static RaceConfig of(Map<String, WorldTargets> worlds, List<Double> rewards, int durationHours) {
        return new RaceConfig(new LinkedHashMap<>(worlds), List.copyOf(rewards), durationHours);
    }

    boolean hasWorld(String world) {
        return worlds.containsKey(world);
    }

    WorldTargets world(String world) {
        return worlds.get(world);
    }

    List<String> worldNames() {
        return List.copyOf(worlds.keySet());
    }

    /** 順位ごとの報酬。要素数が順位の数になる。 */
    List<Double> rewards() {
        return rewards;
    }

    double reward(int rank) {
        return rewards.get(rank - 1);
    }

    int durationHours() {
        return durationHours;
    }

    /** 「1,000」のように表示する。 */
    static String formatReward(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.format(Locale.JAPAN, "%,d", (long) amount);
        }
        return String.format(Locale.JAPAN, "%,.2f", amount);
    }
}
