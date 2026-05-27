package org.ThienNguyen.Lore;

import org.ThienNguyen.Main;
import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Skill.SkillManager;
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

public class SkillLore {

    private static final NamespacedKey SKILL_LORE_START = new NamespacedKey(Main.getInstance(), "skill_lore_start");
    private static final NamespacedKey SKILL_LORE_END   = new NamespacedKey(Main.getInstance(), "skill_lore_end");

    public static void updateLore(ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");
        if (meta.getPersistentDataContainer().has(formatKey, PersistentDataType.STRING)) {
            org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
            return; 
        }

        
        NamespacedKey skillKey = new NamespacedKey(Main.getInstance(), "item_skills");
        if (!meta.getPersistentDataContainer().has(skillKey, PersistentDataType.STRING)) return;

        List<String> newLoreLines = getSkillList(item); 
        if (newLoreLines.isEmpty()) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        Integer oldStart = meta.getPersistentDataContainer().get(SKILL_LORE_START, PersistentDataType.INTEGER);
        Integer oldEnd   = meta.getPersistentDataContainer().get(SKILL_LORE_END, PersistentDataType.INTEGER);

        
        if (oldStart != null && oldEnd != null && oldStart >= 0 && oldEnd < lore.size()) {
            for (int i = oldEnd; i >= oldStart; i--) {
                lore.remove(i);
            }
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

        lore.addAll(insertPos, newLoreLines);

        
        meta.getPersistentDataContainer().set(SKILL_LORE_START, PersistentDataType.INTEGER, insertPos);
        meta.getPersistentDataContainer().set(SKILL_LORE_END, PersistentDataType.INTEGER, insertPos + newLoreLines.size() - 1);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * HÀM MỚI: Trả về List các dòng kỹ năng cho LoreFormat {skill}
     */
    public static List<String> getSkillList(ItemStack item) {
        List<String> skillLoreTotal = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return skillLoreTotal;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey skillKey = new NamespacedKey(Main.getInstance(), "item_skills");
        String data = meta.getPersistentDataContainer().get(skillKey, PersistentDataType.STRING);

        if (data == null || data.trim().isEmpty()) return skillLoreTotal;

        String[] skillEntries = data.split(",");
        FileConfiguration mainSkillConfig = Main.getInstance().getSkillConfig();
        boolean useRoman = mainSkillConfig != null && mainSkillConfig.getBoolean("settings.use-roman", false);

        for (String entry : skillEntries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 5);
            if (parts.length < 4) continue;

            String skillId   = parts[0].trim();
            String trigger   = parts[1].trim();
            String cooldown  = parts[2].trim();
            String level     = parts[3].trim();
            String type      = (parts.length >= 5) ? parts[4].trim() : "Weapon";

            ISkill skill = SkillManager.getSkill(skillId);
            if (skill == null) continue;

            String displayLevel = level;
            if (useRoman) {
                try { displayLevel = toRoman(Integer.parseInt(level)); } catch (Exception ignored) {}
            }

            FileConfiguration config;
            String path;

            
            if (type.equalsIgnoreCase("MythicMob") || type.equalsIgnoreCase("MythicMobs") || type.equalsIgnoreCase("MythicLib")) {
                config = Main.getInstance().getSkillMythicLibConfig();
                path = skillId + ".lore";
            } else if (type.equalsIgnoreCase("Command")) {
                config = Main.getInstance().getSkillConfig();
                path = "Command." + skillId;
            } else {
                config = Main.getInstance().getSkillConfig();
                path = "Weapon." + skillId;
            }

            if (config == null) continue;
            List<String> format = config.getStringList(path);
            if (format.isEmpty()) continue;

            List<String> processedSkill = new ArrayList<>();
            for (String line : format) {
                processedSkill.add(formatColor(line
                        .replace("%trigger%", trigger)
                        .replace("%cooldown%", cooldown)
                        .replace("%level%", displayLevel)
                        .replace("%name%", skillId)));
            }

            
            if (!skillLoreTotal.isEmpty() && !processedSkill.isEmpty()) {
                skillLoreTotal.add("");
            }
            skillLoreTotal.addAll(processedSkill);
        }
        return skillLoreTotal;
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
    public static String getFormattedLine(String skillId, int level) {
        FileConfiguration config = Main.getInstance().getSkillConfig(); 
        if (config == null) return "§7Skill: §f" + skillId + " " + toRoman(level);

        String path = "skills." + skillId + ".display-name";
        String displayName = config.getString(path);

        if (displayName == null) return "§7Skill: §f" + skillId + " " + toRoman(level);

        
        return formatColor(displayName + " " + toRoman(level));
    }
}