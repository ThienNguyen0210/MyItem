package org.ThienNguyen.Listener.Passive.Mechanics;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;


public class MythicSkillMechanic extends AbstractMechanic {


    private static volatile boolean warnedMissingMythicMobs = false;

    private final String skillName;
    private final String rawDamageMultiplier;
    private final String targetKey;

    public MythicSkillMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.skillName           = cfg.getString("skill", "").trim();
        this.rawDamageMultiplier = cfg.getString("damage-multiplier", "1.0");

        this.targetKey           = cfg.getString("target", "VICTIM").toUpperCase();

        if (skillName.isEmpty()) {
            Bukkit.getLogger().warning(
                    "[MythicSkillMechanic] thiếu key 'skill' trong config — mechanic này sẽ KHÔNG BAO GIỜ cast được gì.");
        }

        if (!warnedMissingMythicMobs && Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            warnedMissingMythicMobs = true;
            Bukkit.getLogger().warning(
                    "[MythicSkillMechanic] Không tìm thấy plugin MythicMobs trên server — mọi action " +
                            "type: MYTHIC_SKILL sẽ luôn thất bại (return false) cho đến khi cài plugin này.");
        }
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (skillName.isEmpty()) return false;
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) return false;

        LivingEntity caster = ctx.getActor();
        if (caster == null) return false;


        LivingEntity target = resolveTarget(ctx);


        Location origin = resolveLocation(ctx);
        if (origin == null) return false;

        double multiplier = ExpressionResolver.resolve(rawDamageMultiplier, ctx.getActor(), 1.0);


        LivingEntity triggerEntity = (target != null) ? target : caster;

        try {
            BukkitAPIHelper helper = MythicBukkit.inst().getAPIHelper();
            return helper.castSkill(
                    caster,
                    skillName,
                    triggerEntity,
                    origin,
                    target != null ? Collections.singletonList(target) : Collections.emptyList(),
                    Collections.singletonList(origin),
                    (float) multiplier,
                    meta -> {}
            );
        } catch (Throwable e) {
            Main.getInstance().getLogger().warning(
                    "[MythicSkillMechanic] Lỗi khi cast skill '" + skillName + "': "
                            + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    protected LivingEntity resolveTarget(PassiveContext ctx) {
        return switch (targetKey) {
            case "ACTOR", "SELF" -> ctx.getActor();
            case "VICTIM"        -> ctx.getVictim();
            default               -> ctx.getActor();
        };
    }




    private Location resolveLocation(PassiveContext ctx) {
        return switch (targetKey) {
            case "ACTOR", "SELF" -> ctx.getActorLocation();
            case "VICTIM"        -> ctx.getVictimLocation();
            default               -> ctx.getActorLocation();
        };
    }
}