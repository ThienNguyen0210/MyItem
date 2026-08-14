package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * "cooldown_reduction" stat.
 *
 * Stored on the item as a plain DOUBLE, exactly like the other stat classes
 * (Accuracy, Armor, CriticalChance, ...). The number itself is unit-less -
 * how many "points" of the stat translate into an actual percentage cooldown
 * reduction is decided entirely by the formula in
 * {@link org.ThienNguyen.Listener.CooldownReductionListener}, not here.
 */
public class CooldownReduction {

    private static final String KEY_NAME = "cooldown_reduction";

    public static double get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0.0;
        NamespacedKey key = new NamespacedKey(Main.getInstance(), KEY_NAME);
        Double value = meta.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return value != null ? value : 0.0;
    }

    public static void set(ItemStack item, double value) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        NamespacedKey key = new NamespacedKey(Main.getInstance(), KEY_NAME);
        meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
    }
}