package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Chạy 1 command — "van an toàn" cho mọi hành vi chưa có mechanic riêng
 * (give item, gọi plugin khác, broadcast, economy...).
 *
 * yml:
 * - type: COMMAND
 *   target: VICTIM             # SELF | VICTIM — entity dùng để thay placeholder {player}
 *   mode: OP                    # OP (console, full quyền) | PLAYER (chính target chạy, giới hạn quyền họ có)
 *   command: "give {player} diamond 1"
 *
 * Placeholder hỗ trợ trong "command":
 *   {player}  -> tên người chơi của target (resolveTarget theo "target")
 *   {actor}   -> tên người chơi của actor (luôn là chủ passive)
 *   {damage}  -> giá trị damage tại thời điểm trigger (ctx.getDamage()), làm tròn 1 chữ số
 *
 * MODE OP: console chạy lệnh — toàn quyền, dùng cho lệnh admin (give, effect, economy...).
 * MODE PLAYER: chính target chạy lệnh — bị giới hạn theo permission của họ, dùng khi muốn
 * lệnh tôn trọng quyền hạn người chơi (vd lệnh server tự custom có check riêng).
 *
 * "Thành công" = lệnh được gửi đi (không kiểm tra lệnh có thực thi đúng hay không, vì
 * Bukkit không trả về kết quả thực thi đồng bộ cho mọi loại command).
 */
public class CommandMechanic extends AbstractMechanic {

    private final String mode;
    private final String commandTemplate;

    public CommandMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.mode = cfg.getString("mode", "OP").toUpperCase();
        this.commandTemplate = cfg.getString("command", "");
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (commandTemplate.isEmpty()) return false;

        LivingEntity targetEntity = resolveTarget(ctx);
        if (targetEntity == null || !targetEntity.isValid()) return false;

        String playerName = (targetEntity instanceof Player p) ? p.getName() : targetEntity.getName();
        String actorName = (ctx.getActor() != null) ? ctx.getActor().getName() : "";

        String command = commandTemplate
                .replace("{player}", playerName)
                .replace("{actor}", actorName)
                .replace("{damage}", String.format("%.1f", ctx.getDamage()));

        if ("PLAYER".equals(mode)) {
            if (!(targetEntity instanceof Player p)) return false; // mob không chạy command được
            return p.performCommand(command);
        }

        // OP / mặc định: chạy bằng console, toàn quyền.
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}