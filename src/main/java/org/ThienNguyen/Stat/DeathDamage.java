package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class DeathDamage {
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "death_damage");

    public static void set(ItemStack item, double value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.DOUBLE, value);
            item.setItemMeta(meta);
        }
    }

    public static double get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(KEY, PersistentDataType.DOUBLE, 0.0);
    }
}