package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class DeepWound {
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "deep_wound");

    public static void set(ItemStack item, double value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
    }

    public static double get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        Double val = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.DOUBLE);
        return val != null ? val : 0.0;
    }

    public static void remove(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(KEY);
        item.setItemMeta(meta);
    }
}