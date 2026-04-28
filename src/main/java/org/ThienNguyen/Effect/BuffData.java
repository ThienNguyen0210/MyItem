package org.ThienNguyen.Effect;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.HashMap;
import java.util.Map;

public class BuffData {
    private static final NamespacedKey EFFECTS_KEY = new NamespacedKey(Main.getInstance(), "item_effects_map");

    // Lưu hiệu ứng vào PDC theo dạng Map (Tên hiệu ứng:Level)
    public static void setEffect(ItemStack item, String effectName, int level) {
        if (item == null || item.getItemMeta() == null) return;
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        // Lấy data hiện tại hoặc tạo mới
        String currentData = pdc.getOrDefault(EFFECTS_KEY, PersistentDataType.STRING, "");
        Map<String, Integer> effects = deserialize(currentData);

        if (level <= 0) effects.remove(effectName.toUpperCase());
        else effects.put(effectName.toUpperCase(), level);

        pdc.set(EFFECTS_KEY, PersistentDataType.STRING, serialize(effects));
        item.setItemMeta(meta);
    }

    public static Map<String, Integer> getEffects(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return new HashMap<>();
        String data = item.getItemMeta().getPersistentDataContainer().get(EFFECTS_KEY, PersistentDataType.STRING);
        return deserialize(data != null ? data : "");
    }

    private static String serialize(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append(":").append(v).append(";"));
        return sb.toString();
    }

    private static Map<String, Integer> deserialize(String data) {
        Map<String, Integer> map = new HashMap<>();
        if (data.isEmpty()) return map;
        for (String entry : data.split(";")) {
            String[] split = entry.split(":");
            if (split.length == 2) map.put(split[0], Integer.parseInt(split[1]));
        }
        return map;
    }
}