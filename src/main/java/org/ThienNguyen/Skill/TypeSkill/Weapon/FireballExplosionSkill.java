package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; // Cầu nối lấy stats xịn
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class FireballExplosionSkill implements ISkill {
    @Override public String getName() { return "FireballExplosion"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "LEFT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {

        // --- BƯỚC 1: LẤY SÁT THƯƠNG THỰC TỪ CACHE ---
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        // --- BƯỚC 2: TÍNH TOÁN SÁT THƯƠNG CUỐI CÙNG ---
        double multiplier = 0.7 + (level * 0.08);
        double finalSkillDamage = realPower * multiplier;

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        // --- BƯỚC 3: PHÓNG HỎA CẦU ---
        Fireball fireball = player.launchProjectile(Fireball.class, direction.multiply(1.8));
        fireball.setShooter(player);

        // Vô hiệu hóa phá hủy địa hình
        fireball.setYield(0.0F);
        fireball.setIsIncendiary(false);

        // Đánh dấu sát thương khủng vào metadata để Listener lấy ra dùng
        fireball.setMetadata("FB_SKILL_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalSkillDamage));

        // Đánh dấu IS_ABILITY để Listener biết đây là skill
        fireball.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

        // 4. Hiệu ứng âm thanh & Thông báo
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.9f);
    }
}