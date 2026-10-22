package dev.spa.resourcerace;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * レース1本の進行。開始・判定・順位確定・報酬・終了を持つ。
 *
 * 状態は RaceStore に保存し、判定と報酬を分けて二重払いを防ぐ。
 */
final class RaceManager {

    private final JavaPlugin plugin;
    private final MarkService marks;
    private final RaceStore store;
    private final EconomyService economy;
    private final Random random = new Random();

    private RaceConfig config;
    private RaceState state;

    RaceManager(JavaPlugin plugin, RaceConfig config, MarkService marks, RaceStore store, EconomyService economy) {
        this.plugin = plugin;
        this.config = config;
        this.marks = marks;
        this.store = store;
        this.economy = economy;
        this.state = store.load();
    }

    RaceConfig config() {
        return config;
    }

    void setConfig(RaceConfig config) {
        this.config = config;
    }

    /** 終了済みも含めた、いま保存しているレース。無ければ null。 */
    RaceState state() {
        return state;
    }

    boolean active() {
        return state != null && !state.finished();
    }

    /** 今のレースのID。終了済み・レース無しなら null。印の掃除に使う。 */
    UUID currentRaceId() {
        return active() ? state.raceId() : null;
    }

    Material targetMaterial() {
        return state == null ? null : Material.getMaterial(state.item());
    }

    /** お題の説明。「資源サーバー2でダイヤモンドを5個集めろ」のような Component。 */
    Component describe() {
        Material material = targetMaterial();
        Component item = material == null ? Component.text(state.item()) : Component.translatable(material.translationKey());
        return Component.text(displayName() + "で").append(item).append(Component.text("を" + state.amount() + "個集めろ"));
    }

    String displayName() {
        RaceConfig.WorldTargets targets = config.world(state.world());
        return targets == null ? state.world() : targets.displayName();
    }

    /** 指定した在庫の中の、自分の印が付いたお題アイテムの合計。 */
    int score(Inventory inventory, UUID player) {
        if (!active()) {
            return 0;
        }
        Material material = targetMaterial();
        if (material == null) {
            return 0;
        }
        int total = marks.count(inventory, player, state.raceId(), material);
        if (inventory instanceof PlayerInventory playerInventory) {
            total += marks.countStack(playerInventory.getItemInOffHand(), player, state.raceId(), material);
        }
        return total;
    }

    /** 成功したら null、失敗したら RESOURCE_RACE_ERROR に続く理由。 */
    String start(String world) {
        if (!config.hasWorld(world)) {
            return "world-not-configured";
        }
        RaceConfig.WorldTargets targets = config.world(world);
        if (targets.targets().isEmpty()) {
            return "no-targets";
        }
        if (active()) {
            finish();
        }
        TargetSpec target = targets.targets().get(random.nextInt(targets.targets().size()));
        state = RaceState.start(world, target.item(), target.amount(), Instant.now(), config.durationHours());
        store.save(state);
        announceStart();
        return null;
    }

    /** 進行中のレースを終了する。無ければ false。 */
    boolean stop() {
        if (!active()) {
            return false;
        }
        finish();
        return true;
    }

    /** 期限切れ・順位が埋まったレースを終了し、未払いの報酬を払う。1分ごとに呼ぶ。 */
    void tick() {
        if (state == null) {
            return;
        }
        if (active() && !Instant.now().isBefore(state.endsAt())) {
            finish();
        }
        if (active() && state.isFull(config.rewards().size())) {
            finish();
        }
        payUnpaid();
    }

    /** 持ち物を見て、お題を集めきっていたら順位を確定する。 */
    void judge(Player player) {
        if (!active()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        // 持ち主がいない印は、持ち物に入れた時点でその人に書き換える。
        marks.claimAll(player.getInventory(), uuid, state.raceId());
        marks.claim(player.getInventory().getItemInOffHand(), uuid, state.raceId());
        GameMode mode = player.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
            return;
        }
        if (!player.getWorld().getName().equals(state.world())) {
            return;
        }
        if (state.hasStanding(uuid)) {
            return;
        }
        Material material = targetMaterial();
        if (material == null) {
            return;
        }
        if (score(player.getInventory(), uuid) >= state.amount()) {
            award(player);
        }
    }

    /** 順位を確定して報酬を払う。確定できなければ false。 */
    boolean award(OfflinePlayer player) {
        if (!active()) {
            return false;
        }
        int maxRanks = config.rewards().size();
        UUID uuid = player.getUniqueId();
        if (state.isFull(maxRanks) || state.hasStanding(uuid)) {
            return false;
        }
        String name = player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
        int rank = state.place(uuid, name, Instant.now(), maxRanks);
        if (rank == 0) {
            return false;
        }
        // 先に順位を残してから払う。落ちても、払い終えたら paid が立つ。
        store.save(state);
        announcePlacement(state.standing(uuid));
        pay(state.standing(uuid));
        if (state.isFull(maxRanks)) {
            finish();
        }
        return true;
    }

