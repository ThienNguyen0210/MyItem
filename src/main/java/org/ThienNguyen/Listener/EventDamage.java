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
    private String cachedArmorFormula = null;
    private String cachedMagicFormula = null;
    private net.objecthunter.exp4j.Expression cachedArmorExpression = null;
    private net.objecthunter.exp4j.Expression cachedMagicExpression = null;
    private static Map<String, String> abilityTriggerCache = null;
    private final Random random = new Random();
    private final String METADATA_CURSE = "CURSED_REDUCTION";
    private static final String METADATA_EXTRA_DAMAGE = "ABILITY_EXTRA_DAMAGE";
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (event.getDamager().hasMetadata("THORNS_REFLECT")) {
            return;
        }
        if (event.getEntity().hasMetadata("passive_damage_skip")) {
            return;
        }
        double scriptDamage = 0;
        boolean isFromScript = false;
        boolean isFromAbility = false;

        if (target.hasMetadata("SKILL_DAMAGE_VALUE")) {
            scriptDamage = target.getMetadata("SKILL_DAMAGE_VALUE").get(0).asDouble();
            isFromScript = true;
            target.removeMetadata("SKILL_DAMAGE_VALUE", Main.getInstance());
        }

        if (target.hasMetadata("IS_ABILITY") || target.hasMetadata("IS_SKILL_PROCESS")) {
            isFromAbility = true;
        }

        // ========== XỬ LÝ HIỂN THỊ CHO SKILL/ABILITY ==========
        if (isFromAbility) {
            double damageToDisplay = isFromScript ? scriptDamage : event.getDamage();

            // Set damage vào metadata Normal Damage
            target.setMetadata("DISPLAY_NORMAL_DAMAGE", new FixedMetadataValue(Main.getInstance(), damageToDisplay));

            // Gọi hiển thị damage
            org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
        }

        // Return sớm cho Ability (sau khi đã hiển thị)
        if (!isFromScript && isFromAbility) {
            return;
        }

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

        if (!isFromScript && event.getDamager().hasMetadata("DISARMED_STATUS") && !isFromAbility) {
            event.setCancelled(true);
            if (attacker != null) attacker.sendActionBar("§c§l✖ Bạn đang bị tước vũ khí!");
            return;
        }

        if (!isFromScript && attacker != null && !isSkillDamage && !isFromAbility) {
            if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
                ItemStack weapon = attacker.getInventory().getItemInMainHand();
                if (weapon != null && !weapon.getType().isAir()) {
                    if (!MMOCORE.canUse(attacker, weapon)) {
                        event.setCancelled(true);
                        attacker.sendMessage("§cNo Level !:)");
                        return;
                    }
                }
            }
        }

        double currentDamage = isFromScript ? scriptDamage : event.getDamage();

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

        // Xóa metadata hiển thị cũ
        target.removeMetadata("DISPLAY_SPECIAL_STATUS", Main.getInstance());
        target.removeMetadata("LAST_HIT_CRIT", Main.getInstance());
        target.removeMetadata("DISPLAY_ELEMENTS_DATA", Main.getInstance());
        target.removeMetadata("DISPLAY_NORMAL_DAMAGE", Main.getInstance());
        target.removeMetadata("DISPLAY_TRUE_DAMAGE", Main.getInstance());
        target.removeMetadata("DISPLAY_MAGIC_DAMAGE", Main.getInstance());
        target.removeMetadata("DISPLAY_PENDING", Main.getInstance());

        if (attacker != null && !isSkillDamage) {
            attackerStats = PlayerCombatCache.getStats(attacker.getUniqueId());
            java.util.UUID attackerUuid = attacker.getUniqueId();

            if (event.getDamager() instanceof Projectile) {
                currentDamage += PlayerCombatCache.getEffective(attackerUuid, "bow_damage", attackerStats.totalBowDamage);
            }

            currentDamage += (PlayerCombatCache.getEffective(attackerUuid, "damage", attackerStats.totalBonusDmg) * curseMultiplier);

            double pvpPveMultiplier = (target instanceof Player)
                    ? PlayerCombatCache.getEffective(attackerUuid, "pvp_damage", attackerStats.totalPvpBonus)
                    : PlayerCombatCache.getEffective(attackerUuid, "pve_damage", attackerStats.totalPveBonus);
            currentDamage *= (1 + pvpPveMultiplier / 100.0);
            currentDamage *= (1 + PlayerCombatCache.getEffective(attackerUuid, "all_damage", attackerStats.totalAllDamage) / 100.0);

            if (attackerStats.weaponElementLevels != null && !attackerStats.weaponElementLevels.isEmpty()) {
                for (Map.Entry<String, Integer> entry : attackerStats.weaponElementLevels.entrySet()) {
                    String eId = entry.getKey();
                    int attackLevel = entry.getValue();
                    int defenseLevel = 0;
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

            double effectiveCritChance = PlayerCombatCache.getEffective(attackerUuid, "critical_chance", attackerStats.totalCritChance);
            if (random.nextDouble() * 100 <= effectiveCritChance) {
                double baseCritMult = Main.getInstance().getCustomListenerConfig().getDouble("crit-multiplier", 1.5);
                double effectiveCritDamage = PlayerCombatCache.getEffective(attackerUuid, "critical_damage", attackerStats.totalCritDamage);
                double critMultiplier = baseCritMult + (effectiveCritDamage / 100.0);

                // Áp dụng giảm sát thương chí mạng của nạn nhân (nếu có)
                if (target instanceof Player victimForCrit) {
                    PlayerCombatCache.CombatStats victimCritStats = PlayerCombatCache.getStats(victimForCrit.getUniqueId());
                    double effectiveCritDmgReduction = PlayerCombatCache.getEffective(
                            victimForCrit.getUniqueId(), "crit_damage_reduction",
                            victimCritStats != null ? victimCritStats.totalCritDamageReduction : 0.0);
                    if (effectiveCritDmgReduction > 0) {
                        double maxReduction = Main.getInstance().getCustomListenerConfig().getDouble("crit-damage-reduction-cap", 80.0);
                        double reduction = Math.min(effectiveCritDmgReduction, maxReduction) / 100.0;
                        critMultiplier = Math.max(1.0, critMultiplier * (1.0 - reduction / 100.0));
                    }
                }

                currentDamage *= critMultiplier;
                target.setMetadata("LAST_HIT_CRIT", new FixedMetadataValue(Main.getInstance(), true));
            }
            double effectiveDeepWound = PlayerCombatCache.getEffective(attackerUuid, "deep_wound", attackerStats.totalDeepWound);
            if (effectiveDeepWound > 0) {
                int durationTicks = Main.getInstance().getCustomListenerConfig()
                        .getInt("deep-wound-duration-ticks", 60);
                double existing = target.hasMetadata("DEEP_WOUND_REDUCTION")
                        ? target.getMetadata("DEEP_WOUND_REDUCTION").get(0).asDouble() : 0.0;
                double newVal = Math.max(existing, Math.min(effectiveDeepWound, 100.0));
                target.setMetadata("DEEP_WOUND_REDUCTION", new FixedMetadataValue(Main.getInstance(), newVal));

                // FIX: cancel task cũ trước khi tạo mới
                if (target.hasMetadata("DEEP_WOUND_TASK")) {
                    Bukkit.getScheduler().cancelTask(target.getMetadata("DEEP_WOUND_TASK").get(0).asInt());
                    target.removeMetadata("DEEP_WOUND_TASK", Main.getInstance());
                }
                int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    target.removeMetadata("DEEP_WOUND_REDUCTION", Main.getInstance());
                    target.removeMetadata("DEEP_WOUND_TASK", Main.getInstance());
                }, durationTicks).getTaskId();
                target.setMetadata("DEEP_WOUND_TASK", new FixedMetadataValue(Main.getInstance(), taskId));
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

        if (target instanceof Player victim) {
            victimStats = PlayerCombatCache.getStats(victim.getUniqueId());
            java.util.UUID victimUuid = victim.getUniqueId();

            double rawDodge = (victimStats != null)
                    ? PlayerCombatCache.getEffective(victimUuid, "dodge_rate", victimStats.totalDodge) : 0;
            double attackerAccuracy = (attackerStats != null)
                    ? PlayerCombatCache.getEffective(attacker.getUniqueId(), "accuracy", attackerStats.totalAccuracy) : 0.0;
            double finalDodgeChance = Math.max(0, rawDodge - attackerAccuracy);

            if (random.nextDouble() * 100 <= finalDodgeChance) {
                target.setMetadata("DISPLAY_SPECIAL_STATUS", new FixedMetadataValue(Main.getInstance(), "DODGE"));
                event.setCancelled(true);
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                return;
            }

            double blockChance = (victimStats != null)
                    ? PlayerCombatCache.getEffective(victimUuid, "block_rate", victimStats.totalBlock) : 0;
            if (random.nextDouble() * 100 <= blockChance) {
                target.setMetadata("DISPLAY_SPECIAL_STATUS", new FixedMetadataValue(Main.getInstance(), "BLOCK"));
                event.setCancelled(true);
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                return;
            }

            double defMultiplier = (attacker != null)
                    ? PlayerCombatCache.getEffective(victimUuid, "pvp_defense", victimStats.totalPvpDef)
                    : PlayerCombatCache.getEffective(victimUuid, "pve_defense", victimStats.totalPveDef);
            currentDamage *= Math.max(0, 1 - defMultiplier / 100.0);
            currentDamage *= Math.max(0, 1 - PlayerCombatCache.getEffective(victimUuid, "all_defense", victimStats.totalAllDefense) / 100.0);

            double finalArmor = PlayerCombatCache.getEffective(victimUuid, "armor", victimStats.totalArmor);
            if (attackerStats != null) {
                java.util.UUID atkUuid = attacker.getUniqueId();
                double effectiveArmorPen = PlayerCombatCache.getEffective(atkUuid, "armor_pen", attackerStats.totalArmorPen);
                double effectivePenetration = PlayerCombatCache.getEffective(atkUuid, "penetration", attackerStats.totalPenetration);
                double armorAfterFlatPen = Math.max(0, finalArmor - effectiveArmorPen);
                finalArmor = armorAfterFlatPen * Math.max(0, 1 - effectivePenetration / 100.0);
            }
            currentDamage = applyArmorFormula(currentDamage, finalArmor);
            double effectiveDamageReduction = (victimStats != null)
                    ? PlayerCombatCache.getEffective(victimUuid, "damage_reduction", victimStats.totalDamageReduction) : 0.0;
            if (effectiveDamageReduction > 0) {
                double minDamage = Main.getInstance().getCustomListenerConfig().getDouble("damage-reduction-min", 1.0);
                currentDamage = Math.max(minDamage, currentDamage - effectiveDamageReduction);
            }
            if (!isFromScript && event.getDamager() instanceof LivingEntity attackerEntity && !isSkillDamage && !isFromAbility) {
                handleCachedAbilities(victim, attackerEntity, victimStats.bestAbilities, currentDamage, "defense");
                handleCachedAbilities(victim, attackerEntity, victimStats.bestAbilities, currentDamage, "defense_self");
            }
        }

        double extraFromAbilities = 0.0;
        if (target.hasMetadata(METADATA_EXTRA_DAMAGE)) {
            extraFromAbilities = target.getMetadata(METADATA_EXTRA_DAMAGE).get(0).asDouble();
            target.removeMetadata(METADATA_EXTRA_DAMAGE, Main.getInstance());
        }

        double trueDmg = (attackerStats != null)
                ? PlayerCombatCache.getEffective(attacker.getUniqueId(), "true_damage", attackerStats.totalTrueDamage)
                : 0.0;

        double effectiveMagicDamage = (attackerStats != null)
                ? PlayerCombatCache.getEffective(attacker.getUniqueId(), "magic_damage", attackerStats.totalMagicDamage)
                : 0.0;

        double finalMagicDmg = 0.0;
        if (attackerStats != null && effectiveMagicDamage > 0) {
            if (isFromAbility || isFromScript || isSkillDamage) {
                double rawMagic = effectiveMagicDamage * curseMultiplier;
                if (target instanceof Player && victimStats != null) {
                    double effectiveMagicDef = PlayerCombatCache.getEffective(
                            ((Player) target).getUniqueId(), "magic_defense", victimStats.totalMagicDefense);
                    finalMagicDmg = applyMagicDefenseFormula(rawMagic, effectiveMagicDef);
                } else {
                    finalMagicDmg = rawMagic;
                }
            }
        }

        double effectiveDeathDamage = (attackerStats != null)
                ? PlayerCombatCache.getEffective(attacker.getUniqueId(), "death_damage", attackerStats.totalDeathDamage)
                : 0.0;

        double finalDeathDmg = 0.0;
        if (attackerStats != null && effectiveDeathDamage > 0) {
            double threshold = Main.getInstance().getCustomListenerConfig().getDouble("death-damage-threshold", 50.0);
            double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double currentHealthPercent = (target.getHealth() / maxHealth) * 100.0;

            if (currentHealthPercent <= threshold) {
                finalDeathDmg = effectiveDeathDamage * curseMultiplier;
                target.setMetadata("IS_DEATH_STRIKE", new FixedMetadataValue(Main.getInstance(), true));
            }
        }

        double finalElementDamage = weaponElementTotalDmg
                + (attackerStats != null ? attackerStats.totalElementDamage * curseMultiplier : 0.0);

        double finalPhysicalDmg = Math.max(0, currentDamage) + extraFromAbilities;

        double theoreticalTotal = finalPhysicalDmg + finalElementDamage + trueDmg + finalDeathDmg + finalMagicDmg;

        // Set các value khác (giữ nguyên)
        target.setMetadata("VALUE_FINAL_DAMAGE", new FixedMetadataValue(Main.getInstance(), theoreticalTotal));
        target.setMetadata("VALUE_ELEMENTAL_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalElementDamage));
        target.setMetadata("VALUE_MAGIC_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalMagicDmg));
        target.setMetadata("VALUE_TRUE_DAMAGE", new FixedMetadataValue(Main.getInstance(), trueDmg));

        if (attackerStats != null) {
            target.setMetadata("STAT_ATTACKER_CRIT_CHANCE", new FixedMetadataValue(Main.getInstance(),
                    PlayerCombatCache.getEffective(attacker.getUniqueId(), "critical_chance", attackerStats.totalCritChance)));
            target.setMetadata("STAT_ATTACKER_CRIT_DMG", new FixedMetadataValue(Main.getInstance(),
                    PlayerCombatCache.getEffective(attacker.getUniqueId(), "critical_damage", attackerStats.totalCritDamage)));
        }
        if (victimStats != null && target instanceof Player victimForDisplay) {
            target.setMetadata("STAT_VICTIM_DODGE", new FixedMetadataValue(Main.getInstance(),
                    PlayerCombatCache.getEffective(victimForDisplay.getUniqueId(), "dodge_rate", victimStats.totalDodge)));
        }

        target.setMetadata("DISPLAY_MAGIC_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalMagicDmg));

        if (elementDisplayBuilder.length() > 0) {
            target.setMetadata("DISPLAY_ELEMENTS_DATA", new FixedMetadataValue(Main.getInstance(), elementDisplayBuilder.toString()));
        }

        event.setDamage(theoreticalTotal);

        // ── Passive Triggers ──────────────────────────────────────────────────────────
        final Player attackerFinal = attacker;
        final boolean isCritForPassive = target.hasMetadata("LAST_HIT_CRIT");
        boolean isFatalBlows = target instanceof org.bukkit.entity.LivingEntity livingTarget
                && theoreticalTotal >= livingTarget.getHealth();
        if (attackerFinal != null && target instanceof Player victimPlayerPassive) {
            org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                    org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_HIT,
                    attackerFinal, victimPlayerPassive, theoreticalTotal, isCritForPassive, event
            );
        } else if (attackerFinal != null) {
            // FIX: truyền target (mob) thay vì null — đây là lý do passive không trigger khi đánh mob.
            org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                    org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_HIT,
                    attackerFinal, target, theoreticalTotal, isCritForPassive, event
            );
        }

        // ON_TAKE_DAMAGE: actor = nạn nhân (target), victim-trong-context = nguồn gây damage.
        // Mở rộng so với bản cũ: trigger bất kể "kẻ đánh" là Player hay mob/projectile,
        // miễn target (người nhận damage) là Player và sở hữu passive đó.
        if (target instanceof Player victimAsActor) {
            LivingEntity damageSource = (event.getDamager() instanceof LivingEntity le) ? le : null;
            org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                    org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_TAKE_DAMAGE,
                    victimAsActor, damageSource, theoreticalTotal, false, event
            );
        }

        if (attackerFinal != null && isFatalBlows) {
            // FIX: truyền target (LivingEntity, có thể là mob) thay vì ép về null khi không
            // phải Player — đây là lý do ON_KILL "không áp dụng" khi kill mob: victim luôn null
            // nên mechanic như DROP_ITEM/EXPLODE target VICTIM không có entity nào để tác động.
            org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                    org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_KILL,
                    attackerFinal,
                    target,
                    theoreticalTotal, false, event
            );
        }

        // ──────────────────────────────────────────────────────────────
        if (!isFromScript && !isSkillDamage && !isFromAbility && !isFromThorns && damageBeforeReduction > 0) {
            double effectiveThorns = (victimStats != null && target instanceof Player thornsVictim)
                    ? PlayerCombatCache.getEffective(thornsVictim.getUniqueId(), "thorns", victimStats.totalThorns) : 0.0;
            if (effectiveThorns > 0 && event.getDamager() instanceof LivingEntity attackerEntity) {
                double reflected = damageBeforeReduction * (effectiveThorns / 100.0);
                if (reflected > 0) {
                    attackerEntity.setMetadata("THORNS_REFLECT", new FixedMetadataValue(Main.getInstance(), true));
                    attackerEntity.damage(reflected, target);
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                            () -> attackerEntity.removeMetadata("THORNS_REFLECT", Main.getInstance()), 1L);
                }
            }

            double effectiveLifesteal = (attackerStats != null && attacker != null)
                    ? PlayerCombatCache.getEffective(attacker.getUniqueId(), "lifesteal", attackerStats.totalLifesteal) : 0.0;
            if (effectiveLifesteal > 0 && attacker != null && theoreticalTotal > 0) {
                applyLifesteal(attacker, theoreticalTotal, effectiveLifesteal);
            }
        }

        // ====================== FINAL DISPLAY LOGIC (ĐÃ SỬA) ======================
        double displayPhysicalFinal = finalPhysicalDmg;
        if (finalDeathDmg > 0) {
            displayPhysicalFinal += finalDeathDmg;
            target.setMetadata("IS_DEATH_STRIKE_HIT", new FixedMetadataValue(Main.getInstance(), true));
        }

        final double fPhysical = displayPhysicalFinal;
        final double fTrue = trueDmg;
        final double fMagic = finalMagicDmg;
        final boolean isCritFinal = target.hasMetadata("LAST_HIT_CRIT");

        boolean isFatalBlow = (target.getHealth() - theoreticalTotal) <= 0;

        double displayNormal = fPhysical;
        double displayTrue = fTrue;
        double displayMagic = fMagic;

        // Cộng dồn nếu có damage pending
        if (target.hasMetadata("DISPLAY_PENDING")) {
            double oldNormal = target.hasMetadata("DISPLAY_NORMAL_DAMAGE") ? target.getMetadata("DISPLAY_NORMAL_DAMAGE").get(0).asDouble() : 0;
            double oldTrue = target.hasMetadata("DISPLAY_TRUE_DAMAGE") ? target.getMetadata("DISPLAY_TRUE_DAMAGE").get(0).asDouble() : 0;
            double oldMagic = target.hasMetadata("DISPLAY_MAGIC_DAMAGE") ? target.getMetadata("DISPLAY_MAGIC_DAMAGE").get(0).asDouble() : 0;

            displayNormal += oldNormal;
            displayTrue += oldTrue;
            displayMagic += oldMagic;
        }

        // Set metadata cuối
        target.setMetadata("DISPLAY_NORMAL_DAMAGE", new FixedMetadataValue(Main.getInstance(), displayNormal));
        target.setMetadata("DISPLAY_TRUE_DAMAGE", new FixedMetadataValue(Main.getInstance(), displayTrue));
        target.setMetadata("DISPLAY_MAGIC_DAMAGE", new FixedMetadataValue(Main.getInstance(), displayMagic));

        if (isCritFinal) {
            target.setMetadata("LAST_HIT_CRIT", new FixedMetadataValue(Main.getInstance(), true));
        }

        // Hiển thị
        if (isFatalBlow) {
            org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
        } else {
            if (!target.hasMetadata("DISPLAY_PENDING")) {
                target.setMetadata("DISPLAY_PENDING", new FixedMetadataValue(Main.getInstance(), true));
            }

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (target == null || !target.isValid() || target.isDead()) return;
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                target.removeMetadata("DISPLAY_PENDING", Main.getInstance());
            });
        }

    }

    /**
     * Xử lý sát thương từ nguồn không rõ (poison, fall, void, burn, custom skill,...)
     * Áp dụng cho cả Player và Mob
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnknownDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return; // Đã xử lý ở event chính
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (event.getFinalDamage() <= 0) return;

        // ── Passive Trigger: ON_TAKE_DAMAGE cho damage tự gây (fall/fire/poison/void/lava...) ──
        // victim = target chính nó (actor) -> cho phép condition "target-type: SELF" hoạt động
        // (passive chỉ áp dụng khi nạn nhân tự gây damage lên bản thân, không phải bị ai đánh).
        // Chỉ trigger khi target là Player, vì PassiveManager đọc passive_ids từ equipment player.
        if (target instanceof Player selfDamagedPlayer) {
            org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                    org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_TAKE_DAMAGE,
                    selfDamagedPlayer, selfDamagedPlayer, event.getFinalDamage(), false
            );
        }

        // Xóa metadata cũ để tránh hiển thị lẫn lộn
        clearDisplayMetadata(target);

        double damage = event.getFinalDamage();

        // Set metadata cho hiển thị Normal Damage
        target.setMetadata("DISPLAY_NORMAL_DAMAGE", new FixedMetadataValue(Main.getInstance(), damage));
        target.setMetadata("DISPLAY_TRUE_DAMAGE", new FixedMetadataValue(Main.getInstance(), 0));
        target.setMetadata("DISPLAY_MAGIC_DAMAGE", new FixedMetadataValue(Main.getInstance(), 0));

        // Kiểm tra có phải đòn kết liễu không
        boolean isFatal = (target.getHealth() - damage) <= 0;

        if (isFatal) {
            // Nếu là đòn giết → hiển thị ngay
            org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
        } else {
            // Damage thường → delay 1 tick để gộp nếu có nhiều damage cùng lúc
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (target.isValid() && !target.isDead()) {
                    org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                }
            });
        }

    }
    private void clearDisplayMetadata(LivingEntity target) {
        target.removeMetadata("DISPLAY_NORMAL_DAMAGE", Main.getInstance());
        target.removeMetadata("DISPLAY_TRUE_DAMAGE", Main.getInstance());
        target.removeMetadata("DISPLAY_MAGIC_DAMAGE", Main.getInstance());
        target.removeMetadata("DISPLAY_ELEMENTS_DATA", Main.getInstance());
        target.removeMetadata("LAST_HIT_CRIT", Main.getInstance());
        target.removeMetadata("DISPLAY_PENDING", Main.getInstance());
    }
    /**
     * HÀM QUAN TRỌNG: Cung cấp dữ liệu cho PlaceholderAPI (Sửa lỗi BUILD FAILURE)
     */
    public static Map<String, Double> calculateFullStaticStats(Player player) {
        Map<String, Double> stats = new HashMap<>();
        PlayerCombatCache.CombatStats cached = PlayerCombatCache.getStats(player.getUniqueId());
        java.util.UUID uuid = player.getUniqueId();

        // Dùng getEffective() thay vì đọc field gốc trực tiếp, để placeholder hiển thị đúng
        // số liệu thật (đã cộng buff tạm từ passive BUFF_STAT) — khớp với cách onDamage() tính
        // damage thật, tránh trường hợp combat tính 1 số nhưng UI/placeholder hiện số khác.
        double effBonusDmg   = PlayerCombatCache.getEffective(uuid, "damage", cached.totalBonusDmg);
        double effTrueDmg    = PlayerCombatCache.getEffective(uuid, "true_damage", cached.totalTrueDamage);
        double effCritChance = PlayerCombatCache.getEffective(uuid, "critical_chance", cached.totalCritChance);
        double effCritDamage = PlayerCombatCache.getEffective(uuid, "critical_damage", cached.totalCritDamage);
        double effArmor      = PlayerCombatCache.getEffective(uuid, "armor", cached.totalArmor);
        double effDodge      = PlayerCombatCache.getEffective(uuid, "dodge_rate", cached.totalDodge);
        double effBlock      = PlayerCombatCache.getEffective(uuid, "block_rate", cached.totalBlock);
        double effThorns     = PlayerCombatCache.getEffective(uuid, "thorns", cached.totalThorns);
        double effLifesteal  = PlayerCombatCache.getEffective(uuid, "lifesteal", cached.totalLifesteal);

        double baseTotal = effBonusDmg + effTrueDmg + cached.totalElementDamage;

        stats.put("base", baseTotal);
        stats.put("chance", effCritChance);
        stats.put("damage", effCritDamage);
        stats.put("element", cached.totalElementDamage);
        stats.put("armor", effArmor);
        stats.put("dodge", effDodge);
        stats.put("block", effBlock);
        stats.put("thorns", effThorns);
        stats.put("lifesteal", effLifesteal);

        return stats;
    }
    public static void reloadAbilityTriggerCache() {
        abilityTriggerCache = new HashMap<>();
        var section = Main.getInstance().getAbilityTargetConfig().getConfigurationSection("Abilities");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                abilityTriggerCache.put(key.toUpperCase(), section.getString(key, "attack"));
            }
        }
    }
    private void handleCachedAbilities(Player player, LivingEntity opponent, Map<String, double[]> abilities, double damage, String triggerType) {
        if (abilities == null || abilities.isEmpty()) return;
        if (abilityTriggerCache == null) reloadAbilityTriggerCache();

        for (Map.Entry<String, double[]> entry : abilities.entrySet()) {
            String abilityName = entry.getKey();
            double[] data = entry.getValue();
            String configTrigger = abilityTriggerCache.getOrDefault(abilityName, "attack");
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

        // Áp dụng vết thương sâu nếu player đang bị debuff
        if (player.hasMetadata("DEEP_WOUND_REDUCTION")) {
            double reduction = player.getMetadata("DEEP_WOUND_REDUCTION").get(0).asDouble();
            heal *= Math.max(0.0, 1.0 - (reduction / 100.0));
        }

        double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHp, player.getHealth() + heal));
    }

    private double applyArmorFormula(double damage, double armor) {
        if (cachedArmorFormula == null) {
            cachedArmorFormula = Main.getInstance().getCustomListenerConfig()
                    .getString("armor-formula", "default");
            if (!cachedArmorFormula.equals("default") && !cachedArmorFormula.equals("damage * (100 / (100 + armor))")) {
                try {
                    cachedArmorExpression = new ExpressionBuilder(cachedArmorFormula)
                            .variables("damage", "armor").build();
                } catch (Exception ex) {
                    cachedArmorFormula = "default";
                }
            }
        }
        if (cachedArmorExpression == null) {
            return armor <= 0 ? damage : damage * (100.0 / (100.0 + armor));
        }
        try {
            return cachedArmorExpression
                    .setVariable("damage", damage)
                    .setVariable("armor", armor)
                    .evaluate();
        } catch (Exception ex) {
            return armor <= 0 ? damage : damage * (100.0 / (100.0 + armor));
        }
    }
    private double applyMagicDefenseFormula(double damage, double mDef) {
        if (mDef <= 0) return damage;
        if (cachedMagicFormula == null) {
            cachedMagicFormula = Main.getInstance().getCustomListenerConfig()
                    .getString("magic-defense-formula", "default");
            if (!cachedMagicFormula.equals("default") && !cachedMagicFormula.equals("damage * (100 / (100 + mDef))")) {
                try {
                    cachedMagicExpression = new ExpressionBuilder(cachedMagicFormula)
                            .variables("damage", "mDef").build();
                } catch (Exception ex) {
                    cachedMagicFormula = "default";
                }
            }
        }
        if (cachedMagicExpression == null) {
            return damage * (100.0 / (100.0 + mDef));
        }
        try {
            return cachedMagicExpression
                    .setVariable("damage", damage)
                    .setVariable("mDef", mDef)
                    .evaluate();
        } catch (Exception ex) {
            return damage * (100.0 / (100.0 + mDef));
        }
    }
    public static EventDamage instance;

    public EventDamage() {
        instance = this;
    }
    public static void resetFormulaCache() {
        if (instance != null) {
            instance.cachedArmorFormula = null;
            instance.cachedArmorExpression = null;
            instance.cachedMagicFormula = null;
            instance.cachedMagicExpression = null;
        }
    }
}