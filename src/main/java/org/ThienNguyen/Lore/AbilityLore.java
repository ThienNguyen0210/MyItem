package org.ThienNguyen.Lore;

import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbilityLore {

    private static final NamespacedKey START_KEY = new NamespacedKey(Main.getInstance(), "ability_lore_start");
    private static final NamespacedKey END_KEY   = new NamespacedKey(Main.getInstance(), "ability_lore_end");

    public static void updateLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");

        
        if (meta.getPersistentDataContainer().has(formatKey, PersistentDataType.STRING)) {
            org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
            return; 
        }

        
        List<String> newAbilityLore = getAbilityList(item);
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        
        Integer oldStart = getSafeInteger(meta, START_KEY);
        Integer oldEnd   = getSafeInteger(meta, END_KEY);

        
        if (oldStart != null && oldEnd != null && oldStart >= 0 && oldEnd < lore.size()) {
            for (int i = oldEnd; i >= oldStart; i--) {
                lore.remove(i);
            }
        }

        
        if (newAbilityLore.isEmpty()) {
            meta.getPersistentDataContainer().remove(START_KEY);
            meta.getPersistentDataContainer().remove(END_KEY);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return;
        }

        
        int insertPos;
        if (oldStart != null && oldStart <= lore.size()) {
            insertPos = oldStart;
        } else {
            insertPos = lore.size();
            
            if (!lore.isEmpty() && !ChatColor.stripColor(lore.get(lore.size() - 1)).trim().isEmpty()) {
                lore.add("§7");
                insertPos++;
            }
        }

        
        lore.addAll(insertPos, newAbilityLore);
        meta.getPersistentDataContainer().set(START_KEY, PersistentDataType.INTEGER, insertPos);
        meta.getPersistentDataContainer().set(END_KEY, PersistentDataType.INTEGER, insertPos + newAbilityLore.size() - 1);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * HÀM MỚI: Trả về List các dòng nội tại cho LoreFormat {ability}
     */
    public static List<String> getAbilityList(ItemStack item) {
        List<String> result = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return result;

        String rawAll = AbilityData.getRawAbility(item);
        if (rawAll == null || rawAll.trim().isEmpty()) return result;

        FileConfiguration config = Main.getInstance().getAbilityConfig();
        if (config == null) return result;

        boolean useRoman = config.getBoolean("settings.use-roman", false);
        String[] entries = rawAll.split(",");

        for (String entry : entries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 3);
            if (parts.length != 3) continue;

            String key    = parts[0].trim();
            String level  = parts[1].trim();
            String chance = parts[2].trim();

            String levelToDisplay = level;
            if (useRoman) {
                try {
                    levelToDisplay = toRoman(Integer.parseInt(level));
                } catch (NumberFormatException ignored) {}
            }

            List<String> template = config.getStringList("abilities." + key + ".lore");
            if (template.isEmpty()) continue;

            
            /*
            if (!result.isEmpty()) {
                result.add("");
            }
            */

            for (String line : template) {
                result.add(formatColor(line
                        .replace("{level}", levelToDisplay)
                        .replace("{chance}", chance)));
            }
        }
        return result;
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

    private static Integer getSafeInteger(ItemMeta meta, NamespacedKey key) {
        if (!meta.getPersistentDataContainer().has(key)) return null;
        try {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        } catch (IllegalArgumentException e) {
            Double d = meta.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
            return d != null ? d.intValue() : null;
        }
    }

    private static final TreeMap<Integer, String> romanMap = new TreeMap<>();
    static {
        romanMap.put(1000, "M"); romanMap.put(900, "CM"); romanMap.put(500, "D"); romanMap.put(400, "CD");
        romanMap.put(100, "C"); romanMap.put(90, "XC"); romanMap.put(50, "L"); romanMap.put(40, "XL");
        romanMap.put(10, "X"); romanMap.put(9, "IX"); romanMap.put(5, "V"); romanMap.put(4, "IV");
        romanMap.put(1, "I");
    }

    private static String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        Integer l = romanMap.floorKey(number);
        if (l == null) return String.valueOf(number);
        if (number == l) return romanMap.get(number);
        return romanMap.get(l) + toRoman(number - l);
    }
    public static String getFormattedLine(String abilityId, int level) {
        FileConfiguration config = Main.getInstance().getAbilityConfig(); 
        if (config == null) return "§7Ability: §f" + abilityId + " " + toRoman(level);

        String path = "abilities." + abilityId + ".display-name";
        String displayName = config.getString(path);

        if (displayName == null) return "§7Ability: §f" + abilityId + " " + toRoman(level);

        return formatColor(displayName + " " + toRoman(level));
    }

}