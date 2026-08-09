package org.ThienNguyen.GemSocket;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class GemType {

    /**
     * Tìm loại lỗ (socket type) mà 1 drillId thuộc về, dựa trên type.yml
     * (đã gộp DucLo.yml vào). Trả về null nếu không tìm thấy — KHÔNG fallback
     * ngầm về "common". Dùng chung cho GemDucLo và bất kỳ lệnh tạo/give drill nào.
     */
    public static String resolveSocketTypeForDrill(String drillId) {
        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();
        for (String type : typeConfig.getKeys(false)) {
            if (type.equals("general")) continue;
            if (typeConfig.contains(type + ".drills." + drillId)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Trả về đường dẫn config đầy đủ của 1 drillId trong type.yml
     * (vd: "legendary.drills.DRILL_LEGENDARY"), hoặc null nếu không tìm thấy.
     * Dùng đường dẫn này để đọc cost/success/model-id/material/display-name/lore,
     * nhưng khi lưu vào PDC (gem_item_id) vẫn phải dùng drillId gốc (ngắn),
     * không phải đường dẫn đầy đủ này.
     */
    public static String resolveDrillPath(String drillId) {
        String type = resolveSocketTypeForDrill(drillId);
        return type == null ? null : type + ".drills." + drillId;
    }

    /**
     * Liệt kê tất cả drillId có trong type.yml (gộp từ mọi loại lỗ).
     * Dùng cho tab-completion và các chỗ cần liệt kê toàn bộ mũi khoan.
     */
    public static java.util.List<String> getAllDrillIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();
        for (String type : typeConfig.getKeys(false)) {
            if (type.equals("general")) continue;
            if (typeConfig.contains(type + ".drills")) {
                var section = typeConfig.getConfigurationSection(type + ".drills");
                if (section != null) ids.addAll(section.getKeys(false));
            }
        }
        return ids;
    }

    public static String getSocketFormat(String socketData) {
        FileConfiguration config = Main.getInstance().getGemTypeConfig();


        if (socketData.startsWith("EMPTY_")) {
            String type = socketData.replace("EMPTY_", "");
            String format = config.getString(type + ".format", "&7[ ○ ] Lỗ trống");
            return ChatColor.translateAlternateColorCodes('&', format);
        }


        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
        if (gemConfig.contains(socketData)) {
            String gemName = gemConfig.getString(socketData + ".display-name");
            return ChatColor.translateAlternateColorCodes('&', "&f[ ● ] " + gemName);
        }

        return "§8[ ○ ] Lỗ trống";
    }
}