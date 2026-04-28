package org.ThienNguyen.GemSocket;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;


import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class GemType {

    public static String getSocketFormat(String socketData) {
        FileConfiguration config = Main.getInstance().getGemTypeConfig();

        // Nếu là lỗ trống: EMPTY_common -> lấy format từ type.yml
        if (socketData.startsWith("EMPTY_")) {
            String type = socketData.replace("EMPTY_", "");
            String format = config.getString(type + ".format", "&7[ ○ ] Lỗ trống");
            return ChatColor.translateAlternateColorCodes('&', format);
        }

        // Nếu đã khảm đá: Lấy tên viên đá từ Gem.yml
        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
        if (gemConfig.contains(socketData)) {
            String gemName = gemConfig.getString(socketData + ".display-name");
            return ChatColor.translateAlternateColorCodes('&', "&f[ ● ] " + gemName);
        }

        return "§8[ ○ ] Lỗ trống";
    }
}