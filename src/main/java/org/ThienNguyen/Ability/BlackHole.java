package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class BlackHole implements IAbility {

    @Override
    public String getName() {
        return "BLACK_HOLE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        
        int durationTicks = 60 + (Math.max(0, level - 1) * 10);
        double radius = 5.0;

        
        Location holeLoc = (target != null) ? target.getLocation().add(0, 1, 0) : attacker.getLocation().add(attacker.getLocation().getDirection().multiply(3)).add(0, 1, 0);

        
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    this.cancel();
                    return;
                }

                
                drawBlackSphere(holeLoc);

                
                List<Entity> nearby = (List<Entity>) holeLoc.getWorld().getNearbyEntities(holeLoc, radius, radius, radius);
                for (Entity entity : nearby) {
                    if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                        
                        Vector pull = holeLoc.toVector().subtract(victim.getLocation().toVector()).normalize().multiply(0.4);

                        
                        victim.setVelocity(pull);

                        
                        victim.getWorld().spawnParticle(Particle.SMOKE_NORMAL, victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0.01);
                    }
                }

                
                if (ticks % 10 == 0) {
                    holeLoc.getWorld().playSound(holeLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
                }

                ticks += 2; 
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    private void drawBlackSphere(Location loc) {
        
        for (int i = 0; i < 8; i++) {
            Vector v = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(0.8);
            Location p = loc.clone().add(v);

            
            loc.getWorld().spawnParticle(Particle.REDSTONE, p, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.BLACK, 1.5f));

            
            loc.getWorld().spawnParticle(Particle.PORTAL, p, 1, 0, 0, 0, 0.1);
        }
    }
}