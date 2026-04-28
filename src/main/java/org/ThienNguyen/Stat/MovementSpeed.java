package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MovementSpeed {
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "movement_speed");

    public static double get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        Double val = meta.getPersistentDataContainer().get(KEY, PersistentDataType.DOUBLE);
        return val != null ? val : 0;
    }

    // THIẾU HÀM NÀY NÊN BỊ LỖI COMPILATION ERROR
    public static void set(ItemStack item, double value) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (value == 0) {
            meta.getPersistentDataContainer().remove(KEY);
        } else {
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.DOUBLE, value);
        }
        item.setItemMeta(meta);
    }
}