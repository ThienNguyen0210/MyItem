package org.ThienNguyen.Listener.Passive;

import org.ThienNguyen.Listener.Passive.Mechanics.BuffStatMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.BreakAreaMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.CommandMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.DamageMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.DropItemMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.EffectMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.ExplodeMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.FlameMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.HealMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.LaunchMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.MessageMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.ParticleAnimationMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.ParticleProjectileMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.SoundMechanic;
import org.bukkit.configuration.ConfigurationSection;
import org.ThienNguyen.Listener.Passive.Mechanics.DelayMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.RepeatMechanic;
/**
 * Factory tạo PassiveMechanic từ 1 block "actions[i]" trong yml, dựa vào key "type".
 *
 * LƯU Ý: nếu bạn đã có MechanicRegistry.java khác, giữ bản của bạn — đây là suy luận lại
 * từ cách PassiveDef.fromYaml() gọi MechanicRegistry.create(actionCfg).
 *
 * MỞ RỘNG: thêm 1 dòng case khi có mechanic mới (ví dụ POTION_EFFECT, COMMAND, TELEPORT...).
 * Không cần sửa PassiveDef hay PassiveManager.
 */
public final class MechanicRegistry {

    private MechanicRegistry() {}

    /**
     * @param section block "actions[i]" trong yml, bắt buộc có key "type"
     * @return instance mechanic tương ứng, hoặc null nếu "type" không hợp lệ/thiếu
     *         (PassiveDef sẽ tự bỏ qua action này nếu trả về null, không crash load).
     */
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
            default -> {
                org.ThienNguyen.Main.getInstance().getLogger()
                        .warning("[Passive] Mechanic type không hợp lệ hoặc thiếu: '" + type + "'");
                yield null;
            }
        };
    }
}