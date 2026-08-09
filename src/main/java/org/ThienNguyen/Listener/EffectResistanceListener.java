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


public class EffectResistanceListener implements Listener {

    
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

    
    private static final String META_KEY = "EFFECT_RESISTANCE_PROCESSED";

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) return; 

        if (!BAD_EFFECTS.contains(newEffect.getType())) return;

        
        if (player.hasMetadata(META_KEY)) return;

        double resistance = PlayerCombatCache.getEffective(
                player.getUniqueId(), "effect_resistance",
                PlayerCombatCache.getStats(player.getUniqueId()).totalEffectResistance);

        if (resistance <= 0) return; 

        double multiplier = Math.max(0.0, 1.0 - (resistance / 100.0));
        int newDuration = (int) Math.round(newEffect.getDuration() * multiplier);

        if (newDuration == newEffect.getDuration()) return; 

        event.setCancelled(true); 

        if (newDuration <= 0) return; 

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