package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class Lightning implements IAbility {

    private static final String METADATA_KEY = "ABILITY_EXTRA_DAMAGE";

    @Override
    public String getName() {
        return "LIGHTNING";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        // 1. Hiệu ứng sấm sét (chỉ visual)
        target.getWorld().strikeLightningEffect(target.getLocation());

        // Âm thanh cho ấn tượng hơn (tùy chọn)
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.0f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);

        // 2. Tính sát thương bổ sung: 5% + (level-1)*3% của baseDamage (đã buff từ stat)
        double percent = 5.0 + (Math.max(0, level - 1) * 3.0);
        double extraDamage = baseDamage * (percent / 100.0);

        // 3. Cộng dồn vào metadata (sẽ được xử lý trong listener)
        double currentExtra = 0.0;
        if (target.hasMetadata(METADATA_KEY)) {
            currentExtra = target.getMetadata(METADATA_KEY).get(0).asDouble();
        }
        target.setMetadata(METADATA_KEY, new FixedMetadataValue(Main.getInstance(), currentExtra + extraDamage));
    }
}