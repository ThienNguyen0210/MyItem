package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TeleportSkill implements ISkill {
    @Override public String getName() { return "Teleport"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamage) {
        int distance = 7 + level;
        Location startLoc = player.getLocation();
        
        Vector dir = player.getEyeLocation().getDirection().clone().setY(0).normalize();
        Location targetLoc = startLoc.clone();

        
        for (int i = 1; i <= distance; i++) {
            Location nextCheck = startLoc.clone().add(dir.clone().multiply(i));

            
            if (isSolid(nextCheck.getBlock()) || isSolid(nextCheck.clone().add(0, 1, 0).getBlock())) {
                break;
            }
            targetLoc = nextCheck;
        }

        
        boolean foundGround = false;
        for (int i = 0; i < 5; i++) {
            if (isSolid(targetLoc.getBlock().getRelative(0, -1, 0))) {
                foundGround = true;
                break;
            }
            targetLoc.add(0, -1, 0);
        }

        
        if (!foundGround) {
            targetLoc = startLoc.clone();
        }

        
        targetLoc.setYaw(player.getLocation().getYaw());
        targetLoc.setPitch(player.getLocation().getPitch());

        
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 20, 0.2, 0.2, 0.2, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

        
        player.teleport(targetLoc);

        
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 20, 0.2, 0.2, 0.2, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);

        
        double shockDamage = baseDamage * 0.4;
        player.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

        for (Entity entity : player.getNearbyEntities(2.0, 2.0, 2.0)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player)) {
                victim.setMetadata("SKILL_DAMAGE_PROCESSED", new FixedMetadataValue(Main.getInstance(), true));
                victim.damage(shockDamage, player);
                victim.removeMetadata("SKILL_DAMAGE_PROCESSED", Main.getInstance());

                
                victim.getWorld().spawnParticle(Particle.CRIT_MAGIC, victim.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.05);
            }
        }

        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.removeMetadata("IS_ABILITY", Main.getInstance());
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }

    private boolean isSolid(Block block) {
        return block.getType().isSolid() && block.getType() != Material.AIR;
    }
}