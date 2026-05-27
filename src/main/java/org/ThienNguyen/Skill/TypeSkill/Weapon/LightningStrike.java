package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; 
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
    @Override public String getTrigger() { return "HIT"; } 

    @Override
    public void execute(Player player, LivingEntity target, int level, double baseDamageFromEvent) {
        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        
        double multiplier = 0.8 + (level * 0.07);
        double finalSkillDamage = realPower * multiplier;

        
        
        Location strikeLoc = (target != null) ? target.getLocation() : player.getLocation();

        
        strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
        strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        
        for (Entity entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3.0, 3.0, 3.0)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                
                victim.setNoDamageTicks(0);

                
                victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                
                victim.damage(finalSkillDamage, player);

                
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