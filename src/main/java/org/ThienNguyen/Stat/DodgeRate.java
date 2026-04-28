package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class DodgeRate {
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "dodge_rate");

    public static double get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(KEY, PersistentDataType.DOUBLE, 0.0);
    }

    public static void set(ItemStack item, double value) {
        if (item == null) return;
        item.editMeta(meta -> meta.getPersistentDataContainer().set(KEY, PersistentDataType.DOUBLE, value));
    }
}