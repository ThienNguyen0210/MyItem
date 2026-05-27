package org.ThienNguyen.Listener.Passive;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.player.PlayerData;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class Blood implements Listener {

    private final Random random = new Random();
    private final String METADATA_BLOCK = "IS_SKILL_PROCESS";

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAssassinAttack(EntityDamageByEntityEvent event) {
        
        if (!Bukkit.getPluginManager().isPluginEnabled("SkillAPI") &&
                !Bukkit.getPluginManager().isPluginEnabled("Fabled")) {
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        
        if (target.hasMetadata(METADATA_BLOCK)) return;

        try {
            
            PlayerData skillData = SkillAPI.getPlayerData(attacker);
            if (skillData == null || !skillData.hasClass() || skillData.getMainClass() == null) return;

            String className = skillData.getMainClass().getData().getName();
            if (className == null || !className.equalsIgnoreCase("Assassin")) return;

            
            if (random.nextDouble() * 100 > 25.0) return;

            
            double finalDamage = event.getDamage();

            
            int level = skillData.getMainClass().getLevel();
            double bleedPercent = 2.0 + (level / 100.0);
            double damagePerTick = finalDamage * (bleedPercent / 100.0);

            
            new BukkitRunnable() {
                int count = 0;
                final int maxTicks = 3;

                @Override
                public void run() {
                    
                    if (target == null || target.isDead() || !target.isValid() || count >= maxTicks) {
                        this.cancel();
                        return;
                    }

                    try {
                        
                        target.setMetadata(METADATA_BLOCK, new FixedMetadataValue(Main.getInstance(), true));

                        
                        target.damage(damagePerTick, attacker);

                    } finally {
                        
                        
                        target.removeMetadata(METADATA_BLOCK, Main.getInstance());
                    }

                    count++;
                }
            }.runTaskTimer(Main.getInstance(), 20L, 20L);

        } catch (NoClassDefFoundError | Exception ignored) {
            
        }
    }
}