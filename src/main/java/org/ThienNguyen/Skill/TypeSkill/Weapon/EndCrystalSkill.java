package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EndCrystalSkill implements ISkill {
    @Override public String getName() { return "VoidCrystal"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;
        if (realPower <= 0) realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();

        
        double damagePerSecond = realPower * (0.15 + (level * 0.10));

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2)).add(0, 1, 0);

        
        EnderCrystal crystal = (EnderCrystal) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ENDER_CRYSTAL);
        crystal.setShowingBottom(false);
        crystal.setMetadata("UNBREAKABLE_CRYSTAL", new FixedMetadataValue(Main.getInstance(), true));

        player.getWorld().playSound(spawnLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.5f);

        
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 5 || !player.isOnline()) {
                    crystal.remove();
                    player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, crystal.getLocation(), 1);
                    player.getWorld().playSound(crystal.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1.0f, 0.5f);
                    this.cancel();
                    return;
                }

                Location cLoc = crystal.getLocation().add(0, 0.5, 0);

                
                cLoc.getWorld().spawnParticle(Particle.REDSTONE, cLoc, 10, 0.5, 0.5, 0.5, 0,
                        new Particle.DustOptions(Color.PURPLE, 1.0f));

                
                for (Entity entity : crystal.getNearbyEntities(10, 10, 10)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                        
                        drawBeam(cLoc, victim.getEyeLocation());

                        
                        victim.setNoDamageTicks(0);
                        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        victim.damage(damagePerSecond, player);

                        
                        new BukkitRunnable() {
                            @Override
                            public void run() { if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance()); }
                        }.runTaskLater(Main.getInstance(), 2L);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L); 
    }

    private void drawBeam(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = start.distance(end);
        direction.normalize();

        for (double d = 0; d < distance; d += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(d));
            start.getWorld().spawnParticle(Particle.REDSTONE, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(128, 0, 128), 0.6f));
        }
    }
}