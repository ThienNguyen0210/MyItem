package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Chờ N giây rồi mới chạy children.
 *
 * QUAN TRỌNG: KHÔNG extends AbstractMechanic. AbstractMechanic tự động chạy
 * children NGAY LẬP TỨC sau khi doExecute() trả true (xem AbstractMechanic.execute()).
 * Nếu DelayMechanic extends AbstractMechanic, children sẽ bị chạy 2 lần: 1 lần
 * ngay lập tức (do AbstractMechanic) + 1 lần đúng lịch delay (do BukkitRunnable ở đây).
 * Vì vậy DelayMechanic implements PassiveMechanic trực tiếp, tự quản lý children
 * hoàn toàn, không đi qua cơ chế children mặc định của AbstractMechanic.
 *
 * yml:
 * - type: DELAY
 *   seconds: "2"
 *   children:
 *     - type: EXPLODE
 *       ...
 *
 * "Thành công" = đã lên lịch delay thành công (trả true ngay, không chờ children).
 */
public class DelayMechanic implements PassiveMechanic {

    private final String rawSeconds;
    private final List<PassiveMechanic> children;

    public DelayMechanic(ConfigurationSection cfg) {
        this.rawSeconds = cfg.getString("seconds", "1");
        this.children   = MechanicChildrenParser.parse(cfg, "children");
    }

    @Override
    public boolean execute(PassiveContext ctx) {
        if (children.isEmpty()) return false;

        int seconds = ExpressionResolver.resolveInt(rawSeconds, ctx.getActor(), 1);
        if (seconds < 0) seconds = 0;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (ctx.getActor() == null || !ctx.getActor().isOnline()) return;
                for (PassiveMechanic m : children) {
                    m.execute(ctx);
                }
            }
        }.runTaskLater(Main.getInstance(), seconds * 20L);

        return true;
    }
}