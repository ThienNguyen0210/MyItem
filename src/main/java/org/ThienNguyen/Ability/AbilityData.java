    package org.ThienNguyen.Ability;

    import org.ThienNguyen.Main;
    import org.bukkit.NamespacedKey;
    import org.bukkit.inventory.ItemStack;
    import org.bukkit.persistence.PersistentDataContainer;
    import org.bukkit.persistence.PersistentDataType;

    import java.util.ArrayList;
    import java.util.List;

    public class AbilityData {
        // Chúng ta nên dùng tên key là "item_abilities" (số nhiều) để phân biệt
        private static final NamespacedKey ABILITY_KEY = new NamespacedKey(Main.getInstance(), "item_abilities");


        public static void setAbility(ItemStack item, String abilityName, int level, double chance) {
            if (item == null || item.getItemMeta() == null) return;
            var meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            String currentData = pdc.get(ABILITY_KEY, PersistentDataType.STRING);
            String newAbilityEntry = abilityName.toUpperCase() + ":" + level + ":" + chance;

            if (currentData == null || currentData.isEmpty()) {
                // Nếu chưa có kỹ năng nào, lưu mới luôn
                pdc.set(ABILITY_KEY, PersistentDataType.STRING, newAbilityEntry);
            } else {
                // Nếu đã có, kiểm tra xem kỹ năng này đã tồn tại trong danh sách chưa
                String[] abilities = currentData.split(",");
                boolean found = false;
                StringBuilder finalData = new StringBuilder();

                for (String entry : abilities) {
                    if (entry.startsWith(abilityName.toUpperCase() + ":")) {
                        // Nếu trùng tên, thay thế bằng dữ liệu mới (Level/Chance mới)
                        finalData.append(newAbilityEntry);
                        found = true;
                    } else {
                        finalData.append(entry);
                    }
                    finalData.append(",");
                }

                if (!found) {
                    // Nếu chưa có kỹ năng này trong danh sách, thêm nó vào cuối
                    finalData.append(newAbilityEntry);
                }

                // Xóa dấu phẩy thừa ở cuối nếu có
                String result = finalData.toString();
                if (result.endsWith(",")) result = result.substring(0, result.length() - 1);

                pdc.set(ABILITY_KEY, PersistentDataType.STRING, result);
            }

            item.setItemMeta(meta);
        }

        /**
         * Lấy toàn bộ chuỗi chứa tất cả kỹ năng
         */
        public static String getRawAbility(ItemStack item) {
            if (item == null || !item.hasItemMeta()) return null;
            return item.getItemMeta().getPersistentDataContainer().get(ABILITY_KEY, PersistentDataType.STRING);
        }

        /**
         * Lấy danh sách các kỹ năng đã tách rời để dễ xử lý trong Event
         */
        public static List<String> getAbilityList(ItemStack item) {
            String raw = getRawAbility(item);
            if (raw == null || raw.isEmpty()) return new ArrayList<>();
            return List.of(raw.split(","));
        }
    }