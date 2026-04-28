package org.ThienNguyen.Utils;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Tooltips {

    private static final NamespacedKey ORIGINAL_LORE_KEY = new NamespacedKey(Main.getInstance(), "original_lore");
    private static final NamespacedKey ORIGINAL_NAME_KEY = new NamespacedKey(Main.getInstance(), "original_name");

    private static final String SEPARATOR = "§|§line§|§";

    public static void applyTooltip(Player player, String type) {
        Main plugin = Main.getInstance();
        FileConfiguration config = plugin.getTooltipConfig();

        ConfigurationSection section = config.getConfigurationSection("types." + type.toLowerCase());
        if (section == null) {
            player.sendMessage("§cLoại Tooltip '" + type + "' không tồn tại!");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§cBạn phải cầm vật phẩm trên tay!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 1. LƯU DỮ LIỆU GỐC (Cải thiện để giữ empty lines)
        if (!meta.getPersistentDataContainer().has(ORIGINAL_NAME_KEY, PersistentDataType.STRING)) {
            meta.getPersistentDataContainer().set(ORIGINAL_NAME_KEY, PersistentDataType.STRING,
                    meta.hasDisplayName() ? meta.getDisplayName() : "");

            List<String> originalLoreList = meta.hasLore() && meta.getLore() != null
                    ? meta.getLore() : new ArrayList<>();

            String loreData = String.join(SEPARATOR, originalLoreList);
            meta.getPersistentDataContainer().set(ORIGINAL_LORE_KEY, PersistentDataType.STRING, loreData);
        }

        // 2. LẤY DỮ LIỆU GỐC
        String rawName = meta.getPersistentDataContainer().get(ORIGINAL_NAME_KEY, PersistentDataType.STRING);
        String rawLore = meta.getPersistentDataContainer().get(ORIGINAL_LORE_KEY, PersistentDataType.STRING);

        List<String> oldLore = new ArrayList<>();
        if (rawLore != null && !rawLore.isEmpty()) {
            String[] parts = rawLore.split(Pattern.quote(SEPARATOR));
            for (String part : parts) {
                oldLore.add(part); // Giữ luôn cả dòng trống
            }
        }

        // 3. SETTINGS
        String globalFill = config.getString("settings.fill-character", "");
        int globalLeft = config.getInt("settings.fill-count-left", 1);
        int globalRight = config.getInt("settings.fill-count-right", 39);

        String topIcon = section.getString("top", "");
        String midIcon = section.getString("mid", "");
        String botIcon = section.getString("bottom", "");

        // 4. XỬ LÝ TÊN
        String displayName = (rawName == null || rawName.isEmpty()) ? "§fVật phẩm" : rawName;
        String finalNewName = buildLine(globalFill, globalLeft, globalRight, topIcon, displayName);

        // 5. XỬ LÝ LORE + THÊM EMPTY LINES
        List<String> newLore = new ArrayList<>();
        ConfigurationSection customLinesSection = section.getConfigurationSection("custom-lines");

        for (int i = 0; i < oldLore.size(); i++) {
            String lineText = oldLore.get(i);
            int lineNumber = i + 1;

            String currentIcon = midIcon;
            String currentFill = globalFill;
            int currentLeft = globalLeft;
            int currentRight = globalRight;

            if (customLinesSection != null && customLinesSection.contains(String.valueOf(lineNumber))) {
                if (customLinesSection.isString(String.valueOf(lineNumber))) {
                    currentIcon = customLinesSection.getString(String.valueOf(lineNumber));
                } else if (customLinesSection.isConfigurationSection(String.valueOf(lineNumber))) {
                    ConfigurationSection lineConf = customLinesSection.getConfigurationSection(String.valueOf(lineNumber));
                    if (lineConf != null) {
                        currentIcon = lineConf.getString("icon", midIcon);
                        currentFill = lineConf.getString("fill-character", globalFill);
                        currentLeft = lineConf.getInt("fill-count-left", globalLeft);
                        currentRight = lineConf.getInt("fill-count-right", globalRight);
                    }
                }
            }

            newLore.add(buildLine(currentFill, currentLeft, currentRight, currentIcon, lineText));
        }

        // Thêm dòng phân cách và bottom
        newLore.add(buildLine(globalFill, globalLeft, globalRight, midIcon, " "));
        newLore.add(buildLine(globalFill, globalLeft, globalRight, botIcon, " "));

        // === QUAN TRỌNG: ẨN THUỘC TÍNH VANILLA ===
        meta.setDisplayName(finalNewName);
        meta.setLore(newLore);

        // Ẩn tất cả thuộc tính mặc định của item
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);

        // Hỗ trợ thêm cho phiên bản mới
        try {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_STORED_ENCHANTS"));
        } catch (Exception ignored) {}

        item.setItemMeta(meta);
        player.sendMessage("§a§l✔ §7Đã áp dụng Tooltip thành công.");
    }
    public static void reapplyTooltipSilent(ItemStack item, String type) {
        if (item == null || !item.hasItemMeta()) return;

        Main plugin = Main.getInstance();
        FileConfiguration config = plugin.getTooltipConfig();

        ConfigurationSection section = config.getConfigurationSection("types." + type.toLowerCase());
        if (section == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Lấy cấu hình chung
        String globalFill = config.getString("settings.fill-character", "");
        int globalLeft = config.getInt("settings.fill-count-left", 1);
        int globalRight = config.getInt("settings.fill-count-right", 39);

        // Lấy icon của tooltip type
        String topIcon = section.getString("top", "");
        String midIcon = section.getString("mid", "");
        String botIcon = section.getString("bottom", "");

        // 1. Xử lý Tên Item (Gắn icon TOP)
        String currentName = meta.hasDisplayName() ? meta.getDisplayName() : "§fVật Phẩm";
        meta.setDisplayName(buildLine(globalFill, globalLeft, globalRight, topIcon, currentName));

        // 2. Xử lý Lore (Gắn icon MID và BOTTOM)
        List<String> currentLore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        List<String> newLore = new ArrayList<>();

        // Thêm lore nội dung với icon MID
        for (String line : currentLore) {
            // Tránh null, nếu dòng trống thì dùng khoảng trắng để icon mid vẫn hiển thị đẹp
            String content = (line == null || line.trim().isEmpty()) ? " " : line;
            newLore.add(buildLine(globalFill, globalLeft, globalRight, midIcon, content));
        }

        // Dòng đệm cuối trước khi đóng (tùy chọn, giúp tooltip thoáng hơn)
        newLore.add(buildLine(globalFill, globalLeft, globalRight, midIcon, " "));

        // 3. Bottom border (Dòng cuối cùng đóng tooltip)
        newLore.add(buildLine(globalFill, globalLeft, globalRight, botIcon, " "));

        meta.setLore(newLore);

        // Ẩn thuộc tính vanilla
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM,
                ItemFlag.HIDE_POTION_EFFECTS
        );

        try {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_STORED_ENCHANTS"));
        } catch (Exception ignored) {}

        item.setItemMeta(meta);
    }
    public static void handleUndo(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(ORIGINAL_NAME_KEY, PersistentDataType.STRING)) {
            player.sendMessage("§cVật phẩm này không có dữ liệu gốc!");
            return;
        }

        String oldName = meta.getPersistentDataContainer().get(ORIGINAL_NAME_KEY, PersistentDataType.STRING);
        meta.setDisplayName(oldName.isEmpty() ? null : oldName);

        String rawLore = meta.getPersistentDataContainer().get(ORIGINAL_LORE_KEY, PersistentDataType.STRING);
        if (rawLore == null || rawLore.isEmpty()) {
            meta.setLore(null);
        } else {
            List<String> recoveredLore = Arrays.stream(rawLore.split(Pattern.quote(SEPARATOR)))
                    .collect(Collectors.toList()); // Giữ cả dòng trống
            meta.setLore(recoveredLore);
        }

        // Xóa hết flag ẩn
        for (ItemFlag flag : ItemFlag.values()) {
            meta.removeItemFlags(flag);
        }

        meta.getPersistentDataContainer().remove(ORIGINAL_NAME_KEY);
        meta.getPersistentDataContainer().remove(ORIGINAL_LORE_KEY);

        item.setItemMeta(meta);
        player.sendMessage("§a§l✔ §7Đã khôi phục trạng thái gốc.");
    }

    private static String buildLine(String fill, int left, int right, String icon, String content) {
        String leftPad = (fill != null) ? fill.repeat(left) : "";
        String rightPad = (fill != null) ? fill.repeat(right) : "";
        String coloredContent = ChatColor.translateAlternateColorCodes('&', content);

        // Nếu content là dòng trống thì chỉ trả về padding + icon
        if (content.trim().isEmpty()) {
            return leftPad + "§f" + icon + rightPad;
        }

        return leftPad + "§f" + icon + rightPad + "§f" + coloredContent;
    }
}