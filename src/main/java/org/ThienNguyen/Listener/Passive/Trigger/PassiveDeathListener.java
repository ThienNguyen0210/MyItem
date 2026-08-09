package org.ThienNguyen.Listener.Passive.Trigger;

import org.ThienNguyen.Listener.Passive.PassiveManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Lắng nghe PlayerDeathEvent để kích hoạt passive trigger: ON_DEATH.
 *
 * actor = player vừa chết (chủ passive).
 * victim-trong-context = kẻ giết (nếu có, ví dụ để mechanic DAMAGE "trả thù" lúc chết
 * có đối tượng để tác động). Có thể null nếu chết do môi trường (fall, void, lava...).
 *
 * Đăng ký trong Main.onEnable(): getServer().getPluginManager().registerEvents(new PassiveDeathListener(), this);
 */
public class PassiveDeathListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player actor = event.getEntity();

        // Dùng getKiller() — có sẵn từ rất lâu trên mọi version Bukkit/Spigot/Paper, an toàn nhất.
        // Trả về Player đã đánh đòn cuối nếu chết do PvP, null nếu chết do môi trường/mob/v.v.
        // (Nếu cần bắt cả mob giết, cân nhắc parse event.getDeathMessage() hoặc dùng
        //  EntityDamageEvent cuối cùng lưu qua metadata — không làm ở bản này để giữ đơn giản.)
        LivingEntity killer = actor.getKiller();

        PassiveManager.getInstance().trigger(
                PassiveTrigger.ON_DEATH,
                actor,
                killer,
                0.0,
                false
        );
    }
}