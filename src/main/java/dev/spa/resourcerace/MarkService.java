package dev.spa.resourcerace;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * アイテムの PersistentDataContainer に印を付けたり読んだりする。
 *
 * 印は "&lt;race-id&gt;:&lt;owner&gt;" の文字列で、owner はプレイヤーUUIDか、持ち主がいないことを表す "*"。
 * 古いレースの印は消して、普通のアイテムとスタックできる状態に戻す。
 */
final class MarkService {

    private final NamespacedKey key;

    MarkService(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "mark");
    }

    Mark read(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return Mark.decode(meta.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    void write(ItemStack item, Mark mark) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        item.editMeta(meta -> meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mark.encode()));
    }

    void clear(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        item.editMeta(meta -> meta.getPersistentDataContainer().remove(key));
    }

    /** 今のレースと違う印（今のレースが無いときはすべての印）を消す。 */
    void clearIfStale(ItemStack item, UUID currentRaceId) {
        Mark mark = read(item);
        if (mark != null && !mark.isCurrent(currentRaceId)) {
            clear(item);
        }
    }

    /** 持ち物・開いたコンテナなどの在庫から、古い印をまとめて消す。 */
    void clearStale(Inventory inventory, UUID currentRaceId) {
        if (inventory == null) {
            return;
        }
        for (ItemStack item : inventory.getStorageContents()) {
            clearIfStale(item, currentRaceId);
        }
    }

    /** "*" の印を持ち物に入れたとき、そのプレイヤーのUUIDへ書き換える。 */
    void claim(ItemStack item, UUID owner, UUID currentRaceId) {
        Mark mark = read(item);
        if (mark != null && mark.isCurrent(currentRaceId) && mark.isWildcard()) {
            write(item, mark.ownedBy(owner));
        }
    }

    /** 在庫の中の "*" の印をまとめて受け取る。 */
    void claimAll(Inventory inventory, UUID owner, UUID currentRaceId) {
        if (inventory == null) {
            return;
        }
        for (ItemStack item : inventory.getStorageContents()) {
            claim(item, owner, currentRaceId);
        }
    }

    /** お題のMaterialで、今のレース・指定プレイヤーの印が付いたアイテムの合計。 */
    int count(Inventory inventory, UUID player, UUID currentRaceId, Material material) {
        if (inventory == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            total += countStack(item, player, currentRaceId, material);
        }
        return total;
    }

    /** 1つのアイテム山を数える。 */
    int countStack(ItemStack item, UUID player, UUID currentRaceId, Material material) {
        if (item == null || item.getType() != material) {
            return 0;
        }
        Mark mark = read(item);
        if (mark != null && mark.isCurrent(currentRaceId) && mark.isOwnedBy(player)) {
            return item.getAmount();
        }
        return 0;
    }

    /** オフハンドも含めて数える。 */
    int countWithOffhand(PlayerInventory inventory, UUID player, UUID currentRaceId, Material material) {
        return count(inventory, player, currentRaceId, material)
                + countStack(inventory.getItemInOffHand(), player, currentRaceId, material);
    }
}
