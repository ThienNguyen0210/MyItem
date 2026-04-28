package org.ThienNguyen.Listener;

import net.objecthunter.exp4j.ExpressionBuilder;
import org.ThienNguyen.Ability.AbilityManager;
import org.ThienNguyen.Ability.IAbility;
import org.ThienNguyen.Hook.MMOCORE;
import org.bukkit.entity.Projectile;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EventDamage implements Listener {

    private final Random random = new Random();
    private final String METADATA_CURSE = "CURSED_REDUCTION";
    private static final String METADATA_EXTRA_DAMAGE = "ABILITY_EXTRA_DAMAGE";
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // ── BƯỚC 0: NHẬN DIỆN SÁT THƯƠNG TỪ SCRIPT ──
        double scriptDamage = 0;
        boolean isFromScript = false;
        if (target.hasMetadata("SKILL_DAMAGE_VALUE")) {
            scriptDamage = target.getMetadata("SKILL_DAMAGE_VALUE").get(0).asDouble();
            isFromScript = true;
            target.removeMetadata("SKILL_DAMAGE_VALUE", Main.getInstance());
        }

        if (!isFromScript) {
            if (target.hasMetadata("IS_ABILITY") || target.hasMetadata("IS_SKILL_PROCESS")) {
                return;
            }
        }

        boolean isFromAbility = event.getDamager().hasMetadata("IS_ABILITY");
        boolean isFromThorns = event.getDamager().hasMetadata("THORNS_REFLECT");
        if (isFromThorns) return;

        boolean isSkillDamage = target.hasMetadata("SKILL_DAMAGE_PROCESSED");

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player p) {
                attacker = p;
            }
        }

        // Tước vũ khí & kiểm tra level vũ khí
        if (!isFromScript && event.getDamager().hasMetadata("DISARMED_STATUS") && !isFromAbility) {
            event.setCancelled(true);
            if (attacker != null) attacker.sendActionBar("§c§l✖ Bạn đang bị tước vũ khí!");
            return;
        }

        if (!isFromScript && attacker != null && !isSkillDamage && !isFromAbility) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (weapon != null && !weapon.getType().isAir()) {
                if (!MMOCORE.canUse(attacker, weapon)) {
                    event.setCancelled(true);
                    attacker.sendMessage("§cNo Level !:)");
                    return;
                }
            }
        }

        // ── XÁC ĐỊNH DAMAGE GỐC ──
        double currentDamage = isFromScript ? scriptDamage : event.getDamage();

        // ── BƯỚC 1: ĐỌC METADATA TỪ TIỀM NĂNG (TiemNang.java) ──
        double tnDodge = 0.0;
        double tnCritChance = 0.0;
        double tnCritDamage = 0.0;
        double tnFinalDamage = 0.0;
        double tnElementDamage = 0.0;

        if (target.hasMetadata("STAT_DODGE_CHANCE")) {
            tnDodge = target.getMetadata("STAT_DODGE_CHANCE").get(0).asDouble();
        }
        if (target.hasMetadata("STAT_CRIT_CHANCE")) {
            tnCritChance = target.getMetadata("STAT_CRIT_CHANCE").get(0).asDouble();
        }
        if (target.hasMetadata("STAT_CRIT_DAMAGE_PERCENT")) {
            tnCritDamage = target.getMetadata("STAT_CRIT_DAMAGE_PERCENT").get(0).asDouble();
        }
        if (target.hasMetadata("VALUE_FINAL_DAMAGE")) {
            tnFinalDamage = target.getMetadata("VALUE_FINAL_DAMAGE").get(0).asDouble();
        }
        if (target.hasMetadata("VALUE_ELEMENTAL_DAMAGE")) {
            tnElementDamage = target.getMetadata("VALUE_ELEMENTAL_DAMAGE").get(0).asDouble();
        }

        // ── BƯỚC 2: LỜI NGUYỀN (curse) ──
        double curseMultiplier = 1.0;
        if (event.getDamager().hasMetadata(METADATA_CURSE)) {
            double reductionPercent = event.getDamager().getMetadata(METADATA_CURSE).get(0).asDouble();
            curseMultiplier = Math.max(0.1, 1.0 - (Math.min(90.0, reductionPercent) / 100.0));
        }

        double damageBeforeReduction = currentDamage;

        PlayerCombatCache.CombatStats attackerStats = null;
        PlayerCombatCache.CombatStats victimStats = null;
        double weaponElementTotalDmg = 0.0;
        StringBuilder elementDisplayBuilder = new StringBuilder();

        // Reset metadata hiển thị
        target.removeMetadata("DISPLAY_SPECIAL_STATUS", Main.getInstance());
        target.removeMetadata("LAST_HIT_CRIT", Main.getInstance());
        target.removeMetadata("DISPLAY_ELEMENTS_DATA", Main.getInstance());
        target.removeMetadata("DISPLAY_NORMAL_DAMAGE", Main.getInstance());
        target.removeMetadata("DISPLAY_TRUE_DAMAGE", Main.getInstance());

        // ── BƯỚC 3: TÍNH TOÁN ATTACKER ──
        if (attacker != null && !isSkillDamage) {
            attackerStats = PlayerCombatCache.getStats(attacker.getUniqueId());

            if (event.getDamager() instanceof Projectile) {
                currentDamage += attackerStats.totalBowDamage;
            }

            currentDamage += (attackerStats.totalBonusDmg * curseMultiplier);

            double pvpPveMultiplier = (target instanceof Player) ? attackerStats.totalPvpBonus : attackerStats.totalPveBonus;
            currentDamage *= (1 + pvpPveMultiplier / 100.0);
            currentDamage *= (1 + attackerStats.totalAllDamage / 100.0);

            // Element Damage
            if (attackerStats.weaponElementLevels != null && !attackerStats.weaponElementLevels.isEmpty()) {
                for (Map.Entry<String, Integer> entry : attackerStats.weaponElementLevels.entrySet()) {
                    String eId = entry.getKey();
                    int attackLevel = entry.getValue();

                    int defenseLevel = 0;
                    if (victimStats != null && victimStats.elementDefenses != null) {
                        defenseLevel = victimStats.elementDefenses.getOrDefault(eId, 0);
                    }

                    if (defenseLevel >= attackLevel) continue;

                    int effectiveLevel = attackLevel - defenseLevel;
                    double baseDmg = Main.getInstance().getElementConfig().getDouble(eId + ".base-damage", 2.0);
                    double perDmg = Main.getInstance().getElementConfig().getDouble(eId + ".damage-per", 5.0);

                    double eDmg = (baseDmg + (effectiveLevel * perDmg)) * curseMultiplier;
                    weaponElementTotalDmg += eDmg;

                    if (elementDisplayBuilder.length() > 0) elementDisplayBuilder.append(",");
                    elementDisplayBuilder.append(eId).append(":").append(effectiveLevel).append(":").append(String.format("%.1f", eDmg));

                    org.ThienNguyen.Element.ElementCore.playEffect(target, eId);
                }
            }

            // ── CRITICAL ── (Kết hợp Tiềm Năng + Cache)
            double finalCritChance = attackerStats.totalCritChance + tnCritChance;
            double finalCritDamage = attackerStats.totalCritDamage + tnCritDamage;

            if (random.nextDouble() * 100 <= finalCritChance) {
                double baseCritMult = Main.getInstance().getCustomConfig().getDouble("crit-multiplier", 1.5);
                currentDamage *= (baseCritMult + (finalCritDamage / 100.0));
                target.setMetadata("LAST_HIT_CRIT", new FixedMetadataValue(Main.getInstance(), true));
            }

            damageBeforeReduction = currentDamage;

            if (!isFromScript && !isFromAbility) {
                double magicDmgForAbility = (attackerStats != null) ? (attackerStats.totalMagicDamage * curseMultiplier) : 0.0;
                double totalPowerForAbility = currentDamage + magicDmgForAbility;

                handleCachedAbilities(attacker, target, attackerStats.bestAbilities, totalPowerForAbility, "attack");
                handleCachedAbilities(attacker, target, attackerStats.bestAbilities, totalPowerForAbility, "attack_self");
            }
        } else {
            damageBeforeReduction = currentDamage;
        }

        // ── BƯỚC 4: TÍNH TOÁN VICTIM (DEFENSE) ──
        if (target instanceof Player victim) {
            victimStats = PlayerCombatCache.getStats(victim.getUniqueId());

            // ── XỬ LÝ DODGE VỚI ACCURACY ──
            double rawDodge = (victimStats != null ? victimStats.totalDodge : 0) + tnDodge;
            double attackerAccuracy = (attackerStats != null ? attackerStats.totalAccuracy : 0.0);
            double finalDodgeChance = Math.max(0, rawDodge - attackerAccuracy);

            if (random.nextDouble() * 100 <= finalDodgeChance) {
                target.setMetadata("DISPLAY_SPECIAL_STATUS", new FixedMetadataValue(Main.getInstance(), "DODGE"));
                event.setCancelled(true);
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                return; // Ngắt hoàn toàn
            }

            // ── XỬ LÝ BLOCK (ĐỠ ĐÒN) ──
            double blockChance = (victimStats != null ? victimStats.totalBlock : 0);
            if (random.nextDouble() * 100 <= blockChance) {
                target.setMetadata("DISPLAY_SPECIAL_STATUS", new FixedMetadataValue(Main.getInstance(), "BLOCK"));
                event.setCancelled(true);
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                return; // Ngắt hoàn toàn, không chạy xuống phần tính Armor/Damage phía dưới
            }

            // Các chỉ số phòng thủ (Def, Armor) nếu KHÔNG bị Dodge/Block
            double defMultiplier = (attacker != null) ? victimStats.totalPvpDef : victimStats.totalPveDef;
            currentDamage *= Math.max(0, 1 - defMultiplier / 100.0);
            currentDamage *= Math.max(0, 1 - victimStats.totalAllDefense / 100.0);

            double finalArmor = victimStats.totalArmor;
            if (attackerStats != null) {
                double armorAfterFlatPen = Math.max(0, finalArmor - attackerStats.totalArmorPen);
                finalArmor = armorAfterFlatPen * Math.max(0, 1 - attackerStats.totalPenetration / 100.0);
            }
            currentDamage = applyArmorFormula(currentDamage, finalArmor);

            // Xử lý Ability khi bị đánh (Defense/Defense_Self)
            if (!isFromScript && event.getDamager() instanceof LivingEntity attackerEntity && !isSkillDamage && !isFromAbility) {
                handleCachedAbilities(victim, attackerEntity, victimStats.bestAbilities, currentDamage, "defense");
                handleCachedAbilities(victim, attackerEntity, victimStats.bestAbilities, currentDamage, "defense_self");
            }
        }

        // ── BƯỚC 5: TỔNG HỢP ──
        double extraFromAbilities = target.hasMetadata(METADATA_EXTRA_DAMAGE)
                ? target.getMetadata(METADATA_EXTRA_DAMAGE).get(0).asDouble() : 0.0;

        double trueDmg = (attackerStats != null) ? attackerStats.totalTrueDamage : 0.0;

        double finalMagicDmg = 0.0;
        if (attackerStats != null && attackerStats.totalMagicDamage > 0) {
            if (isFromAbility || isFromScript || isSkillDamage) {
                double rawMagic = attackerStats.totalMagicDamage * curseMultiplier;
                if (target instanceof Player && victimStats != null) {
                    finalMagicDmg = applyMagicDefenseFormula(rawMagic, victimStats.totalMagicDefense);
                } else {
                    finalMagicDmg = rawMagic;
                }
            }
        }

        double finalDeathDmg = 0.0;
        if (attackerStats != null && attackerStats.totalDeathDamage > 0) {
            double threshold = Main.getInstance().getCustomConfig().getDouble("death-damage-threshold", 50.0);
            double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double currentHealthPercent = (target.getHealth() / maxHealth) * 100.0;

            if (currentHealthPercent <= threshold) {
                finalDeathDmg = attackerStats.totalDeathDamage * curseMultiplier;
                target.setMetadata("IS_DEATH_STRIKE", new FixedMetadataValue(Main.getInstance(), true));
            }
        }

        double finalElementDamage = weaponElementTotalDmg
                + (attackerStats != null ? attackerStats.totalElementDamage * curseMultiplier : 0.0)
                + tnElementDamage;

        double finalPhysicalDmg = Math.max(0, currentDamage) + extraFromAbilities;

        // ── KẾT HỢP DAMAGE TỪ TIỀM NĂNG ──
        double theoreticalTotal = finalPhysicalDmg + finalElementDamage + trueDmg + finalDeathDmg + finalMagicDmg;
        if (tnFinalDamage > 0) {
            theoreticalTotal = Math.max(theoreticalTotal, tnFinalDamage); // Ưu tiên damage từ Tiềm Năng nếu lớn hơn
        }

        // Lưu metadata cho TextDisplayManager và PlaceholderAPI
        target.setMetadata("VALUE_FINAL_DAMAGE", new FixedMetadataValue(Main.getInstance(), theoreticalTotal));
        target.setMetadata("VALUE_ELEMENTAL_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalElementDamage));
        target.setMetadata("VALUE_MAGIC_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalMagicDmg));
        target.setMetadata("VALUE_TRUE_DAMAGE", new FixedMetadataValue(Main.getInstance(), trueDmg));

        if (attackerStats != null) {
            target.setMetadata("STAT_ATTACKER_CRIT_CHANCE", new FixedMetadataValue(Main.getInstance(), attackerStats.totalCritChance + tnCritChance));
            target.setMetadata("STAT_ATTACKER_CRIT_DMG", new FixedMetadataValue(Main.getInstance(), attackerStats.totalCritDamage + tnCritDamage));
        }
        if (victimStats != null) {
            target.setMetadata("STAT_VICTIM_DODGE", new FixedMetadataValue(Main.getInstance(), victimStats.totalDodge + tnDodge));
        }

        target.setMetadata("DISPLAY_MAGIC_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalMagicDmg));

        if (elementDisplayBuilder.length() > 0) {
            target.setMetadata("DISPLAY_ELEMENTS_DATA", new FixedMetadataValue(Main.getInstance(), elementDisplayBuilder.toString()));
        }

        event.setDamage(theoreticalTotal);

        // ── THORNS + LIFESTEAL ──
        if (!isFromScript && !isSkillDamage && !isFromAbility && damageBeforeReduction > 0) {
            if (victimStats != null && victimStats.totalThorns > 0 && event.getDamager() instanceof LivingEntity attackerEntity) {
                double reflected = damageBeforeReduction * (victimStats.totalThorns / 100.0);
                if (reflected > 0) {
                    attackerEntity.setMetadata("THORNS_REFLECT", new FixedMetadataValue(Main.getInstance(), true));
                    attackerEntity.damage(reflected, target);
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                            () -> attackerEntity.removeMetadata("THORNS_REFLECT", Main.getInstance()), 1L);
                }
            }

            if (attackerStats != null && attackerStats.totalLifesteal > 0 && attacker != null && theoreticalTotal > 0) {
                applyLifesteal(attacker, theoreticalTotal, attackerStats.totalLifesteal);
            }
        }

        // ── PHẦN HIỂN THỊ CUỐI ──
        double displayPhysicalFinal = finalPhysicalDmg;
        if (finalDeathDmg > 0) {
            displayPhysicalFinal += finalDeathDmg;
            target.setMetadata("IS_DEATH_STRIKE_HIT", new FixedMetadataValue(Main.getInstance(), true));
        }

        final double fPhysicalLyThuyet = displayPhysicalFinal;
        final double fTrueDmg = trueDmg;
        final double fElemDmg = finalElementDamage;
        final double fMagicDmgFinal = finalMagicDmg;
        final boolean isCritFinal = target.hasMetadata("LAST_HIT_CRIT");
        final boolean isDeathStrike = target.hasMetadata("IS_DEATH_STRIKE_HIT");

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (target == null) return;

            target.removeMetadata("SKILL_DAMAGE_VALUE", Main.getInstance());
            target.removeMetadata(METADATA_EXTRA_DAMAGE, Main.getInstance());

            target.setMetadata("DISPLAY_NORMAL_DAMAGE", new FixedMetadataValue(Main.getInstance(), fPhysicalLyThuyet));
            target.setMetadata("DISPLAY_TRUE_DAMAGE", new FixedMetadataValue(Main.getInstance(), fTrueDmg));
            target.setMetadata("DISPLAY_MAGIC_DAMAGE", new FixedMetadataValue(Main.getInstance(), fMagicDmgFinal));

            if (isCritFinal) target.setMetadata("LAST_HIT_CRIT", new FixedMetadataValue(Main.getInstance(), true));
            if (isDeathStrike) target.setMetadata("IS_DEATH_STRIKE_HIT", new FixedMetadataValue(Main.getInstance(), true));

            org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
        });
    }

    /**
     * HÀM QUAN TRỌNG: Cung cấp dữ liệu cho PlaceholderAPI (Sửa lỗi BUILD FAILURE)
     */
    public static Map<String, Double> calculateFullStaticStats(Player player) {
        Map<String, Double> stats = new HashMap<>();
        PlayerCombatCache.CombatStats cached = PlayerCombatCache.getStats(player.getUniqueId());

        double baseTotal = cached.totalBonusDmg + cached.totalTrueDamage + cached.totalElementDamage;

        stats.put("base", baseTotal);
        stats.put("chance", cached.totalCritChance);
        stats.put("damage", cached.totalCritDamage);
        stats.put("element", cached.totalElementDamage);
        stats.put("armor", cached.totalArmor);
        stats.put("dodge", cached.totalDodge);
        stats.put("block", cached.totalBlock);
        stats.put("thorns", cached.totalThorns);
        stats.put("lifesteal", cached.totalLifesteal);

        return stats;
    }

    private void handleCachedAbilities(Player player, LivingEntity opponent, Map<String, double[]> abilities, double damage, String triggerType) {
        if (abilities == null || abilities.isEmpty()) return;
        for (Map.Entry<String, double[]> entry : abilities.entrySet()) {
            String abilityName = entry.getKey();
            double[] data = entry.getValue();
            String configTrigger = Main.getInstance().getAbilityTargetConfig().getString("Abilities." + abilityName, "attack");
            if (!configTrigger.equalsIgnoreCase(triggerType)) continue;

            if (random.nextDouble() * 100 <= data[1]) {
                IAbility ability = AbilityManager.getAbility(abilityName);
                if (ability != null) {
                    LivingEntity finalTarget = configTrigger.toLowerCase().endsWith("_self") ? player : opponent;
                    ability.execute(player, finalTarget, (int) data[0], damage);
                }
            }
        }
    }

    private void applyLifesteal(Player player, double damage, double percent) {
        double heal = damage * (percent / 100.0);
        double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHp, player.getHealth() + heal));
    }

    private double applyArmorFormula(double damage, double armor) {
        String formula = Main.getInstance().getCustomConfig().getString("armor-formula", "damage * (100 / (100 + armor))");
        if (formula == null || formula.isEmpty() || formula.equals("damage * (100 / (100 + armor))")) {
            return armor <= 0 ? damage : damage * (100.0 / (100.0 + armor));
        }
        try {
            return new ExpressionBuilder(formula).variables("damage", "armor").build()
                    .setVariable("damage", damage).setVariable("armor", armor).evaluate();
        } catch (Exception ex) {
            return armor <= 0 ? damage : damage * (100.0 / (100.0 + armor));
        }
    }
    private double applyMagicDefenseFormula(double damage, double mDef) {
        // Đọc công thức từ config, mặc định là giảm theo phần trăm: damage * (100 / (100 + mDef))
        String formula = Main.getInstance().getCustomConfig().getString("magic-defense-formula", "damage * (100 / (100 + mDef))");

        if (mDef <= 0) return damage;

        try {
            return new ExpressionBuilder(formula)
                    .variables("damage", "mDef")
                    .build()
                    .setVariable("damage", damage)
                    .setVariable("mDef", mDef)
                    .evaluate();
        } catch (Exception ex) {
            // Nếu lỗi công thức, dùng công thức mặc định
            return damage * (100.0 / (100.0 + mDef));
        }
    }
}