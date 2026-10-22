package dev.spa.resourcerace;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

/** /race（状況表示）、/race start <world>、/race stop、/race reload。 */
final class RaceCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "resourcerace.admin";

    private final ResourceRacePlugin plugin;
    private final RaceManager race;

    RaceCommand(ResourceRacePlugin plugin, RaceManager race) {
        this.plugin = plugin;
        this.race = race;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "" -> race.showInfo(sender);
            case "start" -> start(sender, args);
            case "stop" -> stop(sender);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(Text.prefixed("&7使い方: /race [start <ワールド>|stop|reload]"));
        }
        return true;
    }

    private void start(CommandSender sender, String[] args) {
        if (!allowed(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.prefixed("&7使い方: /race start <ワールド>"));
            return;
        }
        String world = args[1];
        String error = race.start(world);
        if (error == null) {
            sender.sendMessage("RESOURCE_RACE_OK started world=" + world + " item=" + race.state().item()
                    + " amount=" + race.state().amount() + " race=" + race.state().raceId());
        } else {
            sender.sendMessage("RESOURCE_RACE_ERROR " + error);
        }
    }

    private void stop(CommandSender sender) {
        if (!allowed(sender)) {
            return;
        }
        sender.sendMessage(race.stop() ? "RESOURCE_RACE_OK stopped" : "RESOURCE_RACE_ERROR no-race");
    }

    private void reload(CommandSender sender) {
        if (!allowed(sender)) {
            return;
        }
        plugin.reloadConfig();
        plugin.reloadRaceConfig();
        sender.sendMessage("RESOURCE_RACE_OK reloaded");
    }

    private boolean allowed(CommandSender sender) {
        if (sender.hasPermission(PERMISSION)) {
            return true;
        }
        sender.sendMessage(Text.prefixed("&cこのコマンドを使う権限がありません。"));
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "reload").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return race.config().worldNames().stream()
                    .filter(world -> world.startsWith(args[1]))
                    .toList();
        }
        return List.of();
    }
}
