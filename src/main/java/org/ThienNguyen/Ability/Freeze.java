package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Freeze implements IAbility {

    @Override
    public String getName() {
        return "FREEZE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead() || target.hasMetadata("IS_ABILITY_FREEZE")) return;

        final int durationTicks = 40 + (level * 10);
        final Location center = target.getLocation().getBlock().getLocation();
        final Location standLoc = center.clone().add(0.5, 0.0, 0.5);
        standLoc.setDirection(target.getLocation().getDirection());

        // Đánh dấu mục tiêu
        target.setMetadata("IS_ABILITY_FREEZE", new FixedMetadataValue(Main.getInstance(), true));

        // KHÓA AI: Quái sẽ đứng bất động hoàn toàn nhưng vẫn chịu Gravity
        if (target instanceof Mob mob) {
            mob.setAI(false);
        }

        // Teleport vào giữa block ngay lập tức 1 lần duy nhất
        target.teleport(standLoc);

        List<Block> iceBlocks = new ArrayList<>();
        List<Material> oldMaterials = new ArrayList<>();

        Block bottom = center.getBlock();
        Block top = center.clone().add(0, 1, 0).getBlock();

        saveAndSetIce(bottom, iceBlocks, oldMaterials);
        saveAndSetIce(top, iceBlocks, oldMaterials);

        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, standLoc.clone().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.05);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || ticks >= durationTicks) {
                    cleanup();
                    this.cancel();
                    return;
                }

                // Giữ vị trí XZ nhưng cho phép Y thay đổi (rơi tự do)
                // Nếu quái bị đẩy, ta kéo nó lại tâm XZ của khối băng
                Location current = target.getLocation();
                if (current.getX() != standLoc.getX() || current.getZ() != standLoc.getZ()) {
                    Location loc = standLoc.clone();
                    loc.setY(current.getY()); // Giữ nguyên độ cao hiện tại để nó rơi tự nhiên
                    target.teleport(loc);
                }

                // Bảo vệ khối băng
                for (Block b : iceBlocks) {
                    if (b.getType() != Material.PACKED_ICE) b.setType(Material.PACKED_ICE);
                }

                if (ticks % 10 == 0) {
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE, standLoc.clone().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);
                }

                ticks++;
            }

            private void cleanup() {
                // Trả lại AI
                if (target instanceof Mob mob) {
                    mob.setAI(true);
                }

                if (target.isValid()) {
                    target.removeMetadata("IS_ABILITY_FREEZE", Main.getInstance());
                }

                // Khôi phục địa hình
                for (int i = 0; i < iceBlocks.size(); i++) {
                    iceBlocks.get(i).setType(oldMaterials.get(i));
                }

                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void saveAndSetIce(Block b, List<Block> blocks, List<Material> materials) {
        materials.add(b.getType());
        blocks.add(b);
        b.setType(Material.PACKED_ICE);
    }
}