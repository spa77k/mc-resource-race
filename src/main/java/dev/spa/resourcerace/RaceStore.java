package dev.spa.resourcerace;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * plugins/ResourceRace/state.yml への保存と読み込み。
 *
 * サーバーは毎日停止・再起動されるので、レースと順位をここに残して続きから動かす。
 * 順位を確定して保存してから報酬を払い、払えたら paid を立てる。
 */
final class RaceStore {

    private final JavaPlugin plugin;
    private final File file;

    RaceStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
    }

    /** 保存されているレースが無いときは null。 */
    RaceState load() {
        if (!file.isFile()) {
            return null;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String raceId = yaml.getString("race.race-id");
        String world = yaml.getString("race.world");
        String item = yaml.getString("race.item");
        if (raceId == null || world == null || item == null) {
            return null;
        }
        Instant startedAt = parse(yaml.getString("race.started-at"));
        Instant endsAt = parse(yaml.getString("race.ends-at"));
        if (startedAt == null || endsAt == null) {
            plugin.getLogger().warning("state.yml の日時を読めませんでした。レースを読み込みません。");
            return null;
        }
        RaceState state;
        try {
            state = RaceState.restored(UUID.fromString(raceId), world, item,
                    yaml.getInt("race.amount"), startedAt, endsAt, yaml.getBoolean("race.finished"));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("state.yml の race-id を読めませんでした。レースを読み込みません。");
            return null;
        }
        for (Map<?, ?> raw : yaml.getMapList("standings")) {
            UUID uuid = uuid(raw.get("uuid"));
            Instant at = parse(String.valueOf(raw.get("at")));
            if (uuid == null || at == null) {
                continue;
            }
            Object rank = raw.get("rank");
            Object name = raw.get("name");
            Object paid = raw.get("paid");
            state.restoreStanding(new RaceState.Standing(
                    rank instanceof Number number ? number.intValue() : 0,
                    uuid,
                    name == null ? "" : String.valueOf(name),
                    at,
                    paid instanceof Boolean value && value));
        }
        return state;
    }

    void save(RaceState state) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("race.race-id", state.raceId().toString());
        yaml.set("race.world", state.world());
        yaml.set("race.item", state.item());
        yaml.set("race.amount", state.amount());
        yaml.set("race.started-at", state.startedAt().toString());
        yaml.set("race.ends-at", state.endsAt().toString());
        yaml.set("race.finished", state.finished());
        List<Map<String, Object>> standings = new java.util.ArrayList<>();
        for (RaceState.Standing standing : state.standings()) {
            standings.add(Map.of(
                    "rank", standing.rank(),
                    "uuid", standing.uuid().toString(),
                    "name", standing.name(),
                    "at", standing.at().toString(),
                    "paid", standing.paid()));
        }
        yaml.set("standings", standings);
        try {
            if (!plugin.getDataFolder().isDirectory()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("state.yml を保存できませんでした: " + exception.getMessage());
        }
    }

    private static Instant parse(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static UUID uuid(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
