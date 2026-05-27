package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; 
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiaBocThienTinhSkill implements ISkill {
    private final Random random = new Random();

    @Override public String getName() { return "DiaBocThienTinh"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        
        double finalDmg = realPower * (0.70 + (level * 0.15));


        List<ArmorStand> stones = new ArrayList<>();
        Location center = player.getLocation();

        
        for (int i = 0; i < 6; i++) {
            ArmorStand as = (ArmorStand) center.getWorld().spawnEntity(center.clone().add(0, -1, 0), EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setSmall(false); 
            if (as.getEquipment() != null) {
                as.getEquipment().setHelmet(new ItemStack(Material.STONE));
            }
            stones.add(as);
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cleanup(stones);
                    this.cancel();
                    return;
                }

                if (ticks >= 60) {
                    
                    for (ArmorStand as : stones) {
                        explodeStone(player, as.getLocation().add(0, 1.45, 0), finalDmg);
                        as.remove();
                    }
                    stones.clear();
                    this.cancel();
                    return;
                }

                Location pLoc = player.getLocation().add(0, 1.2, 0);
                for (int i = 0; i < stones.size(); i++) {
                    double currentAngle = angle + (i * Math.PI * 2 / 6);
                    double x = Math.cos(currentAngle) * 3.5;
                    double z = Math.sin(currentAngle) * 3.5;
                    double y = Math.sin(ticks * 0.2 + i) * 0.8;

                    stones.get(i).teleport(pLoc.clone().add(x, y - 1.45, z));

                    if (ticks % 2 == 0) {
                        Location sLoc = stones.get(i).getLocation().add(0, 1.45, 0);
                        sLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, sLoc, 1, 0.1, 0.1, 0.1, 0.05);
                        if (ticks > 40) {
                            sLoc.getWorld().spawnParticle(Particle.FLAME, sLoc, 2, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }

                angle += (0.15 + (ticks * 0.005));
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void explodeStone(Player player, Location loc, double damage) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 3, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 40, 0.5, 0.5, 0.5, 0.2, Bukkit.createBlockData(Material.STONE));
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4.5, 4.5, 4.5)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                
                victim.setNoDamageTicks(0);

                
                victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                
                victim.damage(damage, player);

                
                Vector kb = victim.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2).setY(0.5);
                victim.setVelocity(kb);

                
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
    }

    private void cleanup(List<ArmorStand> stones) {
        for (ArmorStand as : stones) {
            if (as != null) as.remove();
        }
        stones.clear();
    }
}