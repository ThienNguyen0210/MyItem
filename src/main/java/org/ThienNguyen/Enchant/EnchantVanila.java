package org.ThienNguyen.Enchant;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EnchantVanila {

    public static void updateEnchantLore(ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        FileConfiguration config = Main.getInstance().getEnchantConfig();
        if (config == null) return;

        boolean useCustomLore = config.getBoolean("settings.custom-enchant-lore", false);

        if (useCustomLore) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

            
            Map<Enchantment, Integer> currentEnchants = item.getEnchantments();
            
            Map<Enchantment, Integer> pending = new HashMap<>(currentEnchants);

            
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);

                for (Enchantment ent : currentEnchants.keySet()) {
                    
                    
                    String fullKey = ent.getKey().toString().toLowerCase();
                    String enchantKey = fullKey.contains(":") ? fullKey.split(":")[1] : fullKey;

                    if (!config.contains("display." + enchantKey)) continue;

                    String rawFormat = config.getString("display." + enchantKey);
                    if (rawFormat == null || !rawFormat.contains("{value}")) continue;

                    String prefix = ChatColor.translateAlternateColorCodes('&', rawFormat.split("\\{value\\}")[0]);

                    if (line.startsWith(prefix)) {
                        int level = currentEnchants.get(ent);
                        String levelDisplay = (level >= 1 && level <= 10) ? toRoman(level) : String.valueOf(level);
                        String newLine = ChatColor.translateAlternateColorCodes('&', rawFormat.replace("{value}", levelDisplay));

                        lore.set(i, newLine); 
                        pending.remove(ent); 
                        break;
                    }
                }
            }

            
            for (Map.Entry<Enchantment, Integer> entry : pending.entrySet()) {
                String fullKey = entry.getKey().getKey().toString().toLowerCase();
                String enchantKey = fullKey.contains(":") ? fullKey.split(":")[1] : fullKey;

                if (!config.contains("display." + enchantKey)) continue;

                String rawFormat = config.getString("display." + enchantKey);
                int level = entry.getValue();
                String levelDisplay = (level >= 1 && level <= 10) ? toRoman(level) : String.valueOf(level);

                String finalLine = ChatColor.translateAlternateColorCodes('&', rawFormat.replace("{value}", levelDisplay));
                lore.add(finalLine);
            }

            meta.setLore(lore);
        } else {
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
    }

    private static String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(10, "X"); map.put(9, "IX"); map.put(5, "V"); map.put(4, "IV"); map.put(1, "I");
        Integer l = map.floorKey(number);
        if (l == null) return String.valueOf(number);
        if (number == l) return map.get(number);
        return map.get(l) + toRoman(number - l);
    }
}