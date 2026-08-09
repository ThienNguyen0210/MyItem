package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Nổ vanilla tại vị trí target. Damage tách riêng khỏi hiệu ứng hình ảnh.
 *
 * yml:
 * - type: EXPLODE
 *   target: VICTIM             # SELF | VICTIM | BLOCK — xem giải thích bên dưới
 *   power: "4.0"
 *   radius: "%player_level% / 5"
 *   amount: "%player_level% * 3"
 *   damage-type: TRUE
 *   include-self: false
 *   break-blocks: false        # true = nổ THẬT phá block xung quanh
 *
 * "target" hỗ trợ thêm giá trị BLOCK (ngoài SELF/VICTIM kế thừa từ AbstractMechanic):
 *   - SELF/VICTIM: tâm nổ = vị trí của LivingEntity tương ứng (như cũ).
 *   - BLOCK: tâm nổ = vị trí ctx.getBrokenBlock() — CHỈ có giá trị khi trigger là
 *     ON_BLOCK_BREAK (mechanic này gắn vào passive có "trigger: ON_BLOCK_BREAK").
 *     Nếu dùng BLOCK với trigger khác (không có brokenBlock), doExecute() trả false.
 *
 * Đây LÀ MECHANIC DUY NHẤT cần đọc "target" riêng thay vì dùng resolveTarget() chuẩn
 * của AbstractMechanic, vì AbstractMechanic chỉ resolve ra LivingEntity (SELF/VICTIM),
 * không biết về Block.
 */
public class ExplodeMechanic extends AbstractMechanic {

    private final String rawPower;
    private final String rawRadius;
    private final String rawAmount;
    private final String damageType;
    private final boolean includeSelf;
    private final boolean breakBlocks;
    private final String targetKeyRaw; // đọc riêng để hỗ trợ "BLOCK", không qua resolveTarget()

    public ExplodeMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawPower    = cfg.getString("power",  "4.0");
        this.rawRadius   = cfg.getString("radius", "4.0");
        this.rawAmount   = cfg.getString("amount", "0");
        this.damageType  = cfg.getString("damage-type", "TRUE").toUpperCase();
        this.includeSelf = cfg.getBoolean("include-self", false);
        this.breakBlocks = cfg.getBoolean("break-blocks", false);
        this.targetKeyRaw = cfg.getString("target", "VICTIM").toUpperCase();
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        Location origin;
        LivingEntity centerEntity = null; // dùng để loại trừ "include-self" sau này, null nếu tâm là BLOCK

        if ("BLOCK".equals(targetKeyRaw)) {
            org.bukkit.block.Block block = ctx.getBrokenBlock();
            if (block == null) return false; // không phải ON_BLOCK_BREAK, không có block -> bỏ qua
            origin = block.getLocation().add(0.5, 0.5, 0.5); // tâm block, không phải góc block
        } else {
            centerEntity = resolveTarget(ctx);
            if (centerEntity == null || !centerEntity.isValid()) return false;
            origin = centerEntity.getLocation();
        }

        World world = origin.getWorld();
        if (world == null) return false;

        float  power  = (float) ExpressionResolver.resolve(rawPower,  ctx.getActor(), 4.0);
        double radius = ExpressionResolver.resolve(rawRadius, ctx.getActor(), 4.0);
        double amount = ExpressionResolver.resolve(rawAmount, ctx.getActor(), 0);

        // breakBlocks=true -> nổ thật phá block, tự rớt item theo hành vi vanilla.
        // WorldGuard/claim plugin tự hook vào createExplosion() này, không cần check region riêng.
        world.createExplosion(origin, power, false, breakBlocks);

        if (amount <= 0 || radius <= 0) return false;

        List<LivingEntity> affected = new ArrayList<>();
        for (Entity e : world.getNearbyEntities(origin, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || le.isDead() || !le.isValid()) continue;
            if (le.equals(ctx.getActor()) && !includeSelf) continue;
            if (origin.distance(le.getLocation()) > radius) continue;
            affected.add(le);
        }

        if (affected.isEmpty()) return false;

        boolean anySuccess = false;
        for (LivingEntity target : affected) {
            if (applyExplosionDamage(ctx, target, amount)) anySuccess = true;
        }
        return anySuccess;
    }

    private boolean applyExplosionDamage(PassiveContext ctx, LivingEntity target, double amount) {
        if (target.isDead() || !target.isValid()) return false;

        if ("NORMAL".equals(damageType)) {
            Player damager = ctx.getActor();
            target.setMetadata("SKILL_DAMAGE_PROCESSED",
                    new org.bukkit.metadata.FixedMetadataValue(org.ThienNguyen.Main.getInstance(), true));
            target.setMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE,
                    new org.bukkit.metadata.FixedMetadataValue(org.ThienNguyen.Main.getInstance(), true));
            try {
                if (damager != null) target.damage(amount, damager);
                else                 target.damage(amount);
            } finally {
                target.removeMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE, org.ThienNguyen.Main.getInstance());
                target.removeMetadata("SKILL_DAMAGE_PROCESSED", org.ThienNguyen.Main.getInstance());
            }
            return true;
        }

        double newHealth = Math.max(0.0, target.getHealth() - amount);
        target.setHealth(newHealth);
        if (newHealth <= 0.0) target.damage(0.0);
        return true;
    }
}