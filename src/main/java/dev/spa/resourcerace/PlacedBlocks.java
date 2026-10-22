package dev.spa.resourcerace;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * プレイヤーが置いたブロックの位置を、チャンクの PersistentDataContainer に記録する。
 *
 * 置いたブロックを壊したときのドロップには印を付けない（無限増殖を防ぐため）。
 * ワールドをリセットするとチャンクごと消えるので、掃除は要らない。
 */
final class PlacedBlocks {

    private final NamespacedKey key;

    PlacedBlocks(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "placed");
    }

    boolean isPlaced(Block block) {
        String raw = container(block).get(key, PersistentDataType.STRING);
        return raw != null && contains(raw, position(block));
    }

    void record(Block block) {
        PersistentDataContainer container = container(block);
        String raw = container.get(key, PersistentDataType.STRING);
        String position = position(block);
        if (raw == null || raw.isEmpty()) {
            container.set(key, PersistentDataType.STRING, position);
        } else if (!contains(raw, position)) {
            container.set(key, PersistentDataType.STRING, raw + ";" + position);
        }
    }

    void forget(Block block) {
        PersistentDataContainer container = container(block);
        String raw = container.get(key, PersistentDataType.STRING);
        if (raw == null) {
            return;
        }
        String position = position(block);
        StringBuilder builder = new StringBuilder();
        for (String entry : raw.split(";")) {
            if (entry.equals(position)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(entry);
        }
        if (builder.length() == 0) {
            container.remove(key);
        } else {
            container.set(key, PersistentDataType.STRING, builder.toString());
        }
    }

    private static PersistentDataContainer container(Block block) {
        return block.getChunk().getPersistentDataContainer();
    }

    private static boolean contains(String raw, String position) {
        return (";" + raw + ";").contains(";" + position + ";");
    }

    private static String position(Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
