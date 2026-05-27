package org.ThienNguyen.GemSocket;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
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

public class GemRemover implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onRemoverApply(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;

        ItemStack removerItem = event.getCursor();
        ItemStack targetItem = event.getCurrentItem();

        if (removerItem == null || targetItem == null || targetItem.getType() == Material.AIR) return;

        ItemMeta removerMeta = removerItem.getItemMeta();
        if (removerMeta == null) return;

        NamespacedKey typeKey = new NamespacedKey(Main.getInstance(), "gem_item_type");
        String itemType = removerMeta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

        if (itemType == null || !itemType.equals("REMOVER")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        var lang = Main.getInstance().getLangManager();

        player.sendMessage("§e[DEBUG] Bắt đầu xử lý Remover...");

        if (targetItem.getAmount() >= 2) {
            player.sendMessage(lang.getMessage("item.no-stack-allowed"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return;
        }

        List<String> gemsOnItem = GemLogic.getGemsOnItem(targetItem);
        player.sendMessage("§e[DEBUG] Số ngọc: " + gemsOnItem.size());

        if (gemsOnItem.isEmpty()) {
            player.sendMessage("§cVật phẩm này không có ngọc nào để gỡ!");
            return;
        }

        String removedGemId = gemsOnItem.get(random.nextInt(gemsOnItem.size()));
        player.sendMessage("§e[DEBUG] Đang gỡ: " + removedGemId);

        ItemStack returnedGem = createGemItem(removedGemId);
        if (returnedGem != null) {
            if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                player.getInventory().setItemInMainHand(returnedGem);
            } else {
                player.getInventory().addItem(returnedGem);
            }
        }

        if (removeGemFromItem(targetItem, removedGemId, player)) {
            player.sendMessage("§aĐã gỡ ngọc §f" + removedGemId + " §athành công! Ngọc đã được trả về.");
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            new org.ThienNguyen.Listener.CacheListener().refreshCache(player);
        } else {
            player.sendMessage("§cKhông thể gỡ ngọc này!");
        }

        removerItem.setAmount(removerItem.getAmount() - 1);
    }

    private boolean removeGemFromItem(ItemStack item, String gemIdToRemove, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        
        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String currentData = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);
        if (currentData == null || currentData.isEmpty()) return false;

        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
        String gemType = gemConfig.getString(gemIdToRemove + ".type", "common");

        String newData = currentData.replaceFirst(gemIdToRemove, "EMPTY_" + gemType);
        meta.getPersistentDataContainer().set(socketKey, PersistentDataType.STRING, newData);

        player.sendMessage("§e[DEBUG] PDC cập nhật: " + newData);

        
        restoreEmptySocketLore(item, gemIdToRemove, gemType, player);

        item.setItemMeta(meta);
        return true;
    }

    /**
     * THAY LORE THEO FORMAT TRONG TYPE.YML - Tìm đúng dòng có ︵
     */
    private void restoreEmptySocketLore(ItemStack item, String gemIdToRemove, String socketType, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            player.sendMessage("§c[DEBUG] Không có lore");
            return;
        }

        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();

        String emptyFormat = typeConfig.getString(socketType + ".format", "&7[ ○ ] Lỗ trống");
        String coloredEmpty = ChatColor.translateAlternateColorCodes('&', emptyFormat);

        player.sendMessage("§e[DEBUG] Type: " + socketType + " | Format từ type.yml: §f" + coloredEmpty);

        List<String> lore = new ArrayList<>(meta.getLore());
        boolean replaced = false;

        player.sendMessage("§e[DEBUG] Bắt đầu quét lore... Tổng " + lore.size() + " dòng");

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String stripped = ChatColor.stripColor(line);

            player.sendMessage("§7[DEBUG] Dòng " + (i+1) + ": §f" + line);

            
            if (line.contains("︵") || stripped.contains("Sát Thương I") && line.contains("︵")) {
                lore.set(i, coloredEmpty);
                player.sendMessage("§a[DEBUG] ✓ ĐÃ THAY THÀNH CÔNG dòng " + (i+1) + " (dòng có ︵) → " + coloredEmpty);
                replaced = true;
                break;
            }

            
            if (!replaced && stripped.contains("Sát Thương I")) {
                lore.set(i, coloredEmpty);
                player.sendMessage("§a[DEBUG] ✓ Thay dòng " + (i+1) + " theo Sát Thương I");
                replaced = true;
                break;
            }
        }

        if (!replaced && !lore.isEmpty()) {
            
            lore.set(lore.size() - 1, coloredEmpty);
            player.sendMessage("§e[DEBUG] Fallback: Thay dòng cuối cùng");
            replaced = true;
        }

        if (replaced) {
            meta.setLore(lore);
            player.sendMessage("§a[DEBUG] Hoàn tất thay lore theo type.yml - THÀNH CÔNG!");
        } else {
            player.sendMessage("§c[DEBUG] Không thay được lore nào!");
        }
    }

    
    private ItemStack createGemItem(String gemId) {
        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
        if (gemId == null || !gemConfig.contains(gemId)) return null;
        return createGemItemInternal(gemId, gemConfig, "GEMSTONE");
    }

    private ItemStack createGemItemInternal(String id, FileConfiguration config, String itemTag) {
        try {
            String matStr = config.getString(id + ".material", "STONE");
            Material mat = Material.valueOf(matStr.toUpperCase());

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        config.getString(id + ".display-name", id)));

                List<String> loreList = new ArrayList<>();
                for (String line : config.getStringList(id + ".lore")) {
                    loreList.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(loreList);

                if (config.contains(id + ".model-id")) {
                    meta.setCustomModelData(config.getInt(id + ".model-id"));
                }

                NamespacedKey typeKey = new NamespacedKey(Main.getInstance(), "gem_item_type");
                NamespacedKey idKey = new NamespacedKey(Main.getInstance(), "gem_item_id");

                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, itemTag);
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);

                item.setItemMeta(meta);
            }
            return item;
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Lỗi tạo gem: " + id);
            return null;
        }
    }
}