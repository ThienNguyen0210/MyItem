package org.ThienNguyen.Listener.Passive;

import org.ThienNguyen.Listener.Passive.Mechanics.*;
import org.bukkit.configuration.ConfigurationSection;


public final class MechanicRegistry {

    private MechanicRegistry() {}


    public static org.ThienNguyen.Listener.Passive.PassiveMechanic create(ConfigurationSection section) {
        if (section == null) return null;
        String type = section.getString("type", "").toUpperCase();

        return switch (type) {
            case "DAMAGE"    -> new DamageMechanic(section);
            case "DROP_ITEM" -> new DropItemMechanic(section);
            case "HEAL"      -> new HealMechanic(section);
            case "BUFF_STAT" -> new BuffStatMechanic(section);
            case "EFFECT"    -> new EffectMechanic(section);
            case "EXPLODE"   -> new ExplodeMechanic(section);
            case "SUMMON_TNT" -> new SummonTNTMechanic(section);
            case "SOUND"     -> new SoundMechanic(section);
            case "COMMAND"   -> new CommandMechanic(section);
            case "MESSAGE"   -> new MessageMechanic(section);
            case "LAUNCH"    -> new LaunchMechanic(section);
            case "FLAME"     -> new FlameMechanic(section);
            case "PARTICLE_ANIMATION"  -> new ParticleAnimationMechanic(section);
            case "PARTICLE_PROJECTILE" -> new ParticleProjectileMechanic(section);
            case "BREAK_AREA" -> new BreakAreaMechanic(section);
            case "DELAY"  -> new DelayMechanic(section);
            case "REPEAT" -> new RepeatMechanic(section);
            case "HIT_COUNTER" -> new HitCounterMechanic(section);
            case "SUMMON_MM"      -> new SummonMMMechanic(section);
            case "SUMMON_VANILLA" -> new SummonVanillaMechanic(section);
            case "TITLE" -> new TitleMechanic(section);
            case "STATUS" -> new StatusMechanic(section);
            case "STACK_COUNTER" -> new StackCounterMechanic(section);
            case "MYTHIC_SKILL" -> new MythicSkillMechanic(section);
            case "LIGHTNING" -> new LightningMechanic(section);
            case "TARGET_FILTER" -> new TargetFilterMechanic(section);
            case "REVIVE" -> new RevivalMechanic(section);
            case "ADD_VALUE"   -> new AddValueMechanic(section);
            case "CHECK_VALUE" -> new CheckValueMechanic(section);
            case "PROJECTILE_SHOT" -> new ProjectileShotMechanic(section);
            case "POTION_ZONE" -> new PotionZoneMechanic(section);

            default -> {
                org.ThienNguyen.Main.getInstance().getLogger()
                        .warning("[Passive] Mechanic type không hợp lệ hoặc thiếu: '" + type + "'");
                yield null;
            }
        };
    }
}