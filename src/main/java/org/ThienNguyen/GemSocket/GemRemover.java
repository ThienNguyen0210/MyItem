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
import java.util.regex.Pattern;

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

        if (targetItem.getAmount() >= 2) {
            player.sendMessage(lang.getMessage("item.no-stack-allowed"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return;
        }

        NamespacedKey idKey = new NamespacedKey(Main.getInstance(), "gem_item_id");
        String removerId = removerMeta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);

        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
        if (removerId == null || !gemConfig.contains(removerId)) return;

        // Mỗi loại remover chỉ gỡ được ĐÚNG 1 loại ngọc (đọc từ "type" của chính remover
        // trong Gem.yml) — không còn gỡ ngẫu nhiên bất kỳ ngọc nào trên item nữa.
        String targetType = gemConfig.contains(removerId + ".type")
                ? gemConfig.getString(removerId + ".type")
                : null;

        if (targetType == null) {
            Main.getInstance().getLogger().warning(
                    "[GemRemover] Remover '" + removerId + "' thiếu 'type' trong Gem.yml — không rõ nó gỡ loại ngọc nào.");
            player.sendMessage("§cDụng cụ gỡ ngọc này chưa được cấu hình đúng! Vui lòng báo Admin.");
            return;
        }

        List<String> gemsOnItem = GemLogic.getGemsOnItem(targetItem);
        if (gemsOnItem.isEmpty()) {
            player.sendMessage("§cVật phẩm này không có ngọc nào để gỡ!");
            return;
        }

        List<String> matchingGems = new ArrayList<>();
        if (targetType.equalsIgnoreCase("ANY")) {
            // Remover loại "ANY": gỡ được ngọc thuộc bất kỳ độ hiếm nào.
            matchingGems.addAll(gemsOnItem);
        } else {
            for (String gemId : gemsOnItem) {
                String gemType = gemConfig.getString(gemId + ".type", "");
                if (gemType.equalsIgnoreCase(targetType)) {
                    matchingGems.add(gemId);
                }
            }
        }

        if (matchingGems.isEmpty()) {
            if (targetType.equalsIgnoreCase("ANY")) {
                player.sendMessage("§cVật phẩm này không có ngọc nào để gỡ!");
            } else {
                player.sendMessage("§cVật phẩm này không có ngọc loại §f" + targetType + " §cđể gỡ!");
            }
            return;
        }

        String removedGemId = matchingGems.get(random.nextInt(matchingGems.size()));
        String actualGemType = gemConfig.getString(removedGemId + ".type", targetType);

        ItemStack returnedGem = createGemItem(removedGemId);
        if (returnedGem != null) {
            if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                player.getInventory().setItemInMainHand(returnedGem);
            } else {
                player.getInventory().addItem(returnedGem);
            }
        }

        if (removeGemFromItem(targetItem, removedGemId, actualGemType)) {
            player.sendMessage("§aĐã gỡ ngọc §f" + removedGemId + " §athành công! Ngọc đã được trả về.");
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            new org.ThienNguyen.Listener.CacheListener().refreshCache(player);
        } else {
            player.sendMessage("§cKhông thể gỡ ngọc này!");
        }

        removerItem.setAmount(removerItem.getAmount() - 1);
    }

    /**
     * Gỡ ngọc khỏi item: cập nhật item_sockets (PDC) VÀ lore trên CÙNG MỘT
     * ItemMeta, rồi commit 1 lần duy nhất ở cuối. (Bug cũ: restoreEmptySocketLore
     * tự lấy 1 bản ItemMeta riêng qua item.getItemMeta(), sửa lore trên bản đó,
     * rồi hàm này lại setItemMeta bằng bản ItemMeta cũ hơn — ghi đè mất phần lore
     * vừa sửa. Ghép làm 1 object để tránh mất thay đổi.)
     */
    private boolean removeGemFromItem(ItemStack item, String gemIdToRemove, String socketType) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String currentData = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);
        if (currentData == null || currentData.isEmpty()) return false;

        String newData = currentData.replaceFirst(Pattern.quote(gemIdToRemove), "EMPTY_" + socketType);
        meta.getPersistentDataContainer().set(socketKey, PersistentDataType.STRING, newData);

        restoreEmptySocketLore(meta, gemIdToRemove, socketType);

        item.setItemMeta(meta);
        return true;
    }

    /**
     * Thay dòng lore của ngọc bị gỡ bằng dòng lỗ trống, dựa trên format thật của
     * ngọc đó (giống cách GemKham khớp dòng khi khảm ngọc, chỉ làm ngược lại) —
     * thay vì tìm theo chuỗi cứng như "Sát Thương I" hay ký tự "︵" (chỉ đúng
     * cho 1 ngọc cụ thể và sẽ sai với mọi ngọc khác).
     */
    private void restoreEmptySocketLore(ItemMeta meta, String gemIdToRemove, String socketType) {
        if (!meta.hasLore()) return;

        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();

        String emptyFormat = typeConfig.getString(socketType + ".format", "&7[ ○ ] Lỗ trống");
        String coloredEmpty = ChatColor.translateAlternateColorCodes('&', emptyFormat);

        String gemFormat = gemConfig.getString(gemIdToRemove + ".format");
        if (gemFormat == null || gemFormat.isEmpty()) {
            gemFormat = "&f[ ● ] " + gemConfig.getString(gemIdToRemove + ".display-name", gemIdToRemove);
        }
        String coloredGem = ChatColor.translateAlternateColorCodes('&', gemFormat);

        List<String> lore = new ArrayList<>(meta.getLore());
        boolean replaced = false;

        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).equals(coloredGem)) {
                lore.set(i, coloredEmpty);
                replaced = true;
                break;
            }
        }

        // Fallback: so sánh không màu, phòng trường hợp mã màu lệch nhưng nội dung giống nhau.
        if (!replaced) {
            String strippedGem = ChatColor.stripColor(coloredGem);
            for (int i = 0; i < lore.size(); i++) {
                if (ChatColor.stripColor(lore.get(i)).equals(strippedGem)) {
                    lore.set(i, coloredEmpty);
                    replaced = true;
                    break;
                }
            }
        }

        if (replaced) {
            meta.setLore(lore);
        } else {
            Main.getInstance().getLogger().warning(
                    "[GemRemover] Không tìm thấy dòng lore của ngọc '" + gemIdToRemove
                            + "' để khôi phục — lore có thể không đồng bộ với item_sockets.");
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