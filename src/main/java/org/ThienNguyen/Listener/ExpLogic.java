package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class ExpLogic implements Listener {

    private boolean hasFabled = false;

    public ExpLogic() {
        this.hasFabled = Bukkit.getPluginManager().isPluginEnabled("Fabled");
    }

    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFabledExpGain(studio.magemonkey.fabled.api.event.PlayerExperienceGainEvent event) {
        if (!hasFabled) return;

        if (event.getSource() != studio.magemonkey.fabled.api.enums.ExpSource.MOB) return;

        Player player = event.getPlayerData().getPlayer();
        if (player == null) return;

        double originalAmount = event.getExp();
        if (originalAmount <= 0) return;

        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double bonusPercent = stats.totalExpBonus;

        if (bonusPercent > 0) {
            double bonusAmount = originalAmount * (bonusPercent / 100.0);
            event.setExp(originalAmount + bonusAmount);
        }
    }

    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        if (hasFabled) return;                    

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(killer.getUniqueId());
        if (stats.totalExpBonus <= 0) return;

        int bonusExp = (int) (event.getDroppedExp() * (stats.totalExpBonus / 100.0));
        if (bonusExp > 0) {
            killer.giveExp(bonusExp);
        }
    }
}