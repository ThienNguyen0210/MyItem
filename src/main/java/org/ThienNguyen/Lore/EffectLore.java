package org.ThienNguyen.Lore;

import org.ThienNguyen.Effect.BuffData;
import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EffectLore {

    
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

    public static void updateLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");

        if (meta.getPersistentDataContainer().has(formatKey, PersistentDataType.STRING)) {
            org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
            return;
        }

        FileConfiguration config = Main.getInstance().getEffectConfig();
        if (config == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        Map<String, Integer> currentEffects = BuffData.getEffects(item);
        Set<String> processedKeys = new HashSet<>();

        
        for (int i = 0; i < lore.size(); i++) {
            String effectKey = getMatchedEffectKey(lore.get(i), config);
            if (effectKey != null) {
                if (currentEffects.containsKey(effectKey) && currentEffects.get(effectKey) > 0) {
                    lore.set(i, getFormattedLine(effectKey, currentEffects.get(effectKey), config));
                    processedKeys.add(effectKey);
                } else {
                    lore.remove(i);
                    i--;
                }
            }
        }

        
        for (Map.Entry<String, Integer> entry : currentEffects.entrySet()) {
            if (!processedKeys.contains(entry.getKey()) && entry.getValue() > 0) {
                lore.add(getFormattedLine(entry.getKey(), entry.getValue(), config));
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static List<String> getEffectList(ItemStack item) {
        List<String> effectLines = new ArrayList<>();
        Map<String, Integer> effects = BuffData.getEffects(item);
        FileConfiguration config = Main.getInstance().getEffectConfig();
        if (config == null || effects.isEmpty()) return effectLines;

        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            if (entry.getValue() > 0) {
                effectLines.add(getFormattedLine(entry.getKey(), entry.getValue(), config));
            }
        }
        return effectLines;
    }

    /**
     * Logic căn lề Pixel tương tự StatsLore
     */
    private static String getFormattedLine(String key, int level, FileConfiguration config) {
        String format = config.getString("display-names." + key, key);
        boolean useRoman = config.getBoolean("use-roman", true);
        String levelDisplay = useRoman ? toRoman(level) : String.valueOf(level);

        
        Pattern pattern = Pattern.compile("\\{(level|value):(\\d+)\\}");
        Matcher matcher = pattern.matcher(format);

        if (matcher.find()) {
            int targetCharPos = Integer.parseInt(matcher.group(2));
            int targetPixelWidth = targetCharPos * 6;

            
            String prefix = format.substring(0, matcher.start());
            int currentPixelWidth = getStringPixelWidth(formatColor(prefix));

            int pixelNeeded = targetPixelWidth - currentPixelWidth;

            StringBuilder sb = new StringBuilder();
            while (pixelNeeded > 0) {
                sb.append(" ");
                pixelNeeded -= 4;
            }

            
            return formatColor(format.replace(matcher.group(0), sb.toString() + levelDisplay));
        }

        
        return formatColor(format) + " " + levelDisplay;
    }

    

    private static String getMatchedEffectKey(String line, FileConfiguration config) {
        if (config.getConfigurationSection("display-names") == null) return null;
        String strippedLine = ChatColor.stripColor(line);
        for (String key : config.getConfigurationSection("display-names").getKeys(false)) {
            String rawName = config.getString("display-names." + key);
            if (rawName == null) continue;
            
            String cleanName = ChatColor.stripColor(formatColor(rawName.split("\\{")[0])).trim();
            if (!cleanName.isEmpty() && strippedLine.contains(cleanName)) return key;
        }
        return null;
    }

    public static String formatColor(String text) {
        if (text == null || text.isEmpty()) return "";
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

    private static String toRoman(int n) {
        if (n <= 0) return ""; 
        Integer l = romanMap.floorKey(n);
        if (l == null) return "";
        if (n == l) return romanMap.get(n);
        return romanMap.get(l) + toRoman(n - l);
    }
}