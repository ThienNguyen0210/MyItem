package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; 
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DarknessDevourSkill implements ISkill {
    @Override public String getName() { return "DarknessDevour"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        
        
        double damagePerPulse = realPower * (0.10 + (level * 0.05));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 1.5f, 0.1f);

        new BukkitRunnable() {
            int pulses = 0;

            @Override
            public void run() {
                if (pulses >= 10 || !player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location center = player.getLocation();

                
                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = Math.random() * 3.0;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    double y = Math.random() * 2.0;

                    Location particleLoc = center.clone().add(x, y, z);
                    player.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 1, 0, 0, 0, 0.02);
                    player.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 2, new Particle.DustOptions(Color.BLACK, 1.5f));
                }

                
                for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                        
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));

                        
                        
                        victim.setNoDamageTicks(0);

                        
                        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                        
                        victim.damage(damagePerPulse, player);

                        
                        victim.getWorld().spawnParticle(Particle.SQUID_INK, victim.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0.05);

                        
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (victim.isValid()) {
                                    victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }
                        }.runTaskLater(Main.getInstance(), 2L);
                    }
                }
                pulses++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 10L);
    }
}