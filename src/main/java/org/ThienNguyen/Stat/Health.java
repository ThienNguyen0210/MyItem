package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Health {
    private static final NamespacedKey HEALTH_KEY = new NamespacedKey(Main.getInstance(), "health");

    public static void setHealth(ItemStack item, double value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(HEALTH_KEY, PersistentDataType.DOUBLE, value);
            item.setItemMeta(meta);
        }
    }

    public static double getHealth(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(HEALTH_KEY, PersistentDataType.DOUBLE, 0.0);
    }
}