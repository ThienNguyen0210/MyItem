package org.ThienNguyen.Evolution;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class EvolutionManager {

    public static final NamespacedKey TARGET_KEY = new NamespacedKey(Main.getInstance(), "evo_target");
    public static final NamespacedKey CURRENT_KEY = new NamespacedKey(Main.getInstance(), "evo_current");
    public static final NamespacedKey REQUIRED_KEY = new NamespacedKey(Main.getInstance(), "evo_required");
    public static final NamespacedKey NEXT_ID_KEY = new NamespacedKey(Main.getInstance(), "evo_next_id");

    public static void addProgress(Player player, ItemStack item, String mobId) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(TARGET_KEY, PersistentDataType.STRING)) return;

        String target = pdc.get(TARGET_KEY, PersistentDataType.STRING);
        if (!target.equalsIgnoreCase("ALL") && !target.equalsIgnoreCase(mobId) && !mobId.equals("INITIALIZE_ONLY")) return;

        int current = pdc.getOrDefault(CURRENT_KEY, PersistentDataType.INTEGER, 0);
        int required = pdc.getOrDefault(REQUIRED_KEY, PersistentDataType.INTEGER, 0);
        String nextId = pdc.get(NEXT_ID_KEY, PersistentDataType.STRING);

        
        String oldName = meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();

        if (!mobId.equals("INITIALIZE_ONLY")) {
            current++;
        }

        if (current >= required && nextId != null) {
            ItemStack evolved = Main.getInstance().getItemDatabase().loadItem(nextId);
            if (evolved != null) {
                item.setType(evolved.getType());
                item.setItemMeta(evolved.getItemMeta());

                
                handleSuccess(player, item, oldName);
                return;
            }
        }

        pdc.set(CURRENT_KEY, PersistentDataType.INTEGER, current);
        item.setItemMeta(meta);
        updateEvolutionLore(item, current, required);
    }

    private static void handleSuccess(Player player, ItemStack evolvedItem, String oldName) {
        FileConfiguration config = Main.getInstance().getEvolutionConfig();
        if (config == null) return;

        String newName = evolvedItem.getItemMeta().hasDisplayName() ?
                evolvedItem.getItemMeta().getDisplayName() : evolvedItem.getType().name();

        
        List<String> privateMsgs = config.getStringList("settings.private-messages");
        if (privateMsgs != null && !privateMsgs.isEmpty()) {
            for (String msg : privateMsgs) {
                player.sendMessage(msg.replace("{item_old}", oldName)
                        .replace("{item_new}", newName)
                        .replace("{item}", newName)
                        .replace("&", "§"));
            }
        } else {
            
            player.sendMessage("§a§l✔ §fVật phẩm đã tiến hóa thành §e" + newName);
        }

        
        if (config.getBoolean("settings.enable-broadcast", false)) {
            String broadcastMsg = config.getString("settings.broadcast-message", "&6&l[Tiến Hóa] &e{player} &fđã nâng cấp thành công &b{item}&f!");
            Bukkit.broadcastMessage(broadcastMsg.replace("{player}", player.getName())
                    .replace("{item}", newName)
                    .replace("&", "§"));
        }

        
        try {
            String s = config.getString("settings.effects.sound", "ENTITY_PLAYER_LEVELUP");
            player.playSound(player.getLocation(), Sound.valueOf(s), 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        
        try {
            String p = config.getString("settings.effects.particle", "EXPLOSION_HUGE");
            player.getWorld().spawnParticle(Particle.valueOf(p), player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        } catch (Exception e) {
            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 20);
        }
    }

    private static void updateEvolutionLore(ItemStack item, int current, int required) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        int percent = (required > 0) ? (int) ((double) current / required * 100) : 0;
        if (percent > 100) percent = 100;

        
        String format = Main.getInstance().getEvolutionConfig().getString("settings.lore-format", "&6⚡ &fTiến hóa: &b{current}&7/&e{required} &8({percent}%)");

        String targetLine = format.replace("{current}", String.valueOf(current))
                .replace("{required}", String.valueOf(required))
                .replace("{percent}", String.valueOf(percent))
                .replace("&", "§");

        boolean found = false;
        
        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = lore.get(i);
            if (line.contains("Tiến hóa:") || line.contains("Tiến độ:")) {
                lore.set(i, targetLine);
                found = true;
                break;
            }
        }

        
        if (!found) {
            
            if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
                lore.add("");
            }
            lore.add(targetLine);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}