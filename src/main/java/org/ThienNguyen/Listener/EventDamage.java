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
import java.util.UUID;

public class EventDamage implements Listener {
    private String cachedArmorFormula = null;
    private String cachedMagicFormula = null;
    private net.objecthunter.exp4j.Expression cachedArmorExpression = null;
    private net.objecthunter.exp4j.Expression cachedMagicExpression = null;
    private static Map<String, String> abilityTriggerCache = null;
    private final Random random = new Random();
    private final Map<UUID, Long> hitTriggerCooldown = new HashMap<>();
    private final String METADATA_CURSE = "CURSED_REDUCTION";
    private static final String METADATA_EXTRA_DAMAGE = "ABILITY_EXTRA_DAMAGE";

    // ===== Config value caches (avoid re-reading FileConfiguration on every hit) =====
    private Double cachedCritMultiplierBase = null;
    private Double cachedCritDamageReductionCap = null;
    private Integer cachedDeepWoundDurationTicks = null;
    private Double cachedDamageReductionMin = null;
    private Double cachedDeathDamageThreshold = null;
    private final Map<String, double[]> elementBaseConfigCache = new HashMap<>();
    private Boolean mmoCoreEnabledCache = null;

    // Periodic sweep counter for hitTriggerCooldown to prevent unbounded growth
    private int cooldownCleanupCounter = 0;
    private static final int COOLDOWN_CLEANUP_INTERVAL = 200;
    private static final long COOLDOWN_ENTRY_TTL_MS = 5000L;
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Captured up-front, before any of our own damage math or Bukkit's own damage
        // modifiers (ARMOR, RESISTANCE, ABSORPTION, ...) run. event.setDamage(...) below
        // only overrides the BASE modifier — vanilla still layers its own modifiers on top
        // when the final damage is applied to health, so a victim in full armor can end up
        // taking noticeably less than our theoretical combat-math total. Comparing health
        // before vs. after the hit (further down) gives us the number that was truly dealt.
        final double healthAtHitStart = target.getHealth();

        if (event.getDamager().hasMetadata("THORNS_REFLECT")) {
            return;
        }
        if (event.getEntity().hasMetadata("passive_damage_skip")) {
            return;
        }
        // INVINCIBLE_STATUS: bất tử tạm thời — cancel toàn bộ damage kể cả script/ability.
        // Set bởi StatusMechanic (type: STATUS, status: INVINCIBLE).
        if (target.hasMetadata("INVINCIBLE_STATUS")) {
            event.setCancelled(true);
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
            // Consume immediately (mirrors SKILL_DAMAGE_VALUE below). These flags mark a single
            // in-flight ability hit; if left in place, an unrelated attack landing on the same
            // target while a DoT/channeled ability is still "in progress" would also be
            // misclassified as ability damage and bail out before event.setDamage() runs,
            // making that attack deal 0 damage.
            if (target.hasMetadata("IS_ABILITY")) target.removeMetadata("IS_ABILITY", Main.getInstance());
            if (target.hasMetadata("IS_SKILL_PROCESS")) target.removeMetadata("IS_SKILL_PROCESS", Main.getInstance());
        }

        // ========== XỬ LÝ HIỂN THỊ CHO SKILL/ABILITY ==========
        if (isFromAbility) {
            double damageToDisplay = isFromScript ? scriptDamage : event.getDamage();

            // Set damage vào metadata Normal Damage
            TextDisplayManager.setNormalDamage(target, damageToDisplay);

            // Gọi hiển thị damage
            org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
        }

        // Return sớm cho Ability (sau khi đã hiển thị)
        if (!isFromScript && isFromAbility) {
            return;
        }

        // Already checked at the top of this method and we would have returned there if
        // true (nothing sets THORNS_REFLECT in between), so this is provably false here.
        // Kept as a named boolean so the `!isFromThorns` check further down stays readable
        // without a second Bukkit metadata lookup for information we already have.
        boolean isFromThorns = false;

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

        // STUNNED_STATUS: kẻ bị stun không thể tấn công (khác ROOTED_STATUS — chỉ khoá
        // di chuyển, vẫn tấn công bình thường nên KHÔNG có check tương ứng ở đây).
        // Set bởi StatusMechanic (type: STATUS, status: STUN).
        if (!isFromScript && event.getDamager().hasMetadata("STUNNED_STATUS") && !isFromAbility) {
            event.setCancelled(true);
            if (attacker != null) attacker.sendActionBar("§c§l✖ Bạn đang bị choáng!");
            return;
        }

