package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; 
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TiaChopLienHoanSkill implements ISkill {
    private final Random random = new Random();

    @Override public String getName() { return "TiaChopLienHoan"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "LEFT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());

        
        
        double realPower = stats.totalBonusDmg;

        

        
        double finalDmg = realPower * (0.50 + (level * 0.10));

        

        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = startLoc.getDirection().normalize();
        List<LivingEntity> hitEntities = new ArrayList<>();

        LivingEntity firstTarget = null;
        for (double d = 1; d <= 12; d += 0.5) {
            Location point = startLoc.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.05, 0.05, 0.05, 0.01);

            if (random.nextDouble() < 0.15) {
                Vector branch = new Vector(random.nextDouble()-0.5, random.nextDouble()-0.5, random.nextDouble()-0.5).normalize().multiply(1.5);
                drawPowerfulBranch(point, point.clone().add(branch), 1);
            }

            if (point.getBlock().getType().isSolid()) break;

            for (Entity e : point.getWorld().getNearbyEntities(point, 0.8, 0.8, 0.8)) {
                if (e instanceof LivingEntity victim && !e.equals(player) && !(e instanceof ArmorStand)) {
                    firstTarget = victim;
                    break;
                }
            }
            if (firstTarget != null) break;
        }

        if (firstTarget != null) {
            processChain(player, firstTarget, hitEntities, 3, finalDmg);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.8f);
        }
    }

    private void processChain(Player caster, LivingEntity current, List<LivingEntity> hitList, int remainingChains, double damage) {
        if (remainingChains < 0 || current == null) return;

        hitList.add(current);
        applyElectricDamage(caster, current, damage);

        LivingEntity nextTarget = null;
        double nearest = 6.0;

        for (Entity e : current.getNearbyEntities(6, 6, 6)) {
            if (e instanceof LivingEntity victim && !hitList.contains(victim) && !e.equals(caster) && victim.isValid() && !(e instanceof ArmorStand)) {
                double dist = current.getLocation().distance(victim.getLocation());
                if (dist < nearest) {
                    nearest = dist;
                    nextTarget = victim;
                }
            }
        }

        if (nextTarget != null) {
            drawPowerfulBranch(current.getLocation().add(0, 1.2, 0), nextTarget.getLocation().add(0, 1.2, 0), 0);

            final LivingEntity finalNext = nextTarget;
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                    processChain(caster, finalNext, hitList, remainingChains - 1, damage), 3L);
        }
    }

    private void applyElectricDamage(Player caster, LivingEntity victim, double damage) {
        
        victim.setNoDamageTicks(0);

        
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        
        victim.setMetadata("SKILL_DAMAGE_PROCESSED", new FixedMetadataValue(Main.getInstance(), true));

        
        victim.damage(damage, caster);

        
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
        Location loc = victim.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 15, 0.4, 0.4, 0.4, 0.15);
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.6f, 1.6f);

        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isValid()) {
                    victim.removeMetadata("IS_ABILITY", Main.getInstance());
                    victim.removeMetadata("SKILL_DAMAGE_PROCESSED", Main.getInstance());
                }
            }
        }.runTaskLater(Main.getInstance(), 2L);
    }

    private void drawPowerfulBranch(Location a, Location b, int depth) {
        if (depth > 2) return;
        World world = a.getWorld();
        double distance = a.distance(b);
        Vector unit = b.toVector().subtract(a.toVector()).normalize();

        for (double d = 0; d < distance; d += 0.25) {
            Location point = a.clone().add(unit.clone().multiply(d));
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);

            if (random.nextDouble() < (0.15 - (depth * 0.05))) {
                Vector branchDir = unit.clone().add(new Vector(
                        random.nextDouble() - 0.5,
                        random.nextDouble() - 0.5,
                        random.nextDouble() - 0.5)
                ).normalize().multiply(1.5 + random.nextInt(2));
                drawPowerfulBranch(point, point.clone().add(branchDir), depth + 1);
            }

            if (random.nextDouble() < 0.4) {
                Location noise = point.clone().add((random.nextDouble()-0.5)*0.5, (random.nextDouble()-0.5)*0.5, (random.nextDouble()-0.5)*0.5);
                world.spawnParticle(Particle.ELECTRIC_SPARK, noise, 1, 0, 0, 0, 0);
            }
        }
    }
}