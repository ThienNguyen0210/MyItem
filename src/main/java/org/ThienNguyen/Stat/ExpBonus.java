package org.ThienNguyen.Stat;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ExpBonus {
    // Khởi tạo Key để lưu vào PersistentDataContainer của Item
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "exp_bonus");

    /**
     * Lấy giá trị Exp Bonus từ Item
     */
    public static double get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(KEY, PersistentDataType.DOUBLE, 0.0);
    }

    /**
     * Lưu giá trị Exp Bonus vào Item
     * Đây là hàm mà class Stats.java đang bị thiếu
     */
    public static void set(ItemStack item, double value) {
        if (item == null || item.getType().isAir()) return;

        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.DOUBLE, value);
        });
    }
}