        // A "basic attack" is a raw melee hit or bow shot coming straight from a player's
        // weapon — no skill, ability, or script damage involved. All damage modification
        // (stat bonuses, crit, elemental/magic/true/death damage, dodge/block/armor
        // mitigation) below is applied ONLY when this is true; skill/ability/script damage
        // now passes straight through to event.setDamage() unmodified.
        boolean isBasicAttack = attacker != null && !isFromScript && !isFromAbility && !isSkillDamage;

        if (isBasicAttack && isMmoCoreEnabled()) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (weapon != null && !weapon.getType().isAir()) {
                if (!MMOCORE.canUse(attacker, weapon)) {
                    event.setCancelled(true);
                    attacker.sendMessage("§cNo Level !:)");
                    return;
                }
            }
        }

        double curseMultiplier = 1.0;
        if (event.getDamager().hasMetadata(METADATA_CURSE)) {
            double reductionPercent = event.getDamager().getMetadata(METADATA_CURSE).get(0).asDouble();
            curseMultiplier = Math.max(0.1, 1.0 - (Math.min(90.0, reductionPercent) / 100.0));
        }

        double currentDamage = isFromScript ? scriptDamage : event.getDamage();

        // Skills report their base damage via SKILL_DAMAGE_VALUE; add the caster's
        // magic_damage stat on top of it here, the same way melee/bow add their "damage"
        // stat below. magic_defense (further down) then mitigates this for the victim.
        if (isFromScript && attacker != null) {
            PlayerCombatCache.CombatStats casterStats = PlayerCombatCache.getStats(attacker.getUniqueId());
            double effCasterMagicDamage = PlayerCombatCache.getEffective(
                    attacker.getUniqueId(), "magic_damage", casterStats.totalMagicDamage);
            currentDamage += effCasterMagicDamage * curseMultiplier;
        }

        double damageBeforeReduction = currentDamage;

        PlayerCombatCache.CombatStats attackerStats = null;
        // Fetched once upfront (instead of separately inside the crit-reduction check and
        // again later) and reused everywhere the victim's stats are needed for this hit.
        PlayerCombatCache.CombatStats victimStats = (target instanceof Player targetAsPlayer)
                ? PlayerCombatCache.getStats(targetAsPlayer.getUniqueId()) : null;
        double weaponElementTotalDmg = 0.0;
        StringBuilder elementDisplayBuilder = new StringBuilder();

        // These are computed once below (when an attacker exists) and reused later for the
        // STAT_ATTACKER_* display metadata, instead of calling PlayerCombatCache.getEffective()
        // a second time for the exact same UUID+key.
        double effAttackerCritChance = 0.0;
        double effAttackerCritDamage = 0.0;
        // Tracks whether this hit crit, mirroring the setLastHitCrit() call below, so
        // later code in this same method can check the flag locally instead of asking Bukkit
        // to hand back a value we just set ourselves a few lines earlier.
        boolean isCritHit = false;
        // Same idea for the victim's dodge rate, reused for STAT_VICTIM_DODGE later.
        double effVictimDodgeRate = 0.0;
        // Same idea for the attacker's magic_damage stat, reused between the ability-power
        // calc and the final magic-damage calc instead of being fetched twice.
        double effAttackerMagicDamage = 0.0;

        // Xóa metadata hiển thị cũ
        TextDisplayManager.clearDisplayData(target);

        if (isBasicAttack) {
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
                    double[] elementBaseCfg = getElementBaseConfig(eId);
                    double baseDmg = elementBaseCfg[0];
                    double perDmg = elementBaseCfg[1];

                    double eDmg = (baseDmg + (effectiveLevel * perDmg)) * curseMultiplier;
                    weaponElementTotalDmg += eDmg;

                    if (elementDisplayBuilder.length() > 0) elementDisplayBuilder.append(",");
                    elementDisplayBuilder.append(eId).append(":").append(effectiveLevel).append(":").append(String.format("%.1f", eDmg));

                    org.ThienNguyen.Element.ElementCore.playEffect(target, eId);
                }
            }

            effAttackerCritChance = PlayerCombatCache.getEffective(attackerUuid, "critical_chance", attackerStats.totalCritChance);
            effAttackerCritDamage = PlayerCombatCache.getEffective(attackerUuid, "critical_damage", attackerStats.totalCritDamage);
            // logic critical
            if (random.nextDouble() * 100 <= effAttackerCritChance) {
                double baseCritMult = getCritMultiplierBase();
                double critMultiplier = baseCritMult + (effAttackerCritDamage / 100.0);

                // Áp dụng giảm sát thương chí mạng của nạn nhân (nếu có)
                if (target instanceof Player victimForCrit) {
                    double effectiveCritDmgReduction = PlayerCombatCache.getEffective(
                            victimForCrit.getUniqueId(), "crit_damage_reduction",
                            victimStats != null ? victimStats.totalCritDamageReduction : 0.0);
                    if (effectiveCritDmgReduction > 0) {
                        double maxReduction = getCritDamageReductionCap();
                        double reduction = Math.min(effectiveCritDmgReduction, maxReduction) / 100.0;
                        critMultiplier = Math.max(1.0, critMultiplier * (1.0 - reduction / 100.0));
                    }
                }

                currentDamage *= critMultiplier;
                isCritHit = true;
                TextDisplayManager.setLastHitCrit(target);
                CritEffectManager.playCritEffect(target);
            }
            double effectiveDeepWound = PlayerCombatCache.getEffective(attackerUuid, "deep_wound", attackerStats.totalDeepWound);
            if (effectiveDeepWound > 0) {
                int durationTicks = getDeepWoundDurationTicks();
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

            // Dùng getEffective() thay vì đọc field gốc trực tiếp, để buff tạm (magic_damage)
            // cũng ảnh hưởng tới ngưỡng proc ability — khớp với cách finalMagicDmg tính bên dưới.
            effAttackerMagicDamage = PlayerCombatCache.getEffective(
                    attackerUuid, "magic_damage", attackerStats.totalMagicDamage);
            double magicDmgForAbility = effAttackerMagicDamage * curseMultiplier;
            double totalPowerForAbility = currentDamage + magicDmgForAbility;

            handleCachedAbilities(attacker, target, attackerStats.bestAbilities, totalPowerForAbility, "attack");
            handleCachedAbilities(attacker, target, attackerStats.bestAbilities, totalPowerForAbility, "attack_self");
        } else {
            damageBeforeReduction = currentDamage;
        }

        // Damage from anything other than a standard melee/bow/mob attack — i.e. skills,
        // abilities, and scripted damage — is mitigated by the victim's magic_defense stat.
        // (isFromAbility is only ever true here alongside isFromScript, since a pure
        // ability hit with no script damage already returned early above.)
        boolean isSkillLikeDamage = isFromScript || isFromAbility || isSkillDamage;

        // ĐIỀU KIỆN MỚI: Cho phép cả Player đánh Player lẫn Mob (LivingEntity) đánh Player được tính phòng thủ
        boolean isMobOrBasicAttack = (isBasicAttack || (event.getDamager() instanceof LivingEntity && attacker == null))
                && !isFromScript && !isFromAbility && !isSkillDamage;

        if (isMobOrBasicAttack && target instanceof Player victim) {
            java.util.UUID victimUuid = victim.getUniqueId();

            effVictimDodgeRate = (victimStats != null)
                    ? PlayerCombatCache.getEffective(victimUuid, "dodge_rate", victimStats.totalDodge) : 0;
            double attackerAccuracy = (attackerStats != null)
                    ? PlayerCombatCache.getEffective(attacker.getUniqueId(), "accuracy", attackerStats.totalAccuracy) : 0.0;
            double finalDodgeChance = Math.max(0, effVictimDodgeRate - attackerAccuracy);

            if (random.nextDouble() * 100 <= finalDodgeChance) {
                TextDisplayManager.setSpecialStatus(target, "DODGE");
                event.setCancelled(true);
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                return;
            }

            double blockChance = (victimStats != null)
                    ? PlayerCombatCache.getEffective(victimUuid, "block_rate", victimStats.totalBlock) : 0;
            if (random.nextDouble() * 100 <= blockChance) {
                TextDisplayManager.setSpecialStatus(target, "BLOCK");
                event.setCancelled(true);
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                return;
            }

            // Phân biệt chính xác pvp_defense (nếu đánh bởi Player) hay pve_defense (nếu đánh bởi Mob)
            boolean isAttackerAPlayer = (attacker != null);
            double defMultiplier = isAttackerAPlayer
                    ? PlayerCombatCache.getEffective(victimUuid, "pvp_defense", victimStats != null ? victimStats.totalPvpDef : 0.0)
                    : PlayerCombatCache.getEffective(victimUuid, "pve_defense", victimStats != null ? victimStats.totalPveDef : 0.0);

            currentDamage *= Math.max(0, 1 - defMultiplier / 100.0);

            // Kháng toàn phần (all_defense)
            double allDefPercent = victimStats != null ? PlayerCombatCache.getEffective(victimUuid, "all_defense", victimStats.totalAllDefense) : 0.0;
            if (allDefPercent >= 100.0) {
                currentDamage = 0;
            } else {
                currentDamage *= Math.max(0, 1 - allDefPercent / 100.0);
            }

            double finalArmor = victimStats != null ? PlayerCombatCache.getEffective(victimUuid, "armor", victimStats.totalArmor) : 0.0;
            if (attackerStats != null && isAttackerAPlayer) {
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
                double minDamage = getDamageReductionMin();
                currentDamage = Math.max(minDamage, currentDamage - effectiveDamageReduction);
            }
            if (event.getDamager() instanceof LivingEntity attackerEntity && victimStats != null) {
                handleCachedAbilities(victim, attackerEntity, victimStats.bestAbilities, currentDamage, "defense");
                handleCachedAbilities(victim, attackerEntity, victimStats.bestAbilities, currentDamage, "defense_self");
            }
        } else if (isSkillLikeDamage && target instanceof Player magicVictim) {
            double effectiveMagicDef = (victimStats != null)
                    ? PlayerCombatCache.getEffective(magicVictim.getUniqueId(), "magic_defense", victimStats.totalMagicDefense)
                    : 0.0;
            currentDamage = applyMagicDefenseFormula(currentDamage, effectiveMagicDef);
        }

        double extraFromAbilities = 0.0;
        if (target.hasMetadata(METADATA_EXTRA_DAMAGE)) {
            extraFromAbilities = target.getMetadata(METADATA_EXTRA_DAMAGE).get(0).asDouble();
            target.removeMetadata(METADATA_EXTRA_DAMAGE, Main.getInstance());
        }

        double trueDmg = (attackerStats != null)
                ? PlayerCombatCache.getEffective(attacker.getUniqueId(), "true_damage", attackerStats.totalTrueDamage)
                : 0.0;

        double finalMagicDmg = 0.0;

        double effectiveDeathDamage = (attackerStats != null)
                ? PlayerCombatCache.getEffective(attacker.getUniqueId(), "death_damage", attackerStats.totalDeathDamage)
                : 0.0;

        double finalDeathDmg = 0.0;
        if (attackerStats != null && effectiveDeathDamage > 0) {
            double threshold = getDeathDamageThreshold();
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

        TextDisplayManager.setMagicDamage(target, finalMagicDmg);

        if (elementDisplayBuilder.length() > 0) {
            TextDisplayManager.setElementsData(target, elementDisplayBuilder.toString());
        }

        event.setDamage(theoreticalTotal);

        // ── Passive Triggers ──────────────────────────────────────────────────────────
        final Player attackerFinal = attacker;
        final boolean isCritForPassive = isCritHit;
        boolean isFatalBlows = target instanceof org.bukkit.entity.LivingEntity livingTarget
                && theoreticalTotal >= livingTarget.getHealth();

        if (attackerFinal != null) {
            long now = System.currentTimeMillis();
            UUID targetKey = target.getUniqueId();
            if (hitTriggerCooldown.getOrDefault(targetKey, 0L) + 500 <= now) {
                hitTriggerCooldown.put(targetKey, now);

                if (++cooldownCleanupCounter >= COOLDOWN_CLEANUP_INTERVAL) {
                    cooldownCleanupCounter = 0;
                    long cutoff = now - COOLDOWN_ENTRY_TTL_MS;
                    hitTriggerCooldown.entrySet().removeIf(entry -> entry.getValue() < cutoff);
                }

                if (target instanceof Player victimPlayerPassive) {
                    org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                            org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_HIT,
                            attackerFinal, victimPlayerPassive, theoreticalTotal, isCritForPassive, event
                    );
                } else {
                    org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                            org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_HIT,
                            attackerFinal, target, theoreticalTotal, isCritForPassive, event
                    );
                }
            }
        } else if (attackerFinal != null) {
            org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                    org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_HIT,
                    attackerFinal, target, theoreticalTotal, isCritForPassive, event
            );
        }

        if (target instanceof Player victimAsActor) {
            LivingEntity damageSource = (event.getDamager() instanceof LivingEntity le) ? le : null;
            org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                    org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_TAKE_DAMAGE,
                    victimAsActor, damageSource, theoreticalTotal, false, event
            );
        }

        if (attackerFinal != null && isFatalBlows) {
            org.ThienNguyen.Listener.Passive.PassiveManager.getInstance().trigger(
                    org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger.ON_KILL,
                    attackerFinal,
                    target,
                    theoreticalTotal, false, event
            );
        }

        // ──────────────────────────────────────────────────────────────
        if (isBasicAttack && !isFromThorns && damageBeforeReduction > 0) {
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

        // ====================== FINAL DISPLAY LOGIC ======================
        double displayPhysicalFinal = finalPhysicalDmg;
        if (finalDeathDmg > 0) {
            displayPhysicalFinal += finalDeathDmg;
            target.setMetadata("IS_DEATH_STRIKE_HIT", new FixedMetadataValue(Main.getInstance(), true));
        }

        final double fPhysical = displayPhysicalFinal;
        final double fTrue = trueDmg;
        final double fMagic = finalMagicDmg;

        boolean isFatalBlow = (target.getHealth() - theoreticalTotal) <= 0;

        double displayNormal = fPhysical;
        double displayTrue = fTrue;
        double displayMagic = fMagic;

        boolean hadPendingDisplay = TextDisplayManager.isPending(target);

        // Only snapshot health on the first hit of a display batch — later hits merged
        // into the same batch (hadPendingDisplay == true) must keep comparing against the
        // health from *before the first hit*, not their own intermediate health.
        if (!hadPendingDisplay) {
            TextDisplayManager.setHealthBeforeHit(target, healthAtHitStart);
        }

        if (hadPendingDisplay) {
            displayNormal += TextDisplayManager.getNormalDamage(target);
            displayTrue += TextDisplayManager.getTrueDamage(target);
            displayMagic += TextDisplayManager.getMagicDamage(target);
        }

        TextDisplayManager.setNormalDamage(target, displayNormal);
        TextDisplayManager.setTrueDamage(target, displayTrue);
        TextDisplayManager.setMagicDamage(target, displayMagic);

        if (isFatalBlow) {
            // Fatal/overkill blows show the raw theoretical damage (old logic), not a
            // health-delta correction: a killing blow can legitimately be far larger than
            // the victim's remaining HP (e.g. 100,000 raw damage on a 350 HP mob), and
            // capping/rescaling to remaining health would wrongly shrink that down to 350.
            // The health-delta correction only makes sense for non-fatal hits, where the
            // victim survives and we can measure exactly what came off their health bar.
            org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
        } else {
            if (!hadPendingDisplay) {
                TextDisplayManager.setPending(target);
            }

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (target == null || !target.isValid() || target.isDead()) return;

                // Rescale the displayed normal/true/magic values so their sum matches the
                // damage that was truly subtracted from health, instead of our theoretical
                // combat-math total — e.g. a target in full armor may have taken noticeably
                // less than what we calculated, since vanilla applies its own ARMOR/
                // RESISTANCE/etc. modifiers on top of the base damage we set.
                Double healthBeforeHit = TextDisplayManager.getHealthBeforeHit(target);
                if (healthBeforeHit != null) {
                    double actualDamage = Math.max(0, healthBeforeHit - target.getHealth());
                    TextDisplayManager.scaleDisplayToActualDamage(target, actualDamage);
                }

                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                TextDisplayManager.clearPending(target);
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
        // INVINCIBLE_STATUS: bất tử tạm thời — cancel cả damage môi trường (fall/fire/poison/void).
        if (target.hasMetadata("INVINCIBLE_STATUS")) {
            event.setCancelled(true);
            return;
        }

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
        TextDisplayManager.setNormalDamage(target, damage);
        TextDisplayManager.setTrueDamage(target, 0);
        TextDisplayManager.setMagicDamage(target, 0);

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

    /**
     * REVIVE_ARMED: chặn đòn chí mạng lên Player đang được "vũ khí hộ mệnh" bảo vệ
     * (StatusMechanic... không, cụ thể là RevivalMechanic — type: REVIVE) và hồi sinh
     * thay vì để họ chết thật.
     *
     * Đăng ký trên EntityDamageEvent (KHÔNG phải EntityDamageByEntityEvent) ở priority
     * MONITOR để bắt được CẢ 2 nguồn damage — entity tấn công lẫn môi trường (fall/fire/
     * poison/void/...) — giống cách onUnknownDamage() ở trên đã làm, nhưng KHÔNG cần guard
     * "instanceof EntityDamageByEntityEvent" vì logic revive ở đây độc lập hoàn toàn với
     * logic hiển thị/tính damage của 2 handler kia — không có gì để double-xử lý cả.
     *
     * MONITOR chạy sau khi onDamage()/onUnknownDamage() đã tính xong final damage (kể cả
     * damage đã bị custom combat system ở trên ghi đè qua event.setDamage()), nên
     * event.getFinalDamage() ở đây phản ánh đúng số damage THẬT SẼ trừ vào máu.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFatalDamageRevive(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasMetadata("REVIVE_ARMED")) return;

        boolean wouldBeFatal = (player.getHealth() - event.getFinalDamage()) <= 0;
        if (!wouldBeFatal) return;

        event.setCancelled(true);

        if (!player.hasMetadata("REVIVE_MECHANIC_REF")) return; // an toàn — không nên xảy ra
        Object ref = player.getMetadata("REVIVE_MECHANIC_REF").get(0).value();
        if (ref instanceof org.ThienNguyen.Listener.Passive.Mechanics.RevivalMechanic mechanic) {
            mechanic.onRevive(player);
        }
    }

    private void clearDisplayMetadata(LivingEntity target) {
        TextDisplayManager.clearDisplayData(target);
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

    private boolean isMmoCoreEnabled() {
        if (mmoCoreEnabledCache == null) {
            mmoCoreEnabledCache = Bukkit.getPluginManager().isPluginEnabled("MMOCore");
        }
        return mmoCoreEnabledCache;
    }

    private double getCritMultiplierBase() {
        if (cachedCritMultiplierBase == null) {
            cachedCritMultiplierBase = Main.getInstance().getCustomListenerConfig().getDouble("crit-multiplier", 1.5);
        }
        return cachedCritMultiplierBase;
    }

    private double getCritDamageReductionCap() {
        if (cachedCritDamageReductionCap == null) {
            cachedCritDamageReductionCap = Main.getInstance().getCustomListenerConfig().getDouble("crit-damage-reduction-cap", 80.0);
        }
        return cachedCritDamageReductionCap;
    }

    private int getDeepWoundDurationTicks() {
        if (cachedDeepWoundDurationTicks == null) {
            cachedDeepWoundDurationTicks = Main.getInstance().getCustomListenerConfig().getInt("deep-wound-duration-ticks", 60);
        }
        return cachedDeepWoundDurationTicks;
    }

    private double getDamageReductionMin() {
        if (cachedDamageReductionMin == null) {
            cachedDamageReductionMin = Main.getInstance().getCustomListenerConfig().getDouble("damage-reduction-min", 1.0);
        }
        return cachedDamageReductionMin;
    }

    private double getDeathDamageThreshold() {
        if (cachedDeathDamageThreshold == null) {
            cachedDeathDamageThreshold = Main.getInstance().getCustomListenerConfig().getDouble("death-damage-threshold", 50.0);
        }
        return cachedDeathDamageThreshold;
    }

    /** base-damage / damage-per for an element id, cached per id since these rarely change at runtime. */
    private double[] getElementBaseConfig(String eId) {
        return elementBaseConfigCache.computeIfAbsent(eId, id -> new double[]{
                Main.getInstance().getElementConfig().getDouble(id + ".base-damage", 2.0),
                Main.getInstance().getElementConfig().getDouble(id + ".damage-per", 5.0)
        });
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
            instance.cachedCritMultiplierBase = null;
            instance.cachedCritDamageReductionCap = null;
            instance.cachedDeepWoundDurationTicks = null;
            instance.cachedDamageReductionMin = null;
            instance.cachedDeathDamageThreshold = null;
            instance.elementBaseConfigCache.clear();
            instance.mmoCoreEnabledCache = null;
        }
    }
}