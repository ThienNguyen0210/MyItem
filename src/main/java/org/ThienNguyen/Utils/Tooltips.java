package org.ThienNguyen.Utils;

import org.ThienNguyen.Main;
import org.ThienNguyen.Lore.LoreGenerator;
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
    private static final NamespacedKey TOOLTIP_TYPE_KEY = new NamespacedKey(Main.getInstance(), "tooltip_type");

    private static final String SEPARATOR = "§|§line§|§";

    private static void ensureOriginalDataSaved(ItemMeta meta) {
        if (!meta.getPersistentDataContainer().has(ORIGINAL_NAME_KEY, PersistentDataType.STRING)) {
            String name = meta.hasDisplayName() ? meta.getDisplayName() : "";
            meta.getPersistentDataContainer().set(ORIGINAL_NAME_KEY, PersistentDataType.STRING, name);

            List<String> lore = meta.hasLore() && meta.getLore() != null ? meta.getLore() : new ArrayList<>();
            String loreData = String.join(SEPARATOR, lore);
            meta.getPersistentDataContainer().set(ORIGINAL_LORE_KEY, PersistentDataType.STRING, loreData);
        }
    }

    public static void applyTooltip(Player player, String type) {
        Main plugin = Main.getInstance();
        FileConfiguration config = plugin.getTooltipConfig();

        ConfigurationSection section = config.getConfigurationSection("types." + type.toLowerCase());
        if (section == null) {
            player.sendMessage("§8[§bMyItem§8] §cTooltip type '" + type + "' does not exist!");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§8[§bMyItem§8] §cYou must hold an item in your hand!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(TOOLTIP_TYPE_KEY, PersistentDataType.STRING, type.toLowerCase());
        ensureOriginalDataSaved(meta);

        item.setItemMeta(meta);
        reapplyTooltipSilent(item, type);

        player.sendMessage("§8[§bMyItem§8] §a§l✔ §7Tooltip applied successfully.");
    }

    public static void reapplyTooltipSilent(ItemStack item, String type) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(TOOLTIP_TYPE_KEY, PersistentDataType.STRING, type.toLowerCase());
        ensureOriginalDataSaved(meta);

        item.setItemMeta(meta);

        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");
        if (meta.getPersistentDataContainer().has(formatKey, PersistentDataType.STRING)) {
            LoreGenerator.rebuild(item);
            return;
        }

        Main plugin = Main.getInstance();
        FileConfiguration config = plugin.getTooltipConfig();
        ConfigurationSection section = config.getConfigurationSection("types." + type.toLowerCase());
        if (section == null) return;

        meta = item.getItemMeta();
        if (meta == null) return;

        wrapMetaWithTooltip(meta, type);
        item.setItemMeta(meta);
    }

    /**
     * Hàm bọc thuần túy, bao gồm cả hỗ trợ custom-lines
     */
    public static void wrapMetaWithTooltip(ItemMeta meta, String type) {
        Main plugin = Main.getInstance();
        FileConfiguration config = plugin.getTooltipConfig();
        ConfigurationSection section = config.getConfigurationSection("types." + type.toLowerCase());
        if (section == null) return;

        String globalFill = config.getString("settings.fill-character", "");
        int globalLeft = config.getInt("settings.fill-count-left", 1);
        int globalRight = config.getInt("settings.fill-count-right", 39);

        String topIcon = section.getString("top", "");
        String midIcon = section.getString("mid", "");
        String botIcon = section.getString("bottom", "");

        // Lấy cấu hình custom-lines
        ConfigurationSection customLinesSection = section.getConfigurationSection("custom-lines");

        String rawName = meta.getPersistentDataContainer().get(ORIGINAL_NAME_KEY, PersistentDataType.STRING);
        String currentName = (rawName == null || rawName.isEmpty())
                ? (meta.hasDisplayName() ? meta.getDisplayName() : "§fVật Phẩm")
                : rawName;
        meta.setDisplayName(buildLine(globalFill, globalLeft, globalRight, topIcon, currentName));

        List<String> currentLore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        List<String> newLore = new ArrayList<>();

        // Duyệt qua từng dòng lore để xử lý custom-lines
        for (int i = 0; i < currentLore.size(); i++) {
            String lineText = currentLore.get(i);
            int lineNumber = i + 1; // Config đánh số từ 1

            String currentIcon = midIcon;
            String currentFill = globalFill;
            int currentLeft = globalLeft;
            int currentRight = globalRight;

            // Kiểm tra xem dòng hiện tại có bị custom hay không
            if (customLinesSection != null && customLinesSection.contains(String.valueOf(lineNumber))) {
                if (customLinesSection.isString(String.valueOf(lineNumber))) {
                    // Nếu cấu hình dạng: 1: "뀔"
                    currentIcon = customLinesSection.getString(String.valueOf(lineNumber));
                } else if (customLinesSection.isConfigurationSection(String.valueOf(lineNumber))) {
                    // Nếu cấu hình dạng:
                    // 1:
                    //   icon: "뀔"
                    //   fill-character: "..."
                    ConfigurationSection lineConf = customLinesSection.getConfigurationSection(String.valueOf(lineNumber));
                    if (lineConf != null) {
                        currentIcon = lineConf.getString("icon", midIcon);
                        currentFill = lineConf.getString("fill-character", globalFill);
                        currentLeft = lineConf.getInt("fill-count-left", globalLeft);
                        currentRight = lineConf.getInt("fill-count-right", globalRight);
                    }
                }
            }

            String content = (lineText == null || lineText.trim().isEmpty()) ? " " : lineText;

            // Build dòng với các giá trị có thể đã bị override bởi custom-lines
            String leftPad = (currentFill != null) ? currentFill.repeat(currentLeft) : "";
            String rightPad = (currentFill != null) ? currentFill.repeat(currentRight) : "";
            String coloredContent = ChatColor.translateAlternateColorCodes('&', content);

            if (content.trim().isEmpty()) {
                newLore.add(leftPad + "§f" + currentIcon + rightPad);
            } else {
                newLore.add(leftPad + "§f" + currentIcon + rightPad + "§f" + coloredContent);
            }
        }

        // Thêm 2 dòng footer
        newLore.add(buildLine(globalFill, globalLeft, globalRight, midIcon, " "));
        newLore.add(buildLine(globalFill, globalLeft, globalRight, botIcon, " "));

        meta.setLore(newLore);

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ARMOR_TRIM,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );

        try {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_STORED_ENCHANTS"));
        } catch (Exception ignored) {}
    }

    public static void handleUndo(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(TOOLTIP_TYPE_KEY, PersistentDataType.STRING)) {
            player.sendMessage("§cVật phẩm này không có dữ liệu gốc!");
            return;
        }

        meta.getPersistentDataContainer().remove(TOOLTIP_TYPE_KEY);

        boolean hasFormatId = meta.getPersistentDataContainer().has(new NamespacedKey(Main.getInstance(), "lore_format_id"), PersistentDataType.STRING);

        if (hasFormatId) {
            meta.getPersistentDataContainer().remove(ORIGINAL_NAME_KEY);
            meta.getPersistentDataContainer().remove(ORIGINAL_LORE_KEY);
            item.setItemMeta(meta);
            LoreGenerator.rebuild(item);
        } else {
            String oldName = meta.getPersistentDataContainer().get(ORIGINAL_NAME_KEY, PersistentDataType.STRING);
            meta.setDisplayName(oldName != null && !oldName.isEmpty() ? oldName : null);

            String rawLore = meta.getPersistentDataContainer().get(ORIGINAL_LORE_KEY, PersistentDataType.STRING);
            if (rawLore == null || rawLore.isEmpty()) {
                meta.setLore(null);
            } else {
                List<String> recoveredLore = Arrays.stream(rawLore.split(Pattern.quote(SEPARATOR)))
                        .collect(Collectors.toList());
                meta.setLore(recoveredLore);
            }

            for (ItemFlag flag : ItemFlag.values()) {
                meta.removeItemFlags(flag);
            }

            meta.getPersistentDataContainer().remove(ORIGINAL_NAME_KEY);
            meta.getPersistentDataContainer().remove(ORIGINAL_LORE_KEY);

            item.setItemMeta(meta);
        }

        player.sendMessage("§8[§bMyItem§8] §a§l✔ §7Original state restored successfully.");
    }

    private static String buildLine(String fill, int left, int right, String icon, String content) {
        String leftPad = (fill != null) ? fill.repeat(left) : "";
        String rightPad = (fill != null) ? fill.repeat(right) : "";
        String coloredContent = ChatColor.translateAlternateColorCodes('&', content);

        if (content.trim().isEmpty()) {
            return leftPad + "§f" + icon + rightPad;
        }

        return leftPad + "§f" + icon + rightPad + "§f" + coloredContent;
    }
}