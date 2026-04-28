package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class AllDamage {
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "all_damage");

    public static double get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Double val = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.DOUBLE);
        return val != null ? val : 0;
    }

    public static void set(ItemStack item, double value) {
        if (item == null) return;
        var meta = item.getItemMeta();
        if (meta == null) return;
        if (value == 0) meta.getPersistentDataContainer().remove(KEY);
        else meta.getPersistentDataContainer().set(KEY, PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
    }
}