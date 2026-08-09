package org.ThienNguyen.Listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

/**
 * Áp dụng stat "effect_resistance" (% giảm duration của hiệu ứng XẤU/debuff) lên player.
 *
 * Bắt EntityPotionEffectEvent — chuẩn nhất vì bắt được mọi nguồn hiệu ứng (potion uống,
 * mob gây (vd Witch), command /effect, plugin khác cộng effect...), không chỉ riêng
 * hệ thống Deep Wound của plugin này.
 *
 * Cách áp dụng: khi effect MỚI (action ADDED) hoặc effect CHỒNG (CHANGED) là debuff,
 * tính lại duration mới = duration gốc * (1 - resistance/100), rồi set lại effect với
 * duration mới đó. Không cap — resistance 100% nghĩa là duration = 0 (miễn nhiễm hoàn toàn,
 * effect coi như không bao giờ áp lên được, vì set lại với duration 0 không hiển thị).
 *
 * GIỚI HẠN: chỉ áp dụng cho Player (vì effect_resistance đọc từ PlayerCombatCache, chỉ tồn
 * tại cho Player có equipment). Mob/Player khác gây effect lên Player vẫn được bắt đúng vì
 * event này theo TARGET nhận effect, không theo nguồn gây ra.
 */
public class EffectResistanceListener implements Listener {

    /**
     * Danh sách cố định các hiệu ứng coi là "xấu" (debuff) — áp dụng kháng.
     * MỞ RỘNG: thêm vào set này khi cần, không cần sửa logic onPotionEffect().
     */
    private static final Set<PotionEffectType> BAD_EFFECTS = Set.of(
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.NAUSEA,
            PotionEffectType.HUNGER,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.UNLUCK,
            PotionEffectType.BAD_OMEN,
            PotionEffectType.DARKNESS,
            PotionEffectType.LEVITATION
    );

    /** Đánh dấu effect đang được chính listener này set lại, tránh tự bắt lại event do chính mình tạo ra. */
    private static final String META_KEY = "EFFECT_RESISTANCE_PROCESSED";

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) return; // effect bị REMOVE, không phải thêm/đổi — bỏ qua

        if (!BAD_EFFECTS.contains(newEffect.getType())) return;

        // Tránh xử lý lại effect do chính listener này vừa set (recursive loop).
        if (player.hasMetadata(META_KEY)) return;

        double resistance = PlayerCombatCache.getEffective(
                player.getUniqueId(), "effect_resistance",
                PlayerCombatCache.getStats(player.getUniqueId()).totalEffectResistance);

        if (resistance <= 0) return; // không có kháng, giữ nguyên effect gốc

        double multiplier = Math.max(0.0, 1.0 - (resistance / 100.0));
        int newDuration = (int) Math.round(newEffect.getDuration() * multiplier);

        if (newDuration == newEffect.getDuration()) return; // không đổi gì, khỏi set lại

        event.setCancelled(true); // chặn effect gốc (duration chưa giảm)

        if (newDuration <= 0) return; // kháng 100% (hoặc gần đó) -> không áp effect luôn

        PotionEffect reducedEffect = new PotionEffect(
                newEffect.getType(),
                newDuration,
                newEffect.getAmplifier(),
                newEffect.isAmbient(),
                newEffect.hasParticles(),
                newEffect.hasIcon()
        );

        player.setMetadata(META_KEY, new org.bukkit.metadata.FixedMetadataValue(
                org.ThienNguyen.Main.getInstance(), true));
        player.addPotionEffect(reducedEffect);
        player.removeMetadata(META_KEY, org.ThienNguyen.Main.getInstance());
    }
}