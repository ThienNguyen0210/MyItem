package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DeathMarkSkill implements ISkill {
    @Override public String getName() { return "DeathMark"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        
        LivingEntity target = null;
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(15, 15, 15)) {
            if (e instanceof LivingEntity victim && !e.equals(player) && !(e instanceof ArmorStand)) {
                Vector toTarget = victim.getLocation().toVector().subtract(player.getEyeLocation().toVector());
                if (player.getEyeLocation().getDirection().angle(toTarget) < 0.2) { 
                    target = victim;
                    break;
                }
            }
        }

        if (target == null) {
            player.sendMessage("§cKhông tìm thấy mục tiêu trong tầm nhìn!");
            return;
        }

        final LivingEntity finalTarget = target;

        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = (stats != null) ? stats.totalBonusDmg : player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        double explosionDmg = realPower * (0.50 + (level * 0.15));

        
        ArmorStand mark = (ArmorStand) finalTarget.getWorld().spawnEntity(finalTarget.getLocation().add(0, 2.2, 0), EntityType.ARMOR_STAND);
        mark.setVisible(false);
        mark.setGravity(false);
        mark.setMarker(true);
        mark.getEquipment().setHelmet(new ItemStack(Material.SKELETON_SKULL));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);

        
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!finalTarget.isValid() || !player.isOnline() || ticks >= 60) {

                    if (finalTarget.isValid()) {
                        
                        Location loc = finalTarget.getLocation();
                        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
                        loc.getWorld().spawnParticle(Particle.SOUL, loc, 20, 0.5, 0.5, 0.5, 0.1);
                        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

                        
                        finalTarget.setNoDamageTicks(0);
                        finalTarget.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        finalTarget.damage(explosionDmg, player);

                        
                        player.teleport(finalTarget.getLocation().add(player.getLocation().getDirection().multiply(-1)));

                        new BukkitRunnable() {
                            @Override
                            public void run() { if (finalTarget.isValid()) finalTarget.removeMetadata("IS_ABILITY", Main.getInstance()); }
                        }.runTaskLater(Main.getInstance(), 2L);
                    }

                    mark.remove();
                    this.cancel();
                    return;
                }

                
                Location markLoc = finalTarget.getLocation().add(0, 2.5, 0);
                mark.teleport(markLoc); 
                markLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, markLoc, 3, 0.1, 0.1, 0.1, 0.02);

                if (ticks % 20 == 0) { 
                    player.getWorld().playSound(finalTarget.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}