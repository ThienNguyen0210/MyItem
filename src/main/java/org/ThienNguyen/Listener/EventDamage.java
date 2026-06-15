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
            if (event.getDamager() instanceof Projectile) {
                currentDamage += attackerStats.totalBowDamage;
            }

            currentDamage += (attackerStats.totalBonusDmg * curseMultiplier);

            double pvpPveMultiplier = (target instanceof Player) ? attackerStats.totalPvpBonus : attackerStats.totalPveBonus;

            if (!(target instanceof Player)) {
                try {
                    com.sucy.skill.api.player.PlayerData skillData = com.sucy.skill.SkillAPI.getPlayerData(attacker);
                    if (skillData != null && skillData.hasClass()) {
                        String className = skillData.getMainClass().getData().getName();
                        if (className.equalsIgnoreCase("Mage")) {
                            pvpPveMultiplier += 20.0;
                        }
                    }
                } catch (NoClassDefFoundError | Exception e) {}
            }

            currentDamage *= (1 + pvpPveMultiplier / 100.0);
            currentDamage *= (1 + attackerStats.totalAllDamage / 100.0);

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

            if (random.nextDouble() * 100 <= attackerStats.totalCritChance) {
                double baseCritMult = Main.getInstance().getCustomListenerConfig().getDouble("crit-multiplier", 1.5);
                double critMultiplier = baseCritMult + (attackerStats.totalCritDamage / 100.0);

                // Áp dụng giảm sát thương chí mạng của nạn nhân (nếu có)
                if (target instanceof Player victimForCrit) {
                    PlayerCombatCache.CombatStats victimCritStats = PlayerCombatCache.getStats(victimForCrit.getUniqueId());
                    if (victimCritStats != null && victimCritStats.totalCritDamageReduction > 0) {
                        double maxReduction = Main.getInstance().getCustomListenerConfig().getDouble("crit-damage-reduction-cap", 80.0);
                        double reduction = Math.min(victimCritStats.totalCritDamageReduction, maxReduction) / 100.0;
                        critMultiplier = Math.max(1.0, critMultiplier * (1.0 - reduction / 100.0));
                    }
                }

                currentDamage *= critMultiplier;
                target.setMetadata("LAST_HIT_CRIT", new FixedMetadataValue(Main.getInstance(), true));
            }
            if (attackerStats.totalDeepWound > 0) {
                int durationTicks = Main.getInstance().getCustomListenerConfig()
                        .getInt("deep-wound-duration-ticks", 60);
                double existing = target.hasMetadata("DEEP_WOUND_REDUCTION")
                        ? target.getMetadata("DEEP_WOUND_REDUCTION").get(0).asDouble() : 0.0;
                double newVal = Math.max(existing, Math.min(attackerStats.totalDeepWound, 100.0));
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

            double rawDodge = (victimStats != null ? victimStats.totalDodge : 0);
            double attackerAccuracy = (attackerStats != null ? attackerStats.totalAccuracy : 0.0);
            double finalDodgeChance = Math.max(0, rawDodge - attackerAccuracy);

            if (random.nextDouble() * 100 <= finalDodgeChance) {
                target.setMetadata("DISPLAY_SPECIAL_STATUS", new FixedMetadataValue(Main.getInstance(), "DODGE"));
                event.setCancelled(true);
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                return;
            }

            double blockChance = (victimStats != null ? victimStats.totalBlock : 0);
            if (random.nextDouble() * 100 <= blockChance) {
                target.setMetadata("DISPLAY_SPECIAL_STATUS", new FixedMetadataValue(Main.getInstance(), "BLOCK"));
                event.setCancelled(true);
                org.ThienNguyen.Listener.TextDisplayManager.displayAll(target);
                return;
            }

            double defMultiplier = (attacker != null) ? victimStats.totalPvpDef : victimStats.totalPveDef;
            currentDamage *= Math.max(0, 1 - defMultiplier / 100.0);
            currentDamage *= Math.max(0, 1 - victimStats.totalAllDefense / 100.0);

            double finalArmor = victimStats.totalArmor;
            if (attackerStats != null) {
                double armorAfterFlatPen = Math.max(0, finalArmor - attackerStats.totalArmorPen);
                finalArmor = armorAfterFlatPen * Math.max(0, 1 - attackerStats.totalPenetration / 100.0);
            }
            currentDamage = applyArmorFormula(currentDamage, finalArmor);
            if (victimStats != null && victimStats.totalDamageReduction > 0) {
                double minDamage = Main.getInstance().getCustomListenerConfig().getDouble("damage-reduction-min", 1.0);
                currentDamage = Math.max(minDamage, currentDamage - victimStats.totalDamageReduction);
            }
            if (!isFromScript && event.getDamager() instanceof LivingEntity attackerEntity && !isSkillDamage && !isFromAbility) {
                handleCachedAbilities(victim, attackerEntity, victimStats.bestAbilities, currentDamage, "defense");
                handleCachedAbilities(victim, attackerEntity, victimStats.bestAbilities, currentDamage, "defense_self");
            }
        }

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
            double threshold = Main.getInstance().getCustomListenerConfig().getDouble("death-damage-threshold", 50.0);
            double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double currentHealthPercent = (target.getHealth() / maxHealth) * 100.0;

            if (currentHealthPercent <= threshold) {
                finalDeathDmg = attackerStats.totalDeathDamage * curseMultiplier;
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
            target.setMetadata("STAT_ATTACKER_CRIT_CHANCE", new FixedMetadataValue(Main.getInstance(), attackerStats.totalCritChance));
            target.setMetadata("STAT_ATTACKER_CRIT_DMG", new FixedMetadataValue(Main.getInstance(), attackerStats.totalCritDamage));
        }
        if (victimStats != null) {
            target.setMetadata("STAT_VICTIM_DODGE", new FixedMetadataValue(Main.getInstance(), victimStats.totalDodge));
        }

        target.setMetadata("DISPLAY_MAGIC_DAMAGE", new FixedMetadataValue(Main.getInstance(), finalMagicDmg));

        if (elementDisplayBuilder.length() > 0) {
            target.setMetadata("DISPLAY_ELEMENTS_DATA", new FixedMetadataValue(Main.getInstance(), elementDisplayBuilder.toString()));
        }

        event.setDamage(theoreticalTotal);

        // Thorns & Lifesteal
        if (!isFromScript && !isSkillDamage && !isFromAbility && !isFromThorns && damageBeforeReduction > 0) {
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