package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DashSkill implements ISkill {
    @Override public String getName() { return "Dash"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamage) {
        
        double power = 1.8 + (level * 0.2);

        
        Vector direction = player.getLocation().getDirection();
        
        direction.setY(0.2).normalize();

        
        player.setVelocity(direction.multiply(power));

        
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.2, 0.1, 0.2, 0.05);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getEyeLocation(), 1, 0, 0, 0, 0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.8f, 2.0f);

        
        player.setFallDistance(-10.0f);

        
        
        double shockDamage = baseDamage * 0.5; 

        for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player)) {

                
                player.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                victim.damage(shockDamage, player);

                
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);

                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) player.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 1L);
            }
        }
    }
}