package org.ThienNguyen.Utils;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUI implements Listener {

    private final String GUI_TITLE = "§0Cường Hóa Vật Phẩm";
    private final int SLOT_ITEM = 10;
    private final int SLOT_GEM = 12;
    private final int SLOT_PROTECT = 20;
    private final int SLOT_BUTTON = 14;
    private final int SLOT_RESULT = 16;

    private final Upgrade upgradeProcessor = new Upgrade();

    public void openUpgrade(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            if (i == SLOT_ITEM || i == SLOT_GEM || i == SLOT_BUTTON ||
                    i == SLOT_RESULT || i == SLOT_PROTECT) continue;
            inv.setItem(i, filler);
        }

        updateButton(inv, 0, 0);
        player.openInventory(inv);
    }

    private void updateButton(Inventory inv, double chance, double cost) {
        ItemStack button = new ItemStack(Material.ANVIL);
        ItemMeta btnMeta = button.getItemMeta();
        if (btnMeta != null) {
            btnMeta.setDisplayName("§e§lBẤM ĐỂ NÂNG CẤP");
            List<String> lore = new ArrayList<>();
            lore.add("§7Đặt vật phẩm và nguyên liệu vào");

            if (chance > 0) {
                lore.add("");
                lore.add("§fTỉ lệ thành công: §a" + (int) chance + "%");

                // Render chi phí nâng cấp
                lore.add("§fChi phí: §6" + String.format("%,.0f", cost) + "$");

                ItemStack protect = inv.getItem(SLOT_PROTECT);
                if (upgradeProcessor.isProtectionScroll(protect)) {
                    lore.add("");
                    lore.add("§b✔ Đã kích hoạt Bùa Hộ Mệnh");
                }
            } else if (chance == -1) {
                lore.add("");
                lore.add("§cĐá cường hóa không hợp lệ!");
            } else {
                lore.add("");
                lore.add("§cThiếu trang bị hoặc đá...");
            }

            btnMeta.setLore(lore);
            button.setItemMeta(btnMeta);
        }
        inv.setItem(SLOT_BUTTON, button);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        var lang = Main.getInstance().getLangManager(); // Thêm manager ở đây
        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (currentItem != null && currentItem.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            event.setCancelled(true);
            return;
        }

        if (slot == SLOT_BUTTON) {
            event.setCancelled(true);
            handleUpgradeAction(player, inv);
            return;
        }

        if (slot == SLOT_RESULT) {
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getClick().isShiftClick() && event.getRawSlot() >= inv.getSize()) {
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                if (upgradeProcessor.isValidGem(currentItem) || upgradeProcessor.isProtectionScroll(currentItem)) {
                    // Để mặc định
                } else {
                    if (inv.getItem(SLOT_ITEM) == null || inv.getItem(SLOT_ITEM).getType() == Material.AIR) {
                        if (currentItem.getAmount() > 1) {
                            event.setCancelled(true);
                            // Sửa ở đây
                            player.sendMessage(lang.getMessage("upgrade.only-one-item"));
                            return;
                        }
                    } else {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            if (slot == SLOT_ITEM) {
                if (cursorItem.getAmount() > 1) {
                    event.setCancelled(true);
                    // Sửa ở đây
                    player.sendMessage(lang.getMessage("upgrade.only-one-item"));
                    return;
                }
            }

            if (slot == SLOT_GEM) {
                if (!upgradeProcessor.isValidGem(cursorItem)) {
                    event.setCancelled(true);
                    // Sửa ở đây
                    player.sendMessage(lang.getMessage("upgrade.not-a-gem"));
                    return;
                }
            }

            if (slot == SLOT_PROTECT) {
                if (!upgradeProcessor.isProtectionScroll(cursorItem)) {
                    event.setCancelled(true);
                    // Sửa ở đây
                    player.sendMessage(lang.getMessage("upgrade.not-a-scroll"));
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> refreshChance(inv), 1L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        for (int slot : event.getRawSlots()) {
            if (slot == SLOT_GEM || slot == SLOT_PROTECT || slot == SLOT_ITEM) {
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> refreshChance(event.getInventory()), 1L);
    }

    private void refreshChance(Inventory inv) {
        ItemStack item = inv.getItem(SLOT_ITEM);
        ItemStack gem = inv.getItem(SLOT_GEM);

        if (item == null || item.getType() == Material.AIR ||
                gem == null || gem.getType() == Material.AIR) {
            updateButton(inv, 0, 0); // Không có đồ thì cost = 0
            return;
        }

        int gemId = upgradeProcessor.getGemId(gem);
        if (gemId <= 0) {
            updateButton(inv, -1, 0);
            return;
        }

        // Tính tỷ lệ thành công
        double realChance = upgradeProcessor.calculateFinalChance(item, gem);

        // Tính chi phí dựa trên cấp độ tiếp theo (level hiện tại + 1)
        int nextLevel = upgradeProcessor.getItemLevel(item) + 1;
        double cost = upgradeProcessor.getUpgradeCost(nextLevel);

        updateButton(inv, realChance, cost);
    }

    private void handleUpgradeAction(Player player, Inventory inv) {
        var lang = Main.getInstance().getLangManager(); // Thêm manager ở đây
        ItemStack item = inv.getItem(SLOT_ITEM);
        ItemStack gem = inv.getItem(SLOT_GEM);
        ItemStack protect = inv.getItem(SLOT_PROTECT);

        if (item == null || item.getType().isAir() || gem == null || gem.getType().isAir()) {
            // Sửa ở đây
            player.sendMessage(lang.getMessage("upgrade.missing-materials"));
            return;
        }

        if (inv.getItem(SLOT_RESULT) != null && inv.getItem(SLOT_RESULT).getType() != Material.AIR) {
            // Sửa ở đây
            player.sendMessage(lang.getMessage("upgrade.take-result-first"));
            return;
        }

        boolean success = upgradeProcessor.processUpgrade(player, item, gem, protect);

        ItemStack resultStack = item.clone();
        inv.setItem(SLOT_ITEM, null);
        inv.setItem(SLOT_RESULT, resultStack);

        if (gem.getAmount() <= 0) inv.setItem(SLOT_GEM, null);
        if (protect != null && protect.getAmount() <= 0) inv.setItem(SLOT_PROTECT, null);

        updateButton(inv, 0, 0);
        player.playSound(player.getLocation(), success ? Sound.ENTITY_PLAYER_LEVELUP : Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        int[] returnSlots = {SLOT_ITEM, SLOT_GEM, SLOT_RESULT, SLOT_PROTECT};
        for (int slot : returnSlots) {
            ItemStack is = inv.getItem(slot);
            if (is != null && is.getType() != Material.AIR) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), is);
                } else {
                    player.getInventory().addItem(is);
                }
                inv.setItem(slot, null);
            }
        }
    }
}