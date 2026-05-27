package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class LevelRequire {
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "level_require");

    public static int get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        
        if (pdc.has(KEY, PersistentDataType.DOUBLE)) {
            Double val = pdc.get(KEY, PersistentDataType.DOUBLE);
            return val != null ? val.intValue() : 0;
        }

        
        return pdc.getOrDefault(KEY, PersistentDataType.INTEGER, 0);
    }

    public static void set(ItemStack item, int value) {
        if (item == null) return;
        
        item.editMeta(meta -> meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, value));
    }
}