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

        // 1. Kiểm tra xem có phải là Double không (Lỗi do AI lưu 80.0)
        if (pdc.has(KEY, PersistentDataType.DOUBLE)) {
            Double val = pdc.get(KEY, PersistentDataType.DOUBLE);
            return val != null ? val.intValue() : 0;
        }

        // 2. Nếu không phải Double thì đọc theo Integer (Chuẩn cũ)
        return pdc.getOrDefault(KEY, PersistentDataType.INTEGER, 0);
    }

    public static void set(ItemStack item, int value) {
        if (item == null) return;
        // Luôn ép lưu về Integer để dọn dẹp dữ liệu
        item.editMeta(meta -> meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, value));
    }
}