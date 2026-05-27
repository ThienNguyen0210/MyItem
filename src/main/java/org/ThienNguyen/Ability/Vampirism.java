package org.ThienNguyen.Ability;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Vampirism implements IAbility {

    @Override
    public String getName() {
        return "VAMPIRISM";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());

        
        double finalDamage = baseDamage;
        applySafeDamage(target, attacker, finalDamage, plugin);

        
        double healPercent = 0.2 + ((level - 1) * 0.1);
        double healAmount = finalDamage * healPercent;

        
        double maxHealth = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = attacker.getHealth();

        
        attacker.setHealth(Math.min(maxHealth, currentHealth + healAmount));

        
        
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);

        
        attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0.1);
        attacker.getWorld().spawnParticle(Particle.REDSTONE, attacker.getLocation().add(0, 1, 0), 10, 0.2, 0.5, 0.2, 0.05);

        
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 0.5f);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.5f);
    }

    private void applySafeDamage(LivingEntity victim, Player attacker, double damage, Plugin plugin) {
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(plugin, true));
        victim.damage(damage, attacker);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isValid()) {
                    victim.removeMetadata("IS_ABILITY", plugin);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}