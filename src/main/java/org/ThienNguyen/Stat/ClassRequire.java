package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ClassRequire {
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "class_require");

    public static String get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(KEY, PersistentDataType.STRING, "");
    }

    public static void set(ItemStack item, String value) {
        if (item == null) return;
        item.editMeta(meta -> meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, value));
    }
}