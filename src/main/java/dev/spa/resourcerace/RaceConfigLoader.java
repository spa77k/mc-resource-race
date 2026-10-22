package dev.spa.resourcerace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** config.yml を読み込んで RaceConfig にする。 */
final class RaceConfigLoader {

    private RaceConfigLoader() {
    }

    static RaceConfig load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Map<String, RaceConfig.WorldTargets> worlds = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("worlds");
        if (section != null) {
            for (String world : section.getKeys(false)) {
                String displayName = section.getString(world + ".display-name", world);
                List<TargetSpec> targets = TargetParser.parse(
                        section.getMapList(world + ".targets"),
                        RaceConfigLoader::isKnownItem,
                        warning -> plugin.getLogger().warning(warning));
                worlds.put(world, new RaceConfig.WorldTargets(displayName, targets));
            }
        }
        List<Double> rewards = new ArrayList<>();
        for (Object value : config.getList("rewards", List.of())) {
            if (value instanceof Number number) {
                rewards.add(number.doubleValue());
            }
        }
        int durationHours = config.getInt("duration-hours", 167);
        return RaceConfig.of(worlds, rewards, durationHours);
    }

    private static boolean isKnownItem(String name) {
        Material material = Material.getMaterial(name);
        return material != null && material.isItem();
    }
}
