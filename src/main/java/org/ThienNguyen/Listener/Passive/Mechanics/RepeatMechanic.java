package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Lặp lại TOÀN BỘ children, cách nhau interval-seconds, tổng "times" lần.
 *
 * QUAN TRỌNG: KHÔNG extends AbstractMechanic — cùng lý do với DelayMechanic.
 * AbstractMechanic tự chạy children ngay lập tức sau khi doExecute() trả true,
 * sẽ làm children bị chạy thêm 1 lần ngoài ý muốn cộng dồn với vòng lặp ở đây
 * (vd times=4 sẽ thành 5 lần thực thi thay vì 4, hoặc gây hiện tượng "nổ kép"
 * như đã quan sát thấy trong thực tế). RepeatMechanic implements PassiveMechanic
 * trực tiếp, tự quản lý toàn bộ vòng lặp + children.
 *
 * yml:
 * - type: REPEAT
 *   times: "4"
 *   interval-seconds: "1"
 *   children:
 *     - type: EXPLODE
 *       ...
 *
 * "Thành công" = đã lên lịch lặp thành công (times >= 1 và children không rỗng).
 */
public class RepeatMechanic implements PassiveMechanic {

    private final String rawTimes;
    private final String rawInterval;
    private final List<PassiveMechanic> children;

    public RepeatMechanic(ConfigurationSection cfg) {
        this.rawTimes    = cfg.getString("times", "1");
        this.rawInterval = cfg.getString("interval-seconds", "1");
        this.children    = MechanicChildrenParser.parse(cfg, "children");
    }

    @Override
    public boolean execute(PassiveContext ctx) {
        if (children.isEmpty()) return false;

        int times    = ExpressionResolver.resolveInt(rawTimes,    ctx.getActor(), 1);
        int interval = ExpressionResolver.resolveInt(rawInterval, ctx.getActor(), 1);
        if (times < 1) return false;
        if (interval < 0) interval = 0;

        final long intervalTicks = interval * 20L;
        final int[] count = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (ctx.getActor() == null || !ctx.getActor().isOnline() || count[0] >= times) {
                    cancel();
                    return;
                }
                for (PassiveMechanic m : children) {
                    m.execute(ctx);
                }
                count[0]++;
                if (count[0] >= times) cancel();
            }
        }.runTaskTimer(Main.getInstance(), 0L, Math.max(intervalTicks, 1L));

        return true;
    }
}