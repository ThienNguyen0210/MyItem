package org.ThienNguyen.AI.utils;

import org.ThienNguyen.Main;
import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.Lore.AbilityLore;
import org.ThienNguyen.Lore.SkillLore;
import org.ThienNguyen.Lore.StatsLore;
import org.ThienNguyen.Lore.TiersLore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlManager {

    private static File getAiFile() {
        File folder = new File(Main.getInstance().getDataFolder(), "AI");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, "Item.yml");
    }

    public static int saveToAiFolder(String yamlContent) {
        File file = getAiFile();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        int nextId = 1;
        while (config.contains(String.valueOf(nextId))) {
            nextId++;
        }

        YamlConfiguration tempConfig = new YamlConfiguration();
        try {
            String cleanContent = yamlContent.replaceAll("(?i)```yaml", "")
                    .replaceAll("(?i)```", "")
                    .trim();

            tempConfig.loadFromString(cleanContent);

            ConfigurationSection dataToSave = null;
            if (!tempConfig.getKeys(false).isEmpty()) {
                String firstKey = tempConfig.getKeys(false).iterator().next();
                
                if (tempConfig.isConfigurationSection(firstKey)) {
                    dataToSave = tempConfig.getConfigurationSection(firstKey);
                } else {
                    dataToSave = tempConfig;
                }
            }

            if (dataToSave != null) {
                config.set(String.valueOf(nextId), dataToSave);
                config.save(file);
                return nextId;
            }
            return -1;
        } catch (Exception e) {
            Bukkit.getLogger().severe("[WindyAI] Lỗi Parse YAML: \n" + yamlContent);
            return -1;
        }
    }

    public static ItemStack getItemFromAiFolder(String id) {
        File file = getAiFile();
        if (!file.exists()) return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(id);

        if (section == null) return null;

        return buildItemFromConfig(section);
    }

    public static ItemStack buildItemFromConfig(ConfigurationSection section) {
        if (section == null) return null;

        
        String matName = section.getString("material", "NETHERITE_SWORD").toUpperCase();
        Material mat = Material.matchMaterial(matName);
        ItemStack item = new ItemStack(mat != null ? mat : Material.NETHERITE_SWORD);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        

        
        if (section.getBoolean("unbreaking", false)) {
            meta.setUnbreakable(true);
        }

        if (section.contains("lore_format")) {
            meta.getPersistentDataContainer().set(new NamespacedKey(Main.getInstance(), "lore_format_id"), PersistentDataType.STRING, section.getString("lore_format"));
        }

        
        if (section.contains("tier")) {
            meta.getPersistentDataContainer().set(new NamespacedKey(Main.getInstance(), "item_tier_id"), PersistentDataType.STRING, section.getString("tier"));
        }

        
        if (section.contains("stats")) {
            ConfigurationSection statsSec = section.getConfigurationSection("stats");
            for (String key : statsSec.getKeys(false)) {
                NamespacedKey nsk = new NamespacedKey(Main.getInstance(), key);
                if (key.equalsIgnoreCase("level_require")) {
                    meta.getPersistentDataContainer().set(nsk, PersistentDataType.INTEGER, statsSec.getInt(key));
                } else {
                    meta.getPersistentDataContainer().set(nsk, PersistentDataType.DOUBLE, statsSec.getDouble(key));
                }
            }
        }

        
        if (section.contains("skill")) {
            ConfigurationSection skillSec = section.getConfigurationSection("skill");
            for (String key : skillSec.getKeys(false)) {
                meta.getPersistentDataContainer().set(new NamespacedKey(Main.getInstance(), "skill_" + key.toUpperCase()), PersistentDataType.INTEGER, skillSec.getInt(key));
            }
        }

        
        item.setItemMeta(meta);

        
        List<String> rawLore = section.getStringList("lore");
        List<String> finalLore = new ArrayList<>();

        for (String line : rawLore) {
            String renderedLine = line.trim();

            
            if (renderedLine.startsWith("-") && !renderedLine.contains("&m-")) {
                renderedLine = renderedLine.substring(1).trim();
            }

            
            if (renderedLine.contains("{tier}")) {
                renderedLine = renderedLine.replace("{tier}", TiersLore.getTierLine(item));
            } else if (renderedLine.contains("{tier:")) {
                String key = extractKey(renderedLine, "{tier:");
                
                renderedLine = renderedLine.replace("{tier:" + key + "}", TiersLore.getTierLine(item));
            }

            
            if (renderedLine.contains("{stats:")) {
                Pattern pattern = Pattern.compile("\\{stats:([^}]+)\\}");
                Matcher matcher = pattern.matcher(renderedLine);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    double val = section.getDouble("stats." + key);
                    renderedLine = renderedLine.replace(matcher.group(0), StatsLore.getFormattedLore(item, key, val));
                }
            }

            
            if (renderedLine.contains("{element:")) {
                String eleId = extractKey(renderedLine, "{element:");
                int level = section.getInt("elements." + eleId, 0);
                if (level > 0) {
                    renderedLine = renderedLine.replace("{element:" + eleId + "}", org.ThienNguyen.Lore.ElementLore.getFormattedElement(eleId, level));
                } else { renderedLine = ""; }
            }

            
            else if (renderedLine.contains("{effect:")) {
                String effId = extractKey(renderedLine, "{effect:");
                int level = section.getInt("effects." + effId, 0);
                if (level > 0) {
                    String effectLore = org.ThienNguyen.Webapi.Web.getFormattedEffect(effId, level);
                    String cleanEffect = effectLore.trim();
                    if (cleanEffect.startsWith("-") && !cleanEffect.contains("&m-")) {
                        cleanEffect = cleanEffect.substring(1).trim();
                    }
                    renderedLine = renderedLine.replace("{effect:" + effId + "}", cleanEffect);
                } else { renderedLine = ""; }
            }

            
            else if (renderedLine.contains("{ability:")) {
                String abilityKey = extractKey(renderedLine, "{ability:");
                String rawVal = section.getString("ability." + abilityKey);
                if (rawVal != null && rawVal.contains(":")) {
                    try {
                        String[] split = rawVal.split(":");
                        String abilityLore = org.ThienNguyen.Webapi.Web.getFormattedAbility(abilityKey, Integer.parseInt(split[0]), Double.parseDouble(split[1]));
                        if (abilityLore.contains("\n")) {
                            for (String subLine : abilityLore.split("\n")) {
                                String cleanSub = subLine.trim();
                                if (cleanSub.startsWith("-")) cleanSub = cleanSub.substring(1).trim();
                                finalLore.add(formatColor(cleanSub));
                            }
                            continue;
                        }
                        renderedLine = renderedLine.replace("{ability:" + abilityKey + "}", abilityLore);
                    } catch (Exception ignored) {}
                }
            }

            
            if (renderedLine.contains("{skill:")) {
                String key = extractKey(renderedLine, "{skill:");
                int level = section.getInt("skill." + key);
                renderedLine = renderedLine.replace("{skill:" + key + "}", SkillLore.getFormattedLine(key, level));
            }

            if (!renderedLine.isEmpty()) finalLore.add(formatColor(renderedLine));
        }

        
        ItemMeta finalMeta = item.getItemMeta(); 
        finalMeta.setDisplayName(formatColor(section.getString("display_name")));
        finalMeta.setLore(finalLore);
        item.setItemMeta(finalMeta);

        
        if (section.contains("elements")) {
            ConfigurationSection eleSec = section.getConfigurationSection("elements");
            for (String key : eleSec.getKeys(false)) {
                org.ThienNguyen.Element.ElementCore.addElement(item, key.toUpperCase(), eleSec.getInt(key));
            }
        }

        if (section.contains("effects")) {
            ConfigurationSection effSec = section.getConfigurationSection("effects");
            for (String key : effSec.getKeys(false)) {
                org.ThienNguyen.Effect.BuffData.setEffect(item, key.toUpperCase(), effSec.getInt(key));
            }
        }

        if (section.contains("ability")) {
            ConfigurationSection abSec = section.getConfigurationSection("ability");
            for (String key : abSec.getKeys(false)) {
                String rawValue = abSec.get(key).toString();
                if (rawValue.contains(":")) {
                    try {
                        String[] split = rawValue.split(":");
                        org.ThienNguyen.Ability.AbilityData.setAbility(item, key.toUpperCase(), Integer.parseInt(split[0]), Double.parseDouble(split[1]));
                    } catch (Exception ignored) {}
                }
            }
        }

        return item;
    }

    
    private static String extractKey(String line, String prefix) {
        try {
            int start = line.indexOf(prefix) + prefix.length();
            int end = line.indexOf("}", start);
            return line.substring(start, end).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String formatColor(String text) {
        if (text == null || text.isEmpty()) return "";
        
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