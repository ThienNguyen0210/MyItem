package org.ThienNguyen.GemSocket;

import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

public class GemLogic {

    
    public static double getTotalStat(ItemStack item, String statName) {
        double total = 0;
        FileConfiguration config = Main.getInstance().getGemConfig();
        for (String gemId : getGemsOnItem(item)) {
            List<String> stats = config.getStringList(gemId + ".apply.stats");
            for (String line : stats) {
                
                String[] parts = line.split(":");
                if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(statName)) {
                    total += Double.parseDouble(parts[1].trim());
                }
            }
        }
        return total;
    }

    
    public static List<String> getAbilities(ItemStack item) {
        List<String> abilities = new ArrayList<>();
        FileConfiguration config = Main.getInstance().getGemConfig();
        for (String gemId : getGemsOnItem(item)) {
            
            List<String> gemAbils = config.getStringList(gemId + ".apply.ability");
            for (String line : gemAbils) {
                String[] parts = line.split(":"); 
                if (parts.length == 2) {
                    String name = parts[0].trim().toUpperCase();
                    String[] val = parts[1].trim().split(" "); 
                    if (val.length == 2) {
                        abilities.add(name + ":" + val[0] + ":" + val[1]);
                    }
                }
            }
        }
        return abilities;
    }

    
    public static List<String> getBuffs(ItemStack item) {
        List<String> buffs = new ArrayList<>();
        FileConfiguration config = Main.getInstance().getGemConfig();
        for (String gemId : getGemsOnItem(item)) {
            
            buffs.addAll(config.getStringList(gemId + ".apply.BUFF"));
        }
        return buffs;
    }

    
    public static List<String> getGemsOnItem(ItemStack item) {
        List<String> gems = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return gems;
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "item_sockets");
        String data = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            for (String s : data.split("\\|")) {
                if (!s.startsWith("EMPTY_")) gems.add(s);
            }
        }
        return gems;
    }
}