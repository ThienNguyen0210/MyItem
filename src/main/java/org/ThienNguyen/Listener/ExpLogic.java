package org.ThienNguyen.Listener;

import net.Indyuce.mmocore.api.player.PlayerData;
import org.ThienNguyen.Main;
import org.ThienNguyen.Stat.ExpBonus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class ExpLogic implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;

        // 1. Tính tổng % Exp Bonus từ trang bị người chơi đang mặc/cầm
        double totalBonusPercent = getExpBonusFromPlayer(player);
        if (totalBonusPercent <= 0) return;

        // 2. Lấy EXP ban đầu (Trước khi hệ thống cộng EXP của Mob)
        double initialExp = getCurrentExp(player);

        // 3. Đợi 0.2 giây (4 ticks) để MMOCore/Vanilla cộng EXP gốc của Mob xong
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!player.isOnline()) return;

            double expAfterKill = getCurrentExp(player);
            double expGained = expAfterKill - initialExp;

            // Nếu thực sự có nhận được EXP thì mới tính bonus
            if (expGained > 0) {
                // Thuật toán: value 50 -> nhân 0.5 (tức là 50/100)
                double bonusAmount = expGained * (totalBonusPercent / 100.0);

                giveExp(player, bonusAmount);
            }
        }, 4L);
    }

    // Hàm lấy EXP hiện tại (Tự động nhận diện MMOCore hoặc Vanilla)
    private double getCurrentExp(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            return PlayerData.get(player).getExperience();
        } else {
            // Vanilla dùng tổng EXP dựa trên level và progress
            return player.getTotalExperience();
        }
    }

    // Hàm cộng thêm EXP (Tự động nhận diện MMOCore hoặc Vanilla)
    private void giveExp(Player player, double amount) {
        if (amount <= 0) return;

        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            PlayerData.get(player).giveExperience(amount, null); // null để không hiện thông báo trùng lặp nếu cần
        } else {
            player.giveExp((int) amount);
        }
    }

    private double getExpBonusFromPlayer(Player player) {
        double total = 0;
        // Kiểm tra 4 món giáp
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            total += ExpBonus.get(armor);
        }
        // Kiểm tra tay chính và tay phụ
        total += ExpBonus.get(player.getInventory().getItemInMainHand());
        total += ExpBonus.get(player.getInventory().getItemInOffHand());
        return total;
    }
}