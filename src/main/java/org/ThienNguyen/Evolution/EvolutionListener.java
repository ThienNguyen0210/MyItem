package org.ThienNguyen.Evolution;

import org.bukkit.Bukkit;
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

        String mobId = null;

        
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            try {
                
                if (io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().isMythicMob(event.getEntity())) {
                    String internalName = io.lumine.mythic.bukkit.MythicBukkit.inst()
                            .getMobManager()
                            .getMythicMobInstance(event.getEntity())
                            .getType()
                            .getInternalName();
                    mobId = "mm_" + internalName;
                }
            } catch (Throwable ignored) {
                
            }
        }

        
        if (mobId == null) {
            mobId = event.getEntityType().name();
        }

        checkAndApply(killer, mobId);
    }

    private void checkAndApply(Player player, String mobId) {
        
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && !mainHand.getType().isAir()) {
            EvolutionManager.addProgress(player, mainHand, mobId);
        }

        
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack armor : armorContents) {
            if (armor != null && !armor.getType().isAir()) {
                EvolutionManager.addProgress(player, armor, mobId);
            }
        }

    }
}