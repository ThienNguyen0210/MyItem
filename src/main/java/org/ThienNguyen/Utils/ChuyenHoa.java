package org.ThienNguyen.Utils;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ChuyenHoa implements Listener {

    private final String title = "§0Chuyển Hóa Cấp Độ";
    private final int SLOT_A = 11;
    private final int SLOT_B = 15;
    private final int SLOT_BUTTON = 22;

    
    private static final NamespacedKey LEVEL_KEY = new NamespacedKey(Main.getInstance(), "upgrade_level");

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Upgrade upgradeUtils = new Upgrade();
    private final Random random = new Random();

    private double successRate;
    private int minLevel;

    public ChuyenHoa() {
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = Main.getInstance().getChuyenHoaConfig();
        if (config != null) {
            this.successRate = config.getDouble("settings.success-rate", 70.0);
            this.minLevel = config.getInt("settings.min-level-required", 1);
        }
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, title);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName(" ");
            filler.setItemMeta(fMeta);
        }

        for (int i = 0; i < 36; i++) {
            if (i != SLOT_A && i != SLOT_B && i != SLOT_BUTTON) {
                gui.setItem(i, filler);
            }
        }

        ItemStack button = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta bMeta = button.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName("§e§lBẤM ĐỂ CHUYỂN HÓA");
            button.setItemMeta(bMeta);
        }
        gui.setItem(SLOT_BUTTON, button);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(title)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        
        if (slot >= 0 && slot < 36) {
            if (slot != SLOT_A && slot != SLOT_B && slot != SLOT_BUTTON) {
                event.setCancelled(true);
                return;
            }
        }

        if (slot == SLOT_BUTTON) {
            event.setCancelled(true);
            handleTransfer(player, event.getInventory());
        }
    }

    private void handleTransfer(Player player, Inventory inv) {
        var lang = Main.getInstance().getLangManager(); 
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(player.getUniqueId())) {
            long left = (cooldowns.get(player.getUniqueId()) + 5000) - now;
            if (left > 0) {
                
                player.sendMessage(lang.getMessage("item.cooldown", "{time}", String.valueOf(left / 1000 + 1)));
                return;
            }
        }

        ItemStack itemA = inv.getItem(SLOT_A);
        ItemStack itemB = inv.getItem(SLOT_B);

        if (itemA == null || itemB == null || itemA.getType().isAir() || itemB.getType().isAir()) {
            
            player.sendMessage(lang.getMessage("item.need-two-items"));
            return;
        }

        int levelA = upgradeUtils.getItemLevel(itemA);
        int levelB = upgradeUtils.getItemLevel(itemB);

        if (levelA < minLevel || levelB < minLevel) {
            
            player.sendMessage(lang.getMessage("item.min-level", "{level}", String.valueOf(minLevel)));
            return;
        }

        if (random.nextDouble() * 100 < successRate) {
            simulateLevelChange(itemA, levelA, levelB);
            simulateLevelChange(itemB, levelB, levelA);
            finalizeItem(itemA, levelB);
            finalizeItem(itemB, levelA);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            
            player.sendMessage(lang.getMessage("item.success"));
        } else {
            applyPunishment(itemA, levelA);
            applyPunishment(itemB, levelB);

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
            
            player.sendMessage(lang.getMessage("item.failed"));
        }

        cooldowns.put(player.getUniqueId(), now);
    }

    private void simulateLevelChange(ItemStack item, int from, int to) {
        int diff = Math.abs(to - from);
        boolean isUpgrading = to > from;
        for (int i = 0; i < diff; i++) {
            upgradeUtils.applyStats(item, isUpgrading);
        }
    }

    private void applyPunishment(ItemStack item, int currentLevel) {
        if (currentLevel > 0) {
            int newLevel = currentLevel - 1;
            upgradeUtils.applyStats(item, false);
            finalizeItem(item, newLevel);
        }
    }

    private void finalizeItem(ItemStack item, int newLevel) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        
        if (newLevel <= 0) {
            meta.getPersistentDataContainer().remove(LEVEL_KEY);
            
            NamespacedKey baseNameKey = new NamespacedKey(Main.getInstance(), "base_name");
            meta.getPersistentDataContainer().remove(baseNameKey);
        } else {
            meta.getPersistentDataContainer().set(LEVEL_KEY, PersistentDataType.INTEGER, newLevel);
        }

        
        item.setItemMeta(meta);

        
        
        upgradeUtils.updateItemName(item, newLevel);

        
        org.ThienNguyen.Lore.StatsLore.updateLore(item);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(title)) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        
        int[] slots = {SLOT_A, SLOT_B};
        for (int slot : slots) {
            ItemStack is = inv.getItem(slot);
            if (is != null && is.getType() != Material.AIR) {
                player.getInventory().addItem(is).values().forEach(remaining ->
                        player.getWorld().dropItemNaturally(player.getLocation(), remaining)
                );
                inv.setItem(slot, null);
            }
        }
    }
}