package org.ThienNguyen.GemSocket;

import org.ThienNguyen.Main;
import org.ThienNguyen.Lore.LoreGenerator;
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

public class GemKham implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onGemApply(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;

        ItemStack gemItem = event.getCursor();
        ItemStack targetItem = event.getCurrentItem();

        if (gemItem == null || targetItem == null || targetItem.getType() == Material.AIR) return;

        ItemMeta gemMeta = gemItem.getItemMeta();
        if (gemMeta == null) return;

        NamespacedKey typeKey = new NamespacedKey(Main.getInstance(), "gem_item_type");
        String itemType = gemMeta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

        if (itemType == null || !itemType.equals("GEMSTONE")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        var lang = Main.getInstance().getLangManager();

        if (targetItem.getAmount() >= 2) {
            player.sendMessage(lang.getMessage("item.no-stack-allowed"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return;
        }

        NamespacedKey idKey = new NamespacedKey(Main.getInstance(), "gem_item_id");
        String gemId = gemMeta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        FileConfiguration gemConfig = Main.getInstance().getGemConfig();

        if (gemId == null || !gemConfig.contains(gemId)) return;

        String gemRequiredType = gemConfig.getString(gemId + ".type", "common");

        // ====================== SỬA Ở ĐÂY ======================
        int currentGems = getTotalGems(targetItem);
        int totalSockets = getTotalSocketCount(targetItem); // Sử dụng hàm tính TỔNG số lỗ

        if (currentGems >= totalSockets) {
            player.sendMessage(lang.getMessage("item.max-gem-limit"));
            return;
        }

        if (!hasEmptySocket(targetItem, gemRequiredType)) {
            player.sendMessage(lang.getMessage("item.no-empty-socket", "{type}", gemRequiredType));
            return;
        }

        gemItem.setAmount(gemItem.getAmount() - 1);

        int successRate = gemConfig.getInt(gemId + ".success", 100);
        if (random.nextInt(100) < successRate) {
            applyGemToItem(targetItem, gemId, gemRequiredType);

            if (gemConfig.contains(gemId + ".apply.element")) {
                for (String line : gemConfig.getStringList(gemId + ".apply.element")) {
                    String[] p = line.split(":");
                    if (p.length == 2) {
                        try {
                            org.ThienNguyen.Element.ElementCore.addElement(targetItem, p[0].trim().toUpperCase(), Integer.parseInt(p[1].trim()));
                        } catch (Exception ignored) {}
                    }
                }
            }

            player.sendMessage(lang.getMessage("item.apply-success"));
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
            new org.ThienNguyen.Listener.CacheListener().refreshCache(player);
        } else {
            player.sendMessage(lang.getMessage("item.apply-failed"));
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1);
        }
    }

    // ====================== HÀM MỚI - ĐẾM SỐ LỖ TRỐNG ======================
    private int getTotalSocketCount(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String data = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) return 0;

        // Chỉ cần đếm số lượng phần tử sau khi split dấu |
        return data.split("\\|").length;
    }

    // Các hàm cũ giữ nguyên hoặc tinh chỉnh nhẹ
    private int getTotalGems(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String data = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) return 0;

        int count = 0;
        for (String s : data.split("\\|")) {
            if (!s.startsWith("EMPTY_") && !s.isEmpty()) count++;
        }
        return count;
    }

    private boolean hasEmptySocket(ItemStack item, String type) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String data = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);
        return data != null && data.contains("EMPTY_" + type);
    }

    private void applyGemToItem(ItemStack item, String gemId, String type) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();

        // 1. Cập nhật dữ liệu ẩn (PDC)
        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String currentData = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);
        if (currentData != null) {
            String newData = currentData.replaceFirst("EMPTY_" + type, gemId);
            meta.getPersistentDataContainer().set(socketKey, PersistentDataType.STRING, newData);
        }

        // 2. Xử lý Lore (Chỉ replace ký tự đích, giữ nguyên hoa văn)
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Lấy chuỗi định dạng lỗ trống (Ví dụ: "&7Lỗ trống")
        String emptyFormat = typeConfig.getString(type + ".format", "Lỗ trống");
        String coloredEmpty = ChatColor.translateAlternateColorCodes('&', emptyFormat);

        // Lấy format của ngọc (Ví dụ: "&f︵&c Sát Thương I")
        String gemFormat = gemConfig.getString(gemId + ".format");
        if (gemFormat == null || gemFormat.isEmpty()) {
            gemFormat = "&f[ ● ] " + gemConfig.getString(gemId + ".display-name", gemId);
        }
        String coloredGem = ChatColor.translateAlternateColorCodes('&', gemFormat);

        boolean replaced = false;
        for (int i = 0; i < lore.size(); i++) {
            String originalLine = lore.get(i);

            // Kiểm tra xem dòng này có chứa chuỗi định dạng lỗ trống không
            if (originalLine.contains(coloredEmpty)) {
                // CHỈ REPLACE CỤM TỪ LỖ TRỐNG, GIỮ NGUYÊN TRƯỚC VÀ SAU
                String newLine = originalLine.replace(coloredEmpty, coloredGem);
                lore.set(i, newLine);
                replaced = true;
                break;
            }
        }

        // Nếu không tìm thấy dòng nào chứa định dạng chuẩn, thử quét theo text thuần (fallback)
        if (!replaced) {
            String cleanEmptyKey = ChatColor.stripColor(coloredEmpty).trim();
            for (int i = 0; i < lore.size(); i++) {
                if (ChatColor.stripColor(lore.get(i)).contains(cleanEmptyKey)) {
                    // Nếu tìm thấy theo text thuần, thay nguyên dòng để tránh lỗi màu
                    lore.set(i, coloredGem);
                    replaced = true;
                    break;
                }
            }
        }

        // Nếu vẫn không thấy thì mới add vào cuối
        if (!replaced) {
            lore.add(coloredGem);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}