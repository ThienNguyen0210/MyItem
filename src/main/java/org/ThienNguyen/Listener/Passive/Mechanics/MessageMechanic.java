package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Gửi tin nhắn chat riêng cho SELF hoặc VICTIM (chỉ hoạt động nếu target là Player —
 * mob không nhận được message, trả false).
 *
 * yml:
 * - type: MESSAGE
 *   target: SELF                          # SELF | VICTIM
 *   message: "&c[Tử Thần] &7Bạn vừa kết liễu &f{victim_name}&7!"
 *
 * Placeholder hỗ trợ trong "message":
 *   {actor_name}   -> tên actor (chủ passive)
 *   {victim_name}  -> tên victim (đối phương trong context, "Unknown" nếu null/không phải Player)
 *   {damage}       -> giá trị damage tại thời điểm trigger, làm tròn 1 chữ số
 *
 * Hỗ trợ mã màu "&" (tự convert sang "§" qua ChatColor.translateAlternateColorCodes).
 *
 * "Thành công" = message gửi được tới 1 Player hợp lệ.
 */
public class MessageMechanic extends AbstractMechanic {

    private final String messageTemplate;

    public MessageMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.messageTemplate = cfg.getString("message", "");
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (messageTemplate.isEmpty()) return false;

        LivingEntity targetEntity = resolveTarget(ctx);
        if (!(targetEntity instanceof Player player) || !player.isOnline()) return false;

        String actorName = (ctx.getActor() != null) ? ctx.getActor().getName() : "Unknown";
        String victimName = (ctx.getVictim() instanceof Player vp) ? vp.getName()
                : (ctx.getVictim() != null ? ctx.getVictim().getName() : "Unknown");

        String message = messageTemplate
                .replace("{actor_name}", actorName)
                .replace("{victim_name}", victimName)
                .replace("{damage}", String.format("%.1f", ctx.getDamage()));

        message = ChatColor.translateAlternateColorCodes('&', message);

        player.sendMessage(message);
        return true;
    }
}