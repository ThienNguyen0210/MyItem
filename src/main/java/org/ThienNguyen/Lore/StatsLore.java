package org.ThienNguyen.Lore;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.ThienNguyen.Main;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class StatsLore {
    private static int getCharWidth(char c) {
        if ("i!|;:,.".indexOf(c) != -1) return 2;
        if ("l'".indexOf(c) != -1) return 3;
        if ("tI[] ".indexOf(c) != -1) return 4;
        if ("fk<>()*".indexOf(c) != -1) return 5;
        return 6;
    }
    private static int getStringPixelWidth(String text) {
        int width = 0;
        String stripped = ChatColor.stripColor(text);
        for (char c : stripped.toCharArray()) {
            width += getCharWidth(c);
        }
        return width;
    }
    public static void updateLore(ItemStack item, boolean allowRebuild) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");
        if (allowRebuild && pdc.has(formatKey, PersistentDataType.STRING)) {
            org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
            return;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        FileConfiguration statsConfig = Main.getInstance().getStatsConfig();
        if (statsConfig == null) return;

        for (String key : statsConfig.getKeys(false)) {
            NamespacedKey nKey = new NamespacedKey(Main.getInstance(), key);
            Object rawValue = getRawValue(pdc, nKey);
            String format = statsConfig.getString(key);
            if (format == null) continue;

            // Dòng "khung" của stat này ở cấu trúc hiện tại: dùng giá trị thật nếu có,
            // hoặc 0 làm giá trị mẫu chỉ để dò phần chữ/màu tĩnh (không quan tâm con số).
            Object sampleValue = (rawValue != null) ? rawValue : 0.0;
            String skeletonLine = getFormattedLoreFromConfig(statsConfig, key, sampleValue, pdc);

            int foundIndex = -1;
            for (int i = 0; i < lore.size(); i++) {
                if (sameLineIgnoringNumbers(lore.get(i), skeletonLine)) { foundIndex = i; break; }
            }

            if (rawValue != null) {
                String newLine = getFormattedLoreFromConfig(statsConfig, key, rawValue, pdc);
                if (foundIndex != -1) {
                    lore.set(foundIndex, newLine);
                } else if (isValidToAdd(key, rawValue)) {
                    lore.add(newLine);
                }
            } else if (foundIndex != -1) {
                lore.remove(foundIndex);
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * So sánh hai dòng lore theo từng ký tự: mọi ký tự không phải số (chữ, icon, mã màu
     * §..., kể cả mã hex) phải khớp CHÍNH XÁC theo đúng thứ tự; các đoạn số liên tiếp
     * (kể cả dấu chấm thập phân) được coi là khớp bất kể nội dung/độ dài khác nhau.
     * Nhờ vậy dòng flat (màu &#C1E1C1) và dòng percent (màu &#7FFFD4) của cùng một
     * label không bao giờ bị nhận nhầm là cùng một dòng, vì phần mã màu (không phải số)
     * đã khác nhau ngay từ đầu.
     */
    private static boolean sameLineIgnoringNumbers(String a, String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                while (i < a.length() && (Character.isDigit(a.charAt(i)) || a.charAt(i) == '.')) i++;
                while (j < b.length() && (Character.isDigit(b.charAt(j)) || b.charAt(j) == '.')) j++;
                continue;
            }
            if (ca != cb) return false;
            i++; j++;
        }
        return i == a.length() && j == b.length();
    }
    /**
     * Hàm Overload để giữ tương thích với các lệnh gọi cũ.
     * Mặc định sẽ cho phép rebuild nếu có template.
     */
    public static void updateLore(ItemStack item) {
        updateLore(item, true);
    }
    /**
     * Hàm phụ trợ tìm vị trí bắt đầu của keyword trong chuỗi có chứa mã màu
     */
    private static int findIndexInColorRaw(String raw, String keyword) {
        String stripped = ChatColor.stripColor(raw);
        int indexInStripped = stripped.indexOf(keyword);
        if (indexInStripped == -1) return 0;
        int currentStrippedPos = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (currentStrippedPos == indexInStripped) return i;
            char c = raw.charAt(i);
            if (c == '§' || c == '&') {
                i++;
            } else {
                currentStrippedPos++;
            }
        }
        return 0;
    }
    public static List<String> getStatsList(ItemStack item, List<String> excludedKeys) {
        List<String> statsLore = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return statsLore;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        FileConfiguration statsConfig = Main.getInstance().getStatsConfig();
        if (statsConfig == null) return statsLore;
        for (String key : statsConfig.getKeys(false)) {
            if (excludedKeys != null && excludedKeys.contains(key.toLowerCase())) {
                continue;
            }
            Object value = getRawValue(pdc, new NamespacedKey(Main.getInstance(), key));
            if (value != null && isValidToAdd(key, value)) {
                statsLore.add(getFormattedLoreFromConfig(statsConfig, key, value, pdc));
            }
        }
        return statsLore;
    }
    /**
     * Hàm Overload để giữ tương thích nếu cần gọi không có list loại trừ
     */
    public static List<String> getStatsList(ItemStack item) {
        return getStatsList(item, new ArrayList<>());
    }
    /**
     * Lấy nội dung đã format của một chỉ số duy nhất.
     */
    public static String getSingleStat(ItemStack item, String statKey) {
        if (item == null || !item.hasItemMeta()) return "";
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        FileConfiguration statsConfig = Main.getInstance().getStatsConfig();
        if (statsConfig == null) return "";
        NamespacedKey nKey = new NamespacedKey(Main.getInstance(), statKey.toLowerCase());
        Object value = getRawValue(pdc, nKey);
        if (value != null && isValidToAdd(statKey, value)) {
            return getFormattedLoreFromConfig(statsConfig, statKey, value, pdc);
        }
        return "";
    }
    private static String getFormattedLoreFromConfig(FileConfiguration config, String statType, Object value, PersistentDataContainer pdc) {
        String format = config.getString(statType.toLowerCase());
        if (format == null) format = "&7" + statType + " {value}";
        String displayValue = getDisplayValue(statType, value, pdc);
        Pattern pattern = Pattern.compile("\\{value:(\\d+)\\}");
        Matcher matcher = pattern.matcher(format);
        if (matcher.find()) {
            int targetCharPos = Integer.parseInt(matcher.group(1));
            int targetPixelWidth = targetCharPos * 6;
            String prefix = format.substring(0, matcher.start());
            int currentPixelWidth = getStringPixelWidth(formatColor(prefix));
            int pixelNeeded = targetPixelWidth - currentPixelWidth;
            StringBuilder sb = new StringBuilder();
            while (pixelNeeded > 0) {
                sb.append(" ");
                pixelNeeded -= 4;
            }
            if (format.contains("+" + matcher.group(0))) {
                format = format.replace("+" + matcher.group(0), sb.toString() + "+" + displayValue);
            } else {
                format = format.replace(matcher.group(0), sb.toString() + displayValue);
            }
        } else {
            format = format.replace("{value}", displayValue);
        }
        return formatColor(format);
    }
    private static String getDisplayValue(String statType, Object value, PersistentDataContainer pdc) {
        if (value == null) {
            return "0";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number) {
            double val = ((Number) value).doubleValue();
            if (statType.equalsIgnoreCase("UNBREAKING")) {
                double cur = val;
                double maxDur = (pdc != null) ?
                        pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "max_UNBREAKING"),
                                PersistentDataType.DOUBLE, cur) : cur;
                return String.format("%d/%d", (long) cur, (long) maxDur);
            }
            if (statType.equalsIgnoreCase("knockback_resistance")) {
                val *= 100;
            }
            return (val == (long) val) ? String.valueOf((long) val) : String.format("%.2f", val);
        }
        return value.toString();
    }
    public static String getFormattedLore(ItemStack item, String statType, Object value) {
        FileConfiguration config = Main.getInstance().getStatsConfig();
        if (config == null) return "§7" + statType + ": §f" + value;
        PersistentDataContainer pdc = (item != null && item.hasItemMeta()) ? item.getItemMeta().getPersistentDataContainer() : null;
        return getFormattedLoreFromConfig(config, statType, value, pdc);
    }
    private static Object getRawValue(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc.has(key, PersistentDataType.DOUBLE)) return pdc.get(key, PersistentDataType.DOUBLE);
        if (pdc.has(key, PersistentDataType.INTEGER)) return pdc.get(key, PersistentDataType.INTEGER);
        if (pdc.has(key, PersistentDataType.STRING)) return pdc.get(key, PersistentDataType.STRING);
        if (pdc.has(key, PersistentDataType.FLOAT)) return pdc.get(key, PersistentDataType.FLOAT);
        return null;
    }
    private static boolean isValidToAdd(String key, Object value) {
        if (value instanceof String) return !((String) value).isEmpty();
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return d != 0 || key.equalsIgnoreCase("level_require");
        }
        return false;
    }
    private static String formatColor(String text) {
        if (text == null || text.isEmpty()) return text;
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}