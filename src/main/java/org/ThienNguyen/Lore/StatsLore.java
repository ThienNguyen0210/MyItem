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

    // --- BẢNG ĐỘ RỘNG PIXEL CHUẨN CỦA MINECRAFT ---
    private static int getCharWidth(char c) {
        if ("i!|;:,.".indexOf(c) != -1) return 2;
        if ("l'".indexOf(c) != -1) return 3;
        if ("tI[] ".indexOf(c) != -1) return 4; // Khoảng trắng = 4px
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

    public static void updateLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");

        if (pdc.has(formatKey, PersistentDataType.STRING)) {
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

            // Keyword để nhận diện (Ví dụ: "Sát thương:")
            String keyword = ChatColor.stripColor(formatColor(format.split("\\{")[0]))
                    .replace("+", "")
                    .trim();

            if (keyword.isEmpty()) continue;

            boolean replaced = false;
            String newLine = getFormattedLoreFromConfig(statsConfig, key, rawValue, pdc);

            if (rawValue != null) {
                for (int i = 0; i < lore.size(); i++) {
                    String currentLineRaw = lore.get(i);
                    String currentLineStripped = ChatColor.stripColor(currentLineRaw);

                    if (currentLineStripped.contains(keyword)) {
                        // --- FIX LOGIC TẠI ĐÂY ---
                        // Thay vì split phức tạp, ta kiểm tra xem dòng cũ có "tiền tố lạ" (adsadsa) không.
                        // Nếu dòng cũ dài hơn format chuẩn của keyword, ta giữ phần đầu.

                        int keywordIndexInStripped = currentLineStripped.indexOf(keyword);

                        if (keywordIndexInStripped > 0) {
                            // Trích xuất phần tiền tố dựa trên index của keyword trong chuỗi đã strip
                            // Sau đó lấy độ dài đó áp dụng vào chuỗi gốc (có màu)
                            // Đây là cách lấy "adsadsadsa" an toàn nhất
                            String colorCodePrefix = "";
                            // Tìm vị trí tương đối của keyword trong chuỗi có màu
                            // (Dùng tạm Regex thay thế để giữ cấu trúc)
                            lore.set(i, currentLineRaw.substring(0, findIndexInColorRaw(currentLineRaw, keyword)) + newLine);
                        } else {
                            // Nếu keyword nằm ngay đầu dòng, thay thẳng bằng newLine
                            lore.set(i, newLine);
                        }

                        replaced = true;
                        break;
                    }
                }

                if (!replaced && isValidToAdd(key, rawValue)) {
                    lore.add(newLine);
                }
            } else {
                lore.removeIf(line -> ChatColor.stripColor(line).contains(keyword));
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
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
                i++; // Bỏ qua ký tự mã màu
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
            // Kiểm tra loại trừ: Nếu key này đã được render riêng thì bỏ qua
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

        // ====================== XỬ LÝ KIỂU DỮ LIỆU ======================
        String displayValue = getDisplayValue(statType, value, pdc);

        // Regex tìm {value:số}
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
                pixelNeeded -= 4; // Khoảng trắng mặc định 4px
            }

            // ====================== FIX LOGIC REPLACE Ở ĐÂY ======================
            // Kiểm tra xem phía trước {value:xx} có dấu '+' không
            if (format.contains("+" + matcher.group(0))) {
                // Nếu có dấu +, replace cả cụm "+{value:xx}" bằng "khoảng_trắng + giá_trị"
                format = format.replace("+" + matcher.group(0), sb.toString() + "+" + displayValue);
            } else {
                // Nếu không có dấu +, replace bình thường "{value:xx}" bằng "khoảng_trắng giá_trị"
                format = format.replace(matcher.group(0), sb.toString() + displayValue);
            }
        } else {
            // Trường hợp chỉ dùng {value} bình thường
            format = format.replace("{value}", displayValue);
        }

        return formatColor(format);
    }

    private static String getDisplayValue(String statType, Object value, PersistentDataContainer pdc) {
        // THÊM ĐOẠN NÀY ĐỂ FIX LỖI NULL
        if (value == null) {
            return "0"; // Hoặc trả về chuỗi trống "" tùy bạn
        }

        if (value instanceof String) {
            return (String) value;
        }

        if (value instanceof Number) {
            double val = ((Number) value).doubleValue();

            if (statType.equalsIgnoreCase("durability")) {
                double cur = val;
                double maxDur = (pdc != null) ?
                        pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "max_durability"),
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