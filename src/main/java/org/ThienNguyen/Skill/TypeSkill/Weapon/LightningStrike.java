package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; // Cầu nối stats
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class LightningStrike implements ISkill {
    @Override public String getName() { return "LightningStrike"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "HIT"; } // Khi đánh trúng quái

    @Override
    public void execute(Player player, LivingEntity target, int level, double baseDamageFromEvent) {
        // --- BƯỚC 1: LẤY SÁT THƯƠNG THỰC TỪ CACHE ---
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        // --- BƯỚC 2: TÍNH TOÁN FINAL DAMAGE (0.8 + 0.07 mỗi cấp) ---
        double multiplier = 0.8 + (level * 0.07);
        double finalSkillDamage = realPower * multiplier;

        // --- BƯỚC 3: XÁC ĐỊNH VỊ TRÍ SÉT ĐÁNH ---
        // Vì trigger là HIT, ta lấy vị trí của target (kẻ bị đánh) thay vì cách 6 block
        Location strikeLoc = (target != null) ? target.getLocation() : player.getLocation();

        // Hiệu ứng sấm sét và nổ hình ảnh
        strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
        strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        // --- BƯỚC 4: GÂY SÁT THƯƠNG AOE BÁN KÍNH 3 BLOCK ---
        for (Entity entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3.0, 3.0, 3.0)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                // 1. Phá bất tử (Để sét giật xuyên qua các đòn đánh thường)
                victim.setNoDamageTicks(0);

                // 2. Set Metadata lên VICTIM để EventDamage bỏ qua
                victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                // 3. Gây damage thực thi
                victim.damage(finalSkillDamage, player);

                // 4. Dọn dẹp metadata sau 2 ticks
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
}