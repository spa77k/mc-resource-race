package dev.spa.resourcerace;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * レース中、対象ワールドで起きた入手に印を付ける。
 *
 * 付けるのは、ブロックのドロップ・モブのドロップ・戦利品・釣り・ピグリンの物々交換・
 * 自然生成の額縁の中身・かまどの結果・作業台の結果。
 * プレイヤーが置いたブロック・コンテナ・捨てた物を拾ったモブ・プレイヤーが入れた額縁は除外する。
 */
final class RaceListener implements Listener {

    private final JavaPlugin plugin;
    private final RaceManager race;
    private final MarkService marks;
    private final PlacedBlocks placed;
    private final NamespacedKey pickedKey;
    private final NamespacedKey framePlacedKey;
    private final NamespacedKey frameContentKey;
    /** BlockBreakEvent と BlockDropItemEvent のあいだだけ、置いたブロックだったかを覚えておく。 */
    private final Set<String> justBrokenPlaced = new HashSet<>();

    RaceListener(JavaPlugin plugin, RaceManager race, MarkService marks, PlacedBlocks placed) {
        this.plugin = plugin;
        this.race = race;
        this.marks = marks;
        this.placed = placed;
        this.pickedKey = new NamespacedKey(plugin, "picked");
        this.framePlacedKey = new NamespacedKey(plugin, "frame-placed");
        this.frameContentKey = new NamespacedKey(plugin, "frame-content");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (race.config().hasWorld(event.getBlock().getWorld().getName())) {
            placed.record(event.getBlockPlaced());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!placed.isPlaced(event.getBlock())) {
            return;
        }
        String key = key(event.getBlock());
        justBrokenPlaced.add(key);
        placed.forget(event.getBlock());
        plugin.getServer().getScheduler().runTask(plugin, () -> justBrokenPlaced.remove(key));
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) {
        Block block = event.getBlock();
        boolean placedBlock = justBrokenPlaced.remove(key(block)) || placed.isPlaced(block);
        if (!inRace(block.getWorld().getName()) || placedBlock || event.getBlockState() instanceof Container) {
            return;
        }
        Mark mark = new Mark(race.state().raceId(), event.getPlayer().getUniqueId());
        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            marks.write(stack, mark);
            item.setItemStack(stack);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || !inRace(entity.getWorld().getName())) {
            return;
        }
        // プレイヤーの捨てた物や印のない物を拾っていたモブのドロップは数えない。
        if (entity.getPersistentDataContainer().has(pickedKey, PersistentDataType.BYTE)) {
            return;
        }
        Mark mark = Mark.wildcard(race.state().raceId());
        for (ItemStack stack : event.getDrops()) {
            marks.write(stack, mark);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            scheduleJudge(player);
            return;
        }
        if (inRace(entity.getWorld().getName())) {
            entity.getPersistentDataContainer().set(pickedKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (!inRace(event.getWorld().getName())) {
            return;
        }
        Mark mark = Mark.wildcard(race.state().raceId());
        for (ItemStack stack : event.getLoot()) {
            marks.write(stack, mark);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                || !(event.getCaught() instanceof Item caught)
                || !inRace(event.getPlayer().getWorld().getName())) {
            return;
        }
        ItemStack stack = caught.getItemStack();
        marks.write(stack, new Mark(race.state().raceId(), event.getPlayer().getUniqueId()));
        caught.setItemStack(stack);
    }

    @EventHandler
    public void onBarter(PiglinBarterEvent event) {
        if (!inRace(event.getEntity().getWorld().getName())) {
            return;
        }
        Mark mark = Mark.wildcard(race.state().raceId());
        for (ItemStack stack : event.getOutcome()) {
            marks.write(stack, mark);
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getEntity() instanceof ItemFrame) {
            event.getEntity().getPersistentDataContainer().set(framePlacedKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    @EventHandler
    public void onInteractFrame(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) {
            return;
        }
        ItemStack hand = event.getPlayer().getInventory().getItem(event.getHand());
        if (hand != null && !hand.getType().isAir() && frame.getItem().getType().isAir()) {
            frame.getPersistentDataContainer().set(frameContentKey, PersistentDataType.BYTE, (byte) 1);
        }
        // 額縁から中身を取ったときも、持ち主がいない印を受け取れるようにする。
        scheduleJudge(event.getPlayer());
    }

    @EventHandler
    public void onFrameDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame frame && isPlayer(event.getDamager())) {
            markFrameItem(frame);
        }
    }

    @EventHandler
    public void onFrameBreak(HangingBreakByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame frame && isPlayer(event.getRemover())) {
            markFrameItem(frame);
        }
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (!inRace(event.getBlock().getWorld().getName())) {
            return;
        }
        Mark mark = marks.read(event.getSource());
        if (mark != null && mark.isCurrent(race.state().raceId())) {
            marks.write(event.getResult(), mark);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        markCraft(event.getInventory(), event.getView().getPlayer() instanceof Player player ? player : null);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        markCraft(event.getInventory(), event.getWhoClicked() instanceof Player player ? player : null);
        if (event.getWhoClicked() instanceof Player player) {
            scheduleJudge(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleJudge(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleJudge(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            scheduleJudge(player);
        }
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        scheduleJudge(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        race.clearStale(event.getInventory());
        if (event.getPlayer() instanceof Player player) {
            race.clearMarks(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> race.onJoin(player));
    }

    /** 自然生成の額縁の中身に、まだ持ち主がいない印を付ける。 */
    private void markFrameItem(ItemFrame frame) {
        if (!inRace(frame.getWorld().getName())
                || frame.getPersistentDataContainer().has(framePlacedKey, PersistentDataType.BYTE)
                || frame.getPersistentDataContainer().has(frameContentKey, PersistentDataType.BYTE)) {
            return;
        }
        ItemStack stack = frame.getItem();
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        marks.write(stack, Mark.wildcard(race.state().raceId()));
        frame.setItem(stack);
    }

    /** 材料のうち空でないものがすべて同じ持ち主の印付きなら、結果にも同じ印を付ける。 */
    private void markCraft(CraftingInventory inventory, Player player) {
        if (player == null || !inRace(player.getWorld().getName())) {
            return;
        }
        ItemStack result = inventory.getResult();
        if (result == null || result.getType().isAir()) {
            return;
        }
        Mark owner = null;
        for (ItemStack item : inventory.getMatrix()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Mark mark = marks.read(item);
            if (mark == null || !mark.isCurrent(race.state().raceId()) || mark.isWildcard()) {
                return;
            }
            if (owner == null) {
                owner = mark;
            } else if (!owner.owner().equals(mark.owner())) {
                return;
            }
        }
        if (owner != null) {
            marks.write(result, owner);
            inventory.setResult(result);
        }
    }

    private boolean inRace(String world) {
        return race.active() && race.state().world().equals(world);
    }

    private static boolean isPlayer(Entity entity) {
        if (entity instanceof Player) {
            return true;
        }
        return entity instanceof Projectile projectile && projectile.getShooter() instanceof Player;
    }

    private void scheduleJudge(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                race.judge(player);
            }
        });
    }

    private static String key(Block block) {
        return block.getWorld().getName() + "@" + block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
