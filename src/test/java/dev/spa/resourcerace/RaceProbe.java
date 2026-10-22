package dev.spa.resourcerace;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 隔離Paperで、レースの開始・保存・印・数え上げ・順位確定・報酬の二重払い防止・終了を確かめる。
 *
 * コンソール出力の RESOURCE_RACE_OK / RESOURCE_RACE_ERROR は scripts/test-race-paper.py が見る。
 * 実際の入手イベントや画面表示は実クライアントで別に確かめる。
 */
public final class RaceProbe extends JavaPlugin {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("RaceProbe-Alice".getBytes());
    private static final UUID BOB = UUID.nameUUIDFromBytes("RaceProbe-Bob".getBytes());
    private static final UUID CAROL = UUID.nameUUIDFromBytes("RaceProbe-Carol".getBytes());
    private static final UUID DAVE = UUID.nameUUIDFromBytes("RaceProbe-Dave".getBytes());

    private JavaPlugin race;
    private ClassLoader loader;
    private Object marks;

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                runProbe();
                getLogger().info("RACE_PROBE_PASS");
            } catch (Throwable error) {
                getLogger().log(Level.SEVERE, "RACE_PROBE_FAIL", error);
            } finally {
                Bukkit.shutdown();
            }
        }, 40);
    }

    private static void check(boolean result, String message) {
        if (!result) {
            throw new AssertionError(message);
        }
        Bukkit.getLogger().info("ok: " + message);
    }

    private static Object call(Object target, String name, Object... args) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                }
            }
        }
        throw new NoSuchMethodException(name);
    }

    private Object newMark(UUID raceId, UUID owner) throws Exception {
        Class<?> mark = Class.forName("dev.spa.resourcerace.Mark", true, loader);
        Constructor<?> constructor = mark.getDeclaredConstructor(UUID.class, UUID.class);
        constructor.setAccessible(true);
        return constructor.newInstance(raceId, owner);
    }

    private ItemStack marked(Material material, int amount, UUID raceId, UUID owner) throws Exception {
        ItemStack stack = new ItemStack(material, amount);
        call(marks, "write", stack, newMark(raceId, owner));
        return stack;
    }

    private void runProbe() throws Exception {
        race = (JavaPlugin) Bukkit.getPluginManager().getPlugin("ResourceRace");
        check(race != null && race.isEnabled(), "ResourceRace が有効");
        loader = race.getClass().getClassLoader();
        Object manager = call(race, "race");
        marks = call(race, "marks");
        CommandSender console = Bukkit.getConsoleSender();

        // コンソールから開始し、state.yml に残る
        Bukkit.dispatchCommand(console, "race start resource2");
        check((boolean) call(manager, "active"), "コンソールから resource2 を開始できる");
        Object state = call(manager, "state");
        UUID raceId = (UUID) call(state, "raceId");
        Material target = (Material) call(manager, "targetMaterial");
        check(target != null, "お題のMaterialを読める");
        File stateFile = new File(race.getDataFolder(), "state.yml");
        check(stateFile.isFile(), "state.yml が保存される");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        check("resource2".equals(yaml.getString("race.world")), "state.yml にワールドが残る");
        check(raceId.toString().equals(yaml.getString("race.race-id")), "state.yml に race-id が残る");
        check(yaml.getInt("race.amount") >= 1, "state.yml に amount が残る");
        check(yaml.getString("race.ends-at") != null, "state.yml に期限が残る");

        // 設定に無いワールドは開始せず、今のレースも変えない
        Bukkit.dispatchCommand(console, "race start no_such_world");
        check("world-not-configured".equals(call(manager, "start", "no_such_world")),
                "無いワールドは world-not-configured");
        check(raceId.equals(call(call(manager, "state"), "raceId")), "失敗しても今のレースは変わらない");

        // 印を付けて読み直し、* を受け取る
        ItemStack diamond = new ItemStack(Material.DIAMOND, 3);
        call(marks, "write", diamond, newMark(raceId, null));
        Object read = call(marks, "read", diamond);
        check(read != null && (boolean) call(read, "isWildcard"), "印を付けて読み直せる");
        call(marks, "claim", diamond, ALICE, raceId);
        Object claimed = call(marks, "read", diamond);
        check((boolean) call(claimed, "isOwnedBy", ALICE), "* の印は持ち物に入れた人のUUIDになる");

        // 古い印の掃除
        ItemStack old = marked(Material.DIAMOND, 1, UUID.randomUUID(), ALICE);
        Inventory cleanup = Bukkit.createInventory(null, 36);
        cleanup.setItem(0, old);
        call(marks, "clearStale", cleanup, raceId);
        check(call(marks, "read", cleanup.getItem(0)) == null, "古い印は掃除される");

        // 自分の今のレースのお題だけを数える
        Material other = target == Material.DIAMOND ? Material.EMERALD : Material.DIAMOND;
        Inventory inventory = Bukkit.createInventory(null, 36);
        inventory.setItem(0, marked(target, 2, raceId, ALICE));
        inventory.setItem(1, marked(target, 4, raceId, BOB));
        inventory.setItem(2, marked(target, 8, UUID.randomUUID(), ALICE));
        inventory.setItem(3, new ItemStack(target, 16));
        inventory.setItem(4, marked(other, 32, raceId, ALICE));
        check((int) call(manager, "score", inventory, ALICE) == 2,
                "他人の印・古い印・印なし・別のアイテムを数えない");

        // 順位確定と報酬
        Economy economy = economy();
        OfflinePlayer alice = Bukkit.getOfflinePlayer(ALICE);
        double before = economy.getBalance(alice);
        check((boolean) call(manager, "award", alice), "1位を確定できる");
        check(Math.abs(economy.getBalance(alice) - (before + 1000)) < 0.001, "1位の報酬1000が入る");
        check(!(boolean) call(manager, "award", alice), "同じ人を二度確定しない");
        check(Math.abs(economy.getBalance(alice) - (before + 1000)) < 0.001, "同じ人に二重に払わない");

        OfflinePlayer bob = Bukkit.getOfflinePlayer(BOB);
        OfflinePlayer carol = Bukkit.getOfflinePlayer(CAROL);
        OfflinePlayer dave = Bukkit.getOfflinePlayer(DAVE);
        double bobBefore = economy.getBalance(bob);
        double carolBefore = economy.getBalance(carol);
        check((boolean) call(manager, "award", bob), "2位を確定できる");
        check(Math.abs(economy.getBalance(bob) - (bobBefore + 500)) < 0.001, "2位の報酬500が入る");
        check((boolean) call(manager, "award", carol), "3位を確定できる");
        check(Math.abs(economy.getBalance(carol) - (carolBefore + 300)) < 0.001, "3位の報酬300が入る");
        check(!(boolean) call(manager, "active"), "最後の順位が埋まるとレースは終了する");
        check(!(boolean) call(manager, "award", dave), "報酬の数を超える順位は確定できない");

        // 開始と終了
        Bukkit.dispatchCommand(console, "race start resource_nether2");
        check((boolean) call(manager, "active"), "次のレースを開始できる");
        Bukkit.dispatchCommand(console, "race stop");
        check(!(boolean) call(manager, "active"), "stop でレースを終了できる");
        Bukkit.dispatchCommand(console, "race stop");
        check(!(boolean) call(manager, "active"), "レースが無いときも stop は落ちない");
        Bukkit.dispatchCommand(console, "race");
    }

    private Economy economy() {
        RegisteredServiceProvider<Economy> provider =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            throw new IllegalStateException("Vault の経済プラグインがありません");
        }
        return provider.getProvider();
    }
}
