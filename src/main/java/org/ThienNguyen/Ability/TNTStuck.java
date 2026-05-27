package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class TNTStuck implements IAbility {

    @Override
    public String getName() {
        return "TNT_STUCK";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        
        double multiplier = 0.30 + (Math.max(0, level - 1) * 0.15);
        double finalDamage = baseDamage * multiplier;

        
        ArmorStand as = (ArmorStand) target.getWorld().spawnEntity(target.getLocation(), EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setMarker(true); 
        as.setSmall(true);
        as.setGravity(false);
        as.setBasePlate(false);
        as.getEquipment().setHelmet(new ItemStack(Material.TNT));

        
        target.addPassenger(as);

        
        new BukkitRunnable() {
            int ticks = 0;
            final int fuseTicks = 100; 

            @Override
            public void run() {
                
                if (!target.isValid() || target.isDead()) {
                    explode(attacker, as.getLocation(), finalDamage);
                    as.remove();
                    this.cancel();
                    return;
                }

                
                if (ticks % 10 == 0 || (ticks > 40 && ticks % 5 == 0)) {
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 2.0f);
                    target.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, target.getEyeLocation().add(0, 0.5, 0), 1);
                }

                if (ticks >= fuseTicks) {
                    explode(attacker, target.getLocation(), finalDamage);
                    as.remove();
                    this.cancel();
                    return;
                }

                ticks += 2;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    private void explode(Player attacker, Location loc, double damage) {
        
        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        
        loc.getWorld().getNearbyEntities(loc, 4, 4, 4).forEach(entity -> {
            if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof ArmorStand)) {
                victim.setNoDamageTicks(0);
                victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                victim.damage(damage, attacker);
                victim.removeMetadata("IS_ABILITY", Main.getInstance());
            }
        });
    }
}