package org.ThienNguyen.Evolution;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class EvolutionListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String mobId;

        // Kiểm tra xem có phải là MythicMob không
        if (MythicBukkit.inst().getMobManager().isMythicMob(event.getEntity())) {
            // Nếu là MythicMob, thêm tiền tố mm_ vào trước Internal Name
            String internalName = MythicBukkit.inst().getMobManager().getMythicMobInstance(event.getEntity()).getType().getInternalName();
            mobId = "mm_" + internalName;
        } else {
            // Nếu là quái Vanilla, giữ nguyên tên EntityType (Ví dụ: ZOMBIE)
            mobId = event.getEntityType().name();
        }

        checkAndApply(killer, mobId);
    }

    private void checkAndApply(Player player, String mobId) {
        // 1. Kiểm tra vũ khí trên tay chính
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && !mainHand.getType().isAir()) {
            EvolutionManager.addProgress(player, mainHand, mobId);
        }

        // 2. Kiểm tra bộ giáp (Mũ, Áo, Quần, Giày)
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack armor : armorContents) {
            if (armor != null && !armor.getType().isAir()) {
                EvolutionManager.addProgress(player, armor, mobId);
            }
        }
    }
}