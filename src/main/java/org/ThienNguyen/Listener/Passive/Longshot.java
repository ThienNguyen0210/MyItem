package org.ThienNguyen.Listener.Passive;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class Longshot implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArcherAttack(EntityDamageByEntityEvent event) {
        
        if (!Bukkit.getPluginManager().isPluginEnabled("SkillAPI") &&
                !Bukkit.getPluginManager().isPluginEnabled("Fabled")) {
            return;
        }

        
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null || !(event.getEntity() instanceof LivingEntity target)) return;

        
        try {
            
            PlayerData skillData = SkillAPI.getPlayerData(attacker);
            if (skillData == null || !skillData.hasClass() || skillData.getMainClass() == null) return;

            
            String className = skillData.getMainClass().getData().getName();
            if (className == null || !className.equalsIgnoreCase("Archer")) return;

            
            
            double distanceSq = attacker.getLocation().distanceSquared(target.getLocation());
            if (distanceSq < 1.0) return;

            double distance = Math.sqrt(distanceSq);

            
            int level = skillData.getMainClass().getLevel();

            
            
            
            double percentPerBlock = 5.0 + (level / 100.0 * 1.0);
            double totalBonusPercent = distance * percentPerBlock;

            
            double originalDamage = event.getDamage();
            double extraDamage = originalDamage * (totalBonusPercent / 100.0);

            event.setDamage(originalDamage + extraDamage);

        } catch (NoClassDefFoundError | Exception e) {
            
        }
    }
}