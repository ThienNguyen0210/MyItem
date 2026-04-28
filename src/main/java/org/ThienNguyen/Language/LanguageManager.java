package org.ThienNguyen.Language;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final Main plugin;
    private FileConfiguration langConfig; // Đây là biến chính chứa dữ liệu
    private final Map<String, String> cache = new HashMap<>();

    public LanguageManager(Main plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        // 1. Lấy tên file từ config.yml (mặc định là vi.yml)
        String fileName = plugin.getConfig().getString("language", "vi.yml");

        // Đảm bảo đuôi file luôn đúng
        if (!fileName.endsWith(".yml")) fileName += ".yml";

        File langFile = new File(plugin.getDataFolder(), "Language/" + fileName);

        // 2. Nếu file không tồn tại, tạo file mẫu từ resource
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            // Lưu cả 2 file mẫu nếu chưa có
            File viFile = new File(plugin.getDataFolder(), "Language/vi.yml");
            File enFile = new File(plugin.getDataFolder(), "Language/en.yml");
            if (!viFile.exists()) plugin.saveResource("Language/vi.yml", false);
            if (!enFile.exists()) plugin.saveResource("Language/en.yml", false);
        }

        // QUAN TRỌNG: Gán vào biến langConfig
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
        cache.clear();

        // 3. Cache toàn bộ message
        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                cache.put(key, ChatColor.translateAlternateColorCodes('&', langConfig.getString(key)));
            }
        }
        plugin.getLogger().info("§a[Language] Đã nạp ngôn ngữ: " + fileName);
    }

    public String getMessage(String key) {
        return cache.getOrDefault(key, "§cMissing key: " + key);
    }

    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    // Sửa hàm này để trả về langConfig thay vì biến config bị null
    public FileConfiguration getConfig() {
        if (this.langConfig == null) {
            // Trả về cấu hình trống thay vì null để tránh lỗi crash server
            return new YamlConfiguration();
        }
        return this.langConfig;
    }
}