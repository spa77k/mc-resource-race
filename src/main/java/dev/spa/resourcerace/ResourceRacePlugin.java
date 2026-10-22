package dev.spa.resourcerace;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** 資源ワールドで指定アイテムを集めた順に順位を決め、スパコインを渡す探索レース。 */
public final class ResourceRacePlugin extends JavaPlugin {

    private EconomyService economy;
    private MarkService marks;
    private RaceManager race;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.economy = EconomyService.hook(this);
        if (economy == null) {
            getLogger().severe("Vault に経済プラグインが登録されていません。ResourceRace を無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.marks = new MarkService(this);
        PlacedBlocks placed = new PlacedBlocks(this);
        RaceStore store = new RaceStore(this);
        this.race = new RaceManager(this, RaceConfigLoader.load(this), marks, store, economy);

        getServer().getPluginManager().registerEvents(new RaceListener(this, race, marks, placed), this);
        RaceCommand command = new RaceCommand(this, race);
        PluginCommand registered = getCommand("race");
        if (registered == null) {
            getLogger().warning("コマンド race が plugin.yml にありません。");
        } else {
            registered.setExecutor(command);
            registered.setTabCompleter(command);
        }
        // 停止中に期限を過ぎていたら、起動して最初の確認で終了する。以後は1分ごと。
        getServer().getScheduler().runTask(this, race::tick);
        getServer().getScheduler().runTaskTimer(this, race::tick, 20L * 60, 20L * 60);

        RaceState state = race.state();
        if (race.active()) {
            getLogger().info("探索レース「" + state.world() + " / " + state.item() + " x" + state.amount()
                    + "」を読み込みました。");
        }
    }

    void reloadRaceConfig() {
        race.setConfig(RaceConfigLoader.load(this));
    }

    RaceManager race() {
        return race;
    }

    MarkService marks() {
        return marks;
    }

    EconomyService economy() {
        return economy;
    }
}
