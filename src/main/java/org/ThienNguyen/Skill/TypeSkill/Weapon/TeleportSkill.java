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
        // Giữ hướng nhìn ngang để tránh tele xuyên lên trời hoặc xuống đất
        Vector dir = player.getEyeLocation().getDirection().clone().setY(0).normalize();
        Location targetLoc = startLoc.clone();

        // 1. Ray-tracing tìm điểm đến an toàn
        for (int i = 1; i <= distance; i++) {
            Location nextCheck = startLoc.clone().add(dir.clone().multiply(i));

            // Kiểm tra kẹt đầu hoặc chân
            if (isSolid(nextCheck.getBlock()) || isSolid(nextCheck.clone().add(0, 1, 0).getBlock())) {
                break;
            }
            targetLoc = nextCheck;
        }

        // 2. Tìm mặt đất an toàn bên dưới (tối đa 5 block)
        boolean foundGround = false;
        for (int i = 0; i < 5; i++) {
            if (isSolid(targetLoc.getBlock().getRelative(0, -1, 0))) {
                foundGround = true;
                break;
            }
            targetLoc.add(0, -1, 0);
        }

        // Nếu không tìm thấy đất (vực thẳm), giữ nguyên vị trí cũ để an toàn
        if (!foundGround) {
            targetLoc = startLoc.clone();
        }

        // Giữ nguyên góc nhìn sau khi dịch chuyển
        targetLoc.setYaw(player.getLocation().getYaw());
        targetLoc.setPitch(player.getLocation().getPitch());

        // 3. Hiệu ứng tại điểm đi
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 20, 0.2, 0.2, 0.2, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

        // 4. Thực thi dịch chuyển
        player.teleport(targetLoc);

        // 5. Hiệu ứng tại điểm đến & Gây sát thương xung kích
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 20, 0.2, 0.2, 0.2, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);

        // Gây 40% sát thương gốc cho kẻ địch đứng cực gần điểm đến (bán kính 2 block)
        double shockDamage = baseDamage * 0.4;
        player.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

        for (Entity entity : player.getNearbyEntities(2.0, 2.0, 2.0)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player)) {
                victim.setMetadata("SKILL_DAMAGE_PROCESSED", new FixedMetadataValue(Main.getInstance(), true));
                victim.damage(shockDamage, player);
                victim.removeMetadata("SKILL_DAMAGE_PROCESSED", Main.getInstance());

                // Hiệu ứng hạt hư không trúng mục tiêu
                victim.getWorld().spawnParticle(Particle.CRIT_MAGIC, victim.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // Dọn dẹp metadata sau 1 tick
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