package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; 
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

        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        
        double multiplier = 0.7 + (level * 0.08);
        double finalSkillDamage = realPower * multiplier;

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        
        Fireball fireball = player.launchProjectile(Fireball.class, direction.multiply(1.8));
        fireball.setShooter(player);

        
        fireball.setYield(0.0F);
        fireball.setIsIncendiary(false);

        
        fireball.setMetadata("FB_SKILL_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalSkillDamage));

        
        fireball.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

        
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.9f);
    }
}