package org.ThienNguyen.Utils;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class Upgrade {

    private static final NamespacedKey LEVEL_KEY = new NamespacedKey(Main.getInstance(), "upgrade_level");
    private static final NamespacedKey GEM_ID_KEY = new NamespacedKey(Main.getInstance(), "gem_id");
    private static final NamespacedKey PROTECT_KEY = new NamespacedKey(Main.getInstance(), "protection_scroll");
    private static final NamespacedKey BASE_NAME_KEY = new NamespacedKey(Main.getInstance(), "base_name");
    private final Map<Integer, GemConfig> gemConfigs = new HashMap<>();
    private final Map<Integer, Double> levelChances = new HashMap<>();
    private final Map<String, Double> statMultipliers = new HashMap<>();
    private final TreeMap<Integer, String> colorLevels = new TreeMap<>();
    private final Map<Integer, Double> upgradeCosts = new HashMap<>();
    private double defaultCost;
    private String displayFormat;
    private int maxLevel;
    private double defaultChance;

    public Upgrade() {
        loadConfig();
    }

    public void loadConfig() {
        // 1. Load Gem.yml
        FileConfiguration gConfig = Main.getInstance().getUpgradeGemConfig();
        gemConfigs.clear();
        if (gConfig != null) {
            for (String key : gConfig.getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    ConfigurationSection sec = gConfig.getConfigurationSection(key);
                    gemConfigs.put(id, new GemConfig(
                            sec.getDouble("chance-per-level", 0.0),
                            Material.matchMaterial(sec.getString("material", "EMERALD")),
                            sec.getString("display-name", "&fĐá Cường Hóa")
                    ));
                } catch (Exception ignored) {}
            }
        }

        // 2. Load upgrade.yml
        File folder = new File(Main.getInstance().getDataFolder(), "Upgrade");
        File upgradeFile = new File(folder, "upgrade.yml");
        if (!upgradeFile.exists()) Main.getInstance().saveResource("Upgrade/upgrade.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(upgradeFile);

        displayFormat = config.getString("display-format", "%name% %color%[+ %level%]");
        maxLevel = config.getInt("max-level", 15);

        levelChances.clear();
        ConfigurationSection lvSec = config.getConfigurationSection("levels");
        if (lvSec != null) {
            for (String key : lvSec.getKeys(false)) {
                if (!key.equals("default-chance")) {
                    try {
                        levelChances.put(Integer.parseInt(key), lvSec.getDouble(key));
                    } catch (NumberFormatException ignored) {}
                }
            }
            defaultChance = lvSec.getDouble("default-chance", 30.0);
        }

        statMultipliers.clear();
        ConfigurationSection statSec = config.getConfigurationSection("upgrade-stats");
        if (statSec != null) {
            for (String stat : statSec.getKeys(false)) {
                statMultipliers.put(stat, statSec.getDouble(stat));
            }
        }

        colorLevels.clear();
        ConfigurationSection colorSec = config.getConfigurationSection("color-levels");
        if (colorSec != null) {
            for (String key : colorSec.getKeys(false)) {
                try {
                    colorLevels.put(Integer.parseInt(key), colorSec.getString(key));
                } catch (NumberFormatException ignored) {}
            }
        }
        upgradeCosts.clear();
        ConfigurationSection costSec = config.getConfigurationSection("upgrade-costs.levels");
        if (costSec != null) {
            for (String key : costSec.getKeys(false)) {
                try {
                    upgradeCosts.put(Integer.parseInt(key), costSec.getDouble(key));
                } catch (NumberFormatException ignored) {}
            }
        }
        defaultCost = config.getDouble("upgrade-costs.default-cost", 500.0);
    }

    // --- LOGIC CƯỜNG HÓA ---
    public boolean processUpgrade(Player player, ItemStack item, ItemStack gem, ItemStack protect) {
        var lang = Main.getInstance().getLangManager();
        int currentLevel = getItemLevel(item);
        int nextLevel = currentLevel + 1;

        // 1. Kiểm tra cấp độ tối đa
        if (currentLevel >= maxLevel) {
            player.sendMessage(lang.getMessage("upgrade.max-level"));
            return false;
        }

        // 2. Tính toán chi phí và kiểm tra tiền (Vault)
        double cost = upgradeCosts.getOrDefault(nextLevel, defaultCost);
        var eco = Main.getEconomy(); // Sử dụng hàm hook Vault đã tạo ở class Main

        if (eco != null && eco.getBalance(player) < cost) {
            player.sendMessage(lang.getMessage("upgrade.not-enough-money",
                    "{cost}", String.valueOf((int)cost)));
            return false;
        }

        // 3. Chuẩn bị thông số
        boolean hasProtection = isProtectionScroll(protect);
        double finalChance = calculateFinalChance(item, gem);

        // 4. Trừ tiền và thông báo (Nếu có Vault)
        if (eco != null) {
            eco.withdrawPlayer(player, cost);
            player.sendMessage(lang.getMessage("upgrade.pay-success",
                    "{cost}", String.valueOf((int)cost)));
        }

        // 5. Trừ nguyên liệu
        gem.setAmount(gem.getAmount() - 1);
        if (hasProtection) {
            protect.setAmount(protect.getAmount() - 1);
        }

        // 6. Xử lý xác suất
        if (new Random().nextDouble() * 100 < finalChance) {
            // --- THÀNH CÔNG ---
            applyStats(item, true);
            setItemLevel(item, nextLevel);
            updateItemName(item, nextLevel);
            org.ThienNguyen.Lore.StatsLore.updateLore(item);

            player.sendMessage(lang.getMessage("upgrade.success",
                    "{level}", String.valueOf(nextLevel),
                    "{chance}", String.valueOf((int)finalChance),
                    "{cost}", String.valueOf((int)cost)));
            return true;
        } else {
            // --- THẤT BẠI ---
            if (hasProtection) {
                player.sendMessage(lang.getMessage("upgrade.protected",
                        "{cost}", String.valueOf((int)cost)));
                return false;
            }

            if (currentLevel > 0) {
                applyStats(item, false);
                int backLevel = currentLevel - 1;
                setItemLevel(item, backLevel);
                updateItemName(item, backLevel);
                org.ThienNguyen.Lore.StatsLore.updateLore(item);

                String levelStr = (backLevel == 0) ? lang.getMessage("upgrade.base-level-name") : "+" + backLevel;
                player.sendMessage(lang.getMessage("upgrade.failed-down",
                        "{level}", levelStr,
                        "{chance}", String.valueOf((int)finalChance),
                        "{cost}", String.valueOf((int)cost)));
            } else {
                player.sendMessage(lang.getMessage("upgrade.failed-keep",
                        "{chance}", String.valueOf((int)finalChance),
                        "{cost}", String.valueOf((int)cost)));
            }
            return false;
        }
    }

    // --- HÀM TẠO VẬT PHẨM TỪ CONFIG ---

    public static ItemStack createGemFromConfig(int id) {
        FileConfiguration config = Main.getInstance().getUpgradeGemConfig();
        if (config == null) return null;

        String path = String.valueOf(id);
        ConfigurationSection sec = config.getConfigurationSection(path);

        // Duyệt tìm ID nếu không thấy Section trực tiếp
        if (sec == null) {
            for (String key : config.getKeys(false)) {
                try {
                    if (Integer.parseInt(key) == id) {
                        sec = config.getConfigurationSection(key);
                        break;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (sec == null) return null;

        // --- ĐOẠN SỬA LỖI TẠI ĐÂY ---
        // 1. Lấy tên Material từ config
        String materialName = sec.getString("material", "EMERALD");
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) mat = Material.EMERALD;

        // 2. Lấy số lượng (amount) từ config, mặc định là 1 nếu không có
        int amount = sec.getInt("amount", 1);

        // 3. Khởi tạo ItemStack với Material và Amount (Hết lỗi "cannot find symbol mat")
        ItemStack item = new ItemStack(mat, amount);
        // ----------------------------

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', sec.getString("display-name", "&fĐá Cường Hóa")));
            List<String> lore = new ArrayList<>();
            for (String line : sec.getStringList("lore")) lore.add(ChatColor.translateAlternateColorCodes('&', line));
            meta.setLore(lore);

            if (sec.contains("custom-model-data")) {
                meta.setCustomModelData(sec.getInt("custom-model-data"));
            }

            meta.getPersistentDataContainer().set(GEM_ID_KEY, PersistentDataType.INTEGER, id);
            item.setItemMeta(meta);
        }
        return item;
    }
    /**
     * Tạo vật phẩm Bùa Hộ Mệnh lấy từ cấu hình đã nạp trong Main
     */
    public static ItemStack createProtectionScroll() {
        // Lấy trực tiếp FileConfiguration đã được Main nạp sẵn
        FileConfiguration config = Main.getInstance().getProtectionConfig();

        if (config == null) {
            Main.getInstance().getLogger().warning("Khong the tim thay cau hinh protection.yml trong Main!");
            return null;
        }

        ConfigurationSection sec = config.getConfigurationSection("protection-scroll");
        if (sec == null) return null;

        // Đọc Material từ config
        Material mat = Material.matchMaterial(sec.getString("material", "PAPER"));
        ItemStack item = new ItemStack(mat != null ? mat : Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set Name
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    sec.getString("display-name", "&bBùa Hộ Mệnh")));

            // Set Lore
            List<String> lore = new ArrayList<>();
            for (String line : sec.getStringList("lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);

            // Set Custom Model Data (nếu có)
            if (sec.contains("custom-model-data")) {
                meta.setCustomModelData(sec.getInt("custom-model-data"));
            }

            // QUAN TRỌNG: Đánh dấu ID ẩn để hệ thống nhận diện đây là Bùa
            meta.getPersistentDataContainer().set(PROTECT_KEY, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    // --- CÁC HÀM HỖ TRỢ ---

    public boolean isProtectionScroll(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(PROTECT_KEY, PersistentDataType.BYTE);
    }

    public double calculateFinalChance(ItemStack item, ItemStack gem) {
        int currentLevel = getItemLevel(item);
        int gemId = getGemId(gem);
        GemConfig gConfig = gemConfigs.get(gemId);
        double baseChance = levelChances.getOrDefault(currentLevel + 1, defaultChance);
        double bonusChance = (gConfig != null) ? (gConfig.chancePerLevel * currentLevel) : 0;
        return baseChance + bonusChance;
    }

    public void applyStats(ItemStack item, boolean success) {
        item.editMeta(meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (Map.Entry<String, Double> entry : statMultipliers.entrySet()) {
                NamespacedKey key = new NamespacedKey(Main.getInstance(), entry.getKey());
                if (pdc.has(key, PersistentDataType.DOUBLE)) {
                    double currentVal = pdc.get(key, PersistentDataType.DOUBLE);
                    double percent = entry.getValue();
                    double newVal = success ? (currentVal * (1.0 + percent)) : (currentVal / (1.0 + percent));
                    pdc.set(key, PersistentDataType.DOUBLE, Math.round(newVal * 100.0) / 100.0);
                }
            }
        });
    }

    public int getGemId(ItemStack gem) {
        if (gem == null || !gem.hasItemMeta()) return 0;
        return gem.getItemMeta().getPersistentDataContainer().getOrDefault(GEM_ID_KEY, PersistentDataType.INTEGER, 0);
    }

    public int getItemLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }

    private void setItemLevel(ItemStack item, int level) {
        item.editMeta(meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (level <= 0) {
                // Nếu level là 0, xóa hoàn toàn Key khỏi PDC
                pdc.remove(LEVEL_KEY);
            } else {
                // Nếu level > 0, cập nhật giá trị như bình thường
                pdc.set(LEVEL_KEY, PersistentDataType.INTEGER, level);
            }
        });
    }

    public void updateItemName(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String baseName;

        // 1. Kiểm tra/Lấy base_name gốc
        if (pdc.has(BASE_NAME_KEY, PersistentDataType.STRING)) {
            baseName = pdc.get(BASE_NAME_KEY, PersistentDataType.STRING);
        } else {
            // Lấy tên hiện tại và làm sạch bằng Regex nếu chưa có trong PDC
            String currentName = meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();
            baseName = currentName.replaceAll("(?i)\\s*([§&][0-9a-fk-or])*([\\[\\(]).*?\\+.*([\\]\\)])", "")
                    .replaceAll("(?i)\\s*([§&][0-9a-fk-or])*\\+.*", "")
                    .trim();

            // Chỉ lưu vào PDC nếu level > 0 (đang trong quá trình cường hóa)
            if (level > 0) {
                pdc.set(BASE_NAME_KEY, PersistentDataType.STRING, baseName);
            }
        }

        // 2. Xử lý hiển thị và dọn dẹp PDC
        if (level <= 0) {
            // TRƯỜNG HỢP VỀ 0: Xóa sạch dữ liệu cường hóa
            pdc.remove(BASE_NAME_KEY);
            pdc.remove(LEVEL_KEY); // LEVEL_KEY khai báo ở đầu class Upgrade

            // Trả tên về nguyên bản (xử lý mã màu nếu baseName có chứa)
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', baseName));

            // Nếu muốn xóa luôn cả Lore chỉ số khi về 0, bạn có thể gọi thêm:
            // meta.setLore(null);
        } else {
            // TRƯỜNG HỢP CÓ CẤP ĐỘ: Cập nhật tên theo format
            String lvColor = "&a";
            Map.Entry<Integer, String> entry = colorLevels.floorEntry(level);
            if (entry != null) lvColor = entry.getValue();

            String formattedLevel = lvColor + level;

            // Render tên mới từ format trong config
            String finalName = displayFormat.replace("%name%", baseName)
                    .replace("%level%", formattedLevel);

            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', finalName));

            // Đảm bảo Level được lưu lại vào PDC
            pdc.set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        }

        item.setItemMeta(meta);
    }
    public boolean isValidGem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        // Kiểm tra xem item có chứa GEM_ID_KEY hay không
        return item.getItemMeta().getPersistentDataContainer().has(GEM_ID_KEY, PersistentDataType.INTEGER);
    }
    private static class GemConfig {
        double chancePerLevel;
        Material material;
        String displayName;
        GemConfig(double cpl, Material mat, String dn) {
            this.chancePerLevel = cpl; this.material = mat; this.displayName = dn;
        }
    }
    public double getUpgradeCost(int nextLevel) {
        return upgradeCosts.getOrDefault(nextLevel, defaultCost);
    }


}