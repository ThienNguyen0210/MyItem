package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Damage {
    private static final NamespacedKey DAMAGE_KEY = new NamespacedKey(Main.getInstance(), "damage");

    public static void setDamage(ItemStack item, double value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(DAMAGE_KEY, PersistentDataType.DOUBLE, value);
            item.setItemMeta(meta);
        }
    }

    public static double getDamage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(DAMAGE_KEY, PersistentDataType.DOUBLE, 0.0);
    }
}