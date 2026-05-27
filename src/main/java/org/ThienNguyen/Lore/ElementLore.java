package org.ThienNguyen.Lore;

import org.ThienNguyen.Element.ElementCore;
import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElementLore {

    /**
     * Cập nhật Lore cho các item không dùng hệ thống LoreFormat (Legacy)
     */
    public static void updateLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");

        if (meta.getPersistentDataContainer().has(formatKey, PersistentDataType.STRING)) {
            org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
            return;
        }

        FileConfiguration loreConfig = Main.getInstance().getElementLoreConfig();
        if (loreConfig == null) return;

        List<String> currentLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        boolean horizontal = loreConfig.getBoolean("settings.horizontal-display", false);

        if (horizontal) {
            
            Map<String, Integer> attacks = ElementCore.getAllElements(item);
            Map<String, Integer> defenses = ElementCore.getAllDefenses(item);

            
            List<String> newElementLines = getElementList(item);

            
            removeOldElementLore(currentLore, loreConfig);

            
            
            currentLore.addAll(newElementLines);
        } else {
            
            List<String> elementLines = getElementList(item);
            removeOldElementLore(currentLore, loreConfig);
            currentLore.addAll(elementLines);
        }

        meta.setLore(currentLore);
        item.setItemMeta(meta);
    }

    /**
     * Trả về danh sách Lore cho hệ thống LoreFormat {element}
     * Hỗ trợ gộp dòng ngang (Horizontal) bằng StringJoiner
     */
    public static List<String> getElementList(ItemStack item) {
        List<String> elementLore = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return elementLore;

        FileConfiguration loreConfig = Main.getInstance().getElementLoreConfig();
        if (loreConfig == null) return elementLore;

        boolean useRoman = loreConfig.getBoolean("settings.use-roman", false);
        boolean horizontal = loreConfig.getBoolean("settings.horizontal-display", false);
        String separator = formatColor(loreConfig.getString("settings.separator", "  "));

        Map<String, Integer> attacks = ElementCore.getAllElements(item);
        Map<String, Integer> defenses = ElementCore.getAllDefenses(item);

        
        if (horizontal) {
            StringJoiner attackJoiner = new StringJoiner(separator);
            attacks.forEach((id, lv) -> {
                if (lv > 0) attackJoiner.add(getFormattedElementFromSection(loreConfig, "attack", id, lv, useRoman));
            });
            if (attackJoiner.length() > 0) elementLore.add(attackJoiner.toString());
        } else {
            attacks.forEach((id, lv) -> {
                if (lv > 0) elementLore.add(getFormattedElementFromSection(loreConfig, "attack", id, lv, useRoman));
            });
        }

        
        if (horizontal) {
            StringJoiner defenseJoiner = new StringJoiner(separator);
            defenses.forEach((id, lv) -> {
                if (lv > 0) defenseJoiner.add(getFormattedElementFromSection(loreConfig, "defense", id, lv, useRoman));
            });
            if (defenseJoiner.length() > 0) elementLore.add(defenseJoiner.toString());
        } else {
            defenses.forEach((id, lv) -> {
                if (lv > 0) elementLore.add(getFormattedElementFromSection(loreConfig, "defense", id, lv, useRoman));
            });
        }

        return elementLore;
    }

    /**
     * Hàm dùng cho YamlManager và Webapi (Fix lỗi Compilation Error)
     */
    public static String getFormattedElement(String elementId, int level) {
        FileConfiguration loreConfig = Main.getInstance().getElementLoreConfig();
        if (loreConfig == null) return "§7" + elementId + ": " + level;

        boolean useRoman = loreConfig.getBoolean("settings.use-roman", false);

        
        String result = getFormattedElementFromSection(loreConfig, "attack", elementId, level, useRoman);

        
        if (result.contains("Atk " + elementId)) {
            String defResult = getFormattedElementFromSection(loreConfig, "defense", elementId, level, useRoman);
            if (!defResult.contains("Def " + elementId)) return defResult;
        }

        return result;
    }

    /**
     * Hàm hỗ trợ lấy chuỗi định dạng từ Section cụ thể
     */
    private static String getFormattedElementFromSection(FileConfiguration config, String section, String id, int lv, boolean useRoman) {
        String fmt = config.getString(section + "." + id);
        if (fmt == null) fmt = (section.equals("attack") ? "&7Atk " : "&7Def ") + id + ": {value}";
        String val = useRoman ? toRoman(lv) : String.valueOf(lv);
        return formatColor(fmt.replace("{value}", val));
    }

    /**
     * Xóa các dòng Lore cũ để tránh bị lặp khi cập nhật item
     */
    private static void removeOldElementLore(List<String> lore, FileConfiguration config) {
        
        
        Set<String> prefixes = new HashSet<>();
        for (String section : Arrays.asList("attack", "defense")) {
            ConfigurationSection sec = config.getConfigurationSection(section);
            if (sec == null) continue;
            for (String key : sec.getKeys(false)) {
                String fmt = config.getString(section + "." + key);
                if (fmt != null) {
                    String prefix = ChatColor.stripColor(formatColor(fmt.split("\\{value\\}")[0])).trim();
                    if (!prefix.isEmpty()) prefixes.add(prefix);
                }
            }
        }

        
        lore.removeIf(line -> {
            String stripped = ChatColor.stripColor(line).trim();
            
            for (String pre : prefixes) {
                if (stripped.startsWith(pre)) return true;
            }
            
            return stripped.contains("🔥") || stripped.contains("❄") || stripped.contains("⚡") || stripped.contains("🛡");
        });
    }

    public static String formatColor(String text) {
        if (text == null || text.isEmpty()) return text;
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private static final TreeMap<Integer, String> romanMap = new TreeMap<>();
    static {
        romanMap.put(1000, "M"); romanMap.put(900, "CM"); romanMap.put(500, "D");
        romanMap.put(400, "CD"); romanMap.put(100, "C"); romanMap.put(90, "XC");
        romanMap.put(50, "L"); romanMap.put(40, "XL"); romanMap.put(10, "X");
        romanMap.put(9, "IX"); romanMap.put(5, "V"); romanMap.put(4, "IV");
        romanMap.put(1, "I");
    }

    private static String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        Integer l = romanMap.floorKey(number);
        if (l == null) return String.valueOf(number);
        if (number == l) return romanMap.get(number);
        return romanMap.get(l) + toRoman(number - l);
    }
}