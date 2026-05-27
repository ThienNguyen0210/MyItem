package org.ThienNguyen.GemSocket;

import org.ThienNguyen.Main;
import org.ThienNguyen.Lore.LoreGenerator;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GemDucLo implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onDrillApply(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;

        ItemStack drillItem = event.getCursor();
        ItemStack targetItem = event.getCurrentItem();

        if (drillItem == null || targetItem == null || targetItem.getType() == Material.AIR) return;

        ItemMeta drillMeta = drillItem.getItemMeta();
        if (drillMeta == null) return;

        NamespacedKey typeKey = new NamespacedKey(Main.getInstance(), "gem_item_type");
        String itemType = drillMeta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

        if (itemType == null || !itemType.equals("DRILL")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (targetItem.getAmount() >= 2) {
            player.sendMessage(Main.getInstance().getLangManager().getMessage("item.no-stack-allowed"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return;
        }

        NamespacedKey idKey = new NamespacedKey(Main.getInstance(), "gem_item_id");
        String drillId = drillMeta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);

        FileConfiguration config = Main.getInstance().getDucLoConfig();
        if (drillId == null || !config.contains(drillId)) return;

        String socketType = config.getString(drillId + ".type", "common");

        
        if (!canAddMoreSocket(targetItem, socketType)) {
            player.sendMessage(Main.getInstance().getLangManager().getMessage(
                    "item.socket-limit", "{type}", socketType));
            return;
        }

        
        double cost = config.getDouble(drillId + ".cost", 0);
        if (cost > 0) {
            
            if (Main.getInstance().getEconomy().getBalance(player) < cost) {
                player.sendMessage("§cBạn không đủ tiền để đục lỗ! Cần: §f" + cost);
                return;
            }
            
            Main.getInstance().getEconomy().withdrawPlayer(player, cost);
            player.sendMessage("§eĐã thanh toán §f" + cost + " §ephí đục lỗ.");
        }

        
        drillItem.setAmount(drillItem.getAmount() - 1);

        
        int successRate = config.getInt(drillId + ".success", 100);
        if (random.nextInt(100) < successRate) {
            if (addSocketToItem(targetItem, socketType, player, drillId)) {
                player.sendMessage(Main.getInstance().getLangManager().getMessage("item.drill-success"));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
            }
        } else {
            
            player.sendMessage(Main.getInstance().getLangManager().getMessage("item.drill-failed"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
        }
    }

    /**
     * Kiểm tra có thể đục thêm lỗ không
     */
    private boolean canAddMoreSocket(ItemStack item, String socketType) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String currentSockets = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);

        
        if (currentSockets == null || currentSockets.isEmpty()) return true;

        
        String[] sockets = currentSockets.split("\\|");

        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();

        
        
        int maxTotalSockets = typeConfig.getInt(socketType + ".max-total", 2);
        int typeLimit = typeConfig.getInt(socketType + ".limit", 2);

        
        
        if (sockets.length >= maxTotalSockets) {
            return false;
        }

        
        int currentTypeCount = 0;
        FileConfiguration gemConfig = Main.getInstance().getGemConfig();

        for (String s : sockets) {
            
            if (s.equals("EMPTY_" + socketType)) {
                currentTypeCount++;
            }
            
            else if (gemConfig.contains(s)) {
                String gemType = gemConfig.getString(s + ".type", "");
                if (gemType.equalsIgnoreCase(socketType)) {
                    currentTypeCount++;
                }
            }
        }

        return currentTypeCount < typeLimit;
    }
    /**
     * Thêm lỗ ngọc vào item
     */
    private boolean addSocketToItem(ItemStack item, String socketType, Player player, String drillId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        FileConfiguration ducLoConfig = Main.getInstance().getDucLoConfig();
        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();

        
        if (ducLoConfig.contains(drillId + ".model-id")) {
            int newModelId = ducLoConfig.getInt(drillId + ".model-id");
            meta.setCustomModelData(newModelId);
        }

        
        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String currentSockets = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);
        if (currentSockets == null) currentSockets = "";

        String newSocket = "EMPTY_" + socketType;
        String updatedData = currentSockets.isEmpty() ? newSocket : currentSockets + "|" + newSocket;

        meta.getPersistentDataContainer().set(socketKey, PersistentDataType.STRING, updatedData);

        
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");
        String formatId = meta.getPersistentDataContainer().get(formatKey, PersistentDataType.STRING);

        if (formatId != null && !formatId.isEmpty()) {
            
            item.setItemMeta(meta);           
            LoreGenerator.rebuild(item);      
        }
        else {
            
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            String format = typeConfig.getString(socketType + ".format", "&7[ ○ ] Lỗ trống");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', format));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return true;
    }
    
    private boolean withdrawMoney(Player player, double amount) {
        if (amount <= 0) return true;

        
        if (Main.getInstance().getEconomy().getBalance(player) >= amount) {
            Main.getInstance().getEconomy().withdrawPlayer(player, amount);
            return true;
        }
        return false;
    }
}