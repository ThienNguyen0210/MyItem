package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class KnockbackResistance {
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "knockback_resistance");

    public static double get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Double val = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.DOUBLE);
        return val != null ? val : 0;
    }

    public static void set(ItemStack item, double value) {
        if (item == null) return;
        var meta = item.getItemMeta();
        if (meta == null) return;
        // Giới hạn giá trị trong khoảng 0.0 - 1.0
        double finalValue = Math.max(0, Math.min(1, value));
        if (finalValue == 0) meta.getPersistentDataContainer().remove(KEY);
        else meta.getPersistentDataContainer().set(KEY, PersistentDataType.DOUBLE, finalValue);
        item.setItemMeta(meta);
    }
}