    /** ログインした人に、お題・期限・進み具合を見せて、古い印を掃除する。 */
    void onJoin(Player player) {
        clearMarks(player);
        if (active()) {
            player.sendMessage(Text.prefixed(describe()));
            player.sendMessage(Text.of("&7期限: &f" + Deadline.format(state.endsAt())
                    + " &7進み具合: &f" + score(player.getInventory(), player.getUniqueId()) + "/" + state.amount()));
        }
        judge(player);
    }

    /** 開いたコンテナと自分の持ち物から古い印を消す。 */
    void clearStale(Inventory inventory) {
        marks.clearStale(inventory, currentRaceId());
    }

    /** 自分の持ち物とエンダーチェストから古い印を消す。 */
    void clearMarks(Player player) {
        UUID current = currentRaceId();
        marks.clearStale(player.getInventory(), current);
        marks.clearIfStale(player.getInventory().getItemInOffHand(), current);
        marks.clearStale(player.getEnderChest(), current);
    }

    /** /race の表示。 */
    void showInfo(CommandSender sender) {
        if (!active()) {
            sender.sendMessage(Text.prefixed("いまレースはありません。"));
            return;
        }
        sender.sendMessage(Text.prefixed(describe()));
        sender.sendMessage(Text.of("&7対象ワールド: &f" + displayName()));
        sender.sendMessage(Text.of("&7期限: &f" + Deadline.format(state.endsAt())));
        if (sender instanceof Player player) {
            sender.sendMessage(Text.of("&7あなたの進み具合: &f"
                    + score(player.getInventory(), player.getUniqueId()) + "/" + state.amount()));
        }
        List<RaceState.Standing> standings = state.standings();
        if (standings.isEmpty()) {
            sender.sendMessage(Text.of("&7順位: &fまだ誰も達成していません"));
            return;
        }
        sender.sendMessage(Text.of("&7順位:"));
        for (RaceState.Standing standing : standings) {
            sender.sendMessage(Text.of("&f" + standing.rank() + "位: " + standing.name()));
        }
    }

    private void finish() {
        if (state == null) {
            return;
        }
        if (!state.finished()) {
            state.finish();
            store.save(state);
            announceEnd();
        }
        clearMarksOnline();
    }

    private void clearMarksOnline() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            clearMarks(player);
        }
    }

    private void payUnpaid() {
        if (state == null) {
            return;
        }
        for (RaceState.Standing standing : state.unpaid()) {
            if (standing.rank() >= 1 && standing.rank() <= config.rewards().size()) {
                pay(standing);
            }
        }
    }

    private void pay(RaceState.Standing standing) {
        if (standing.paid()) {
            return;
        }
        double reward = config.reward(standing.rank());
        OfflinePlayer target = Bukkit.getOfflinePlayer(standing.uuid());
        if (economy.deposit(target, reward)) {
            if (state.markPaid(standing.uuid())) {
                store.save(state);
            }
        } else {
            plugin.getLogger().warning("報酬を払えませんでした: " + standing.name() + "（" + standing.rank()
                    + "位、" + RaceConfig.formatReward(reward) + "スパコイン）");
        }
    }

    private void announceStart() {
        Component body = Component.text("探索レース開始！ ").append(describe());
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(Text.prefixed(body));
            player.showTitle(Title.title(Text.of("&b探索レース開始！"), describe()));
        }
        plugin.getLogger().info("探索レースを開始しました: " + state.world() + " / " + state.item()
                + " x" + state.amount() + " / " + state.raceId());
    }

    private void announcePlacement(RaceState.Standing standing) {
        double reward = config.reward(standing.rank());
        Component body = Component.text(standing.rank() + "位: " + standing.name()
                + "（報酬 " + RaceConfig.formatReward(reward) + "スパコイン）");
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(Text.prefixed(body));
        }
        Player winner = Bukkit.getPlayer(standing.uuid());
        if (winner != null) {
            winner.showTitle(Title.title(Text.of("&b" + standing.rank() + "位！"),
                    Text.of("報酬 " + RaceConfig.formatReward(reward) + "スパコイン")));
        }
    }

    private void announceEnd() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(Text.prefixed("レースは終了しました。"));
            for (RaceState.Standing standing : state.standings()) {
                player.sendMessage(Text.of("&7" + standing.rank() + "位: &f" + standing.name()));
            }
        }
    }
}
