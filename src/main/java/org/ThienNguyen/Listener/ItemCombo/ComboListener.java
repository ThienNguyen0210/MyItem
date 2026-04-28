package org.ThienNguyen.Listener.ItemCombo;

import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.CacheListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class ComboListener implements Listener {

    public static final String METADATA_KEY = "current_active_combo";
    // Đảm bảo Key này khớp với class ComboItem của bạn
    public static final NamespacedKey COMBO_KEY = new NamespacedKey("windy", "combo_id");

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            updateComboStatus(player);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Kiểm tra interact để cập nhật khi người dùng đeo giáp bằng chuột phải
        ItemStack item = event.getItem();
        if (item != null && (item.getType().name().contains("HELMET") ||
                item.getType().name().contains("CHESTPLATE") ||
                item.getType().name().contains("LEGGINGS") ||
                item.getType().name().contains("BOOTS"))) {
            updateComboStatus(event.getPlayer());
        }
    }
    @EventHandler
    public void onHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        updateComboStatus(event.getPlayer());
    }
    private void updateComboStatus(Player player) {
        // Delay 1-2 tick để server cập nhật trang bị vào slot trước khi quét
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                String oldId = player.hasMetadata(METADATA_KEY) ? player.getMetadata(METADATA_KEY).get(0).asString() : null;
                String currentId = getFullSetComboId(player);

                if ((currentId == null && oldId != null) || (currentId != null && !currentId.equals(oldId))) {
                    if (currentId != null) {
                        player.setMetadata(METADATA_KEY, new FixedMetadataValue(Main.getInstance(), currentId));
                        String msg = Main.getInstance().getComboConfig().getString(currentId + ".message", "&aKích hoạt combo " + currentId);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    } else {
                        player.removeMetadata(METADATA_KEY, Main.getInstance());
                    }

                    // Gọi hàm tính lại chỉ số trong CacheListener
                    CacheListener.refreshCache(player);
                }
            }
        }.runTaskLater(Main.getInstance(), 2L);
    }

    public static String getFullSetComboId(Player player) {
        FileConfiguration comboConfig = Main.getInstance().getComboConfig();
        if (comboConfig == null) return null;

        for (String comboId : comboConfig.getKeys(false)) {
            List<String> requiredSlots = comboConfig.getStringList(comboId + ".require-slots");

            // TRƯỜNG HỢP 1: Tự định nghĩa slot (Head, Mainhand...)
            if (requiredSlots != null && !requiredSlots.isEmpty()) {
                boolean allMatch = true;
                for (String slotName : requiredSlots) {
                    EquipmentSlot slot = parseSlot(slotName);
                    if (slot == null || !isMatchingCombo(player.getInventory().getItem(slot), comboId)) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) return comboId;
            }
            // TRƯỜNG HỢP 2: Mặc định (Kiểm tra 4 món giáp)
            else if (checkBasicArmorSet(player, comboId)) {
                return comboId;
            }
        }
        return null;
    }

    private static boolean isMatchingCombo(ItemStack item, String comboId) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;

        // Cách 1: Thử lấy theo key mặc định của plugin
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "combo_id");
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        // Cách 2: Nếu id vẫn null, thử lấy theo key "windy:combo_id" (nếu bạn lỡ hardcode ở đâu đó)
        if (id == null) {
            NamespacedKey backupKey = new NamespacedKey("windy", "combo_id");
            id = item.getItemMeta().getPersistentDataContainer().get(backupKey, PersistentDataType.STRING);
        }

        return comboId.equalsIgnoreCase(id);
    }

    private static boolean checkBasicArmorSet(Player player, String comboId) {
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            if (!isMatchingCombo(player.getInventory().getItem(slot), comboId)) {
                return false;
            }
        }
        return true;
    }

    private static EquipmentSlot parseSlot(String name) {
        return switch (name.toLowerCase()) {
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "boots", "feet" -> EquipmentSlot.FEET;
            case "mainhand", "hand" -> EquipmentSlot.HAND;
            case "offhand" -> EquipmentSlot.OFF_HAND;
            default -> null;
        };
    }
}