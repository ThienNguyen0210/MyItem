package org.ThienNguyen.Listener.Passive.Trigger;

import org.ThienNguyen.Listener.Passive.PassiveManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Lắng nghe BlockBreakEvent để kích hoạt passive trigger: ON_BLOCK_BREAK.
 *
 * MONITOR priority, ignoreCancelled=true — chỉ trigger nếu việc phá block THẬT SỰ xảy ra
 * (không bị WorldGuard/claim plugin/permission khác huỷ trước đó). Đây là lý do
 * ON_BLOCK_BREAK tự "tôn trọng" mọi protection plugin: nếu BlockBreakEvent gốc bị cancel,
 * passive cũng không bao giờ chạy theo.
 *
 * Đăng ký trong Main.onEnable():
 *   getServer().getPluginManager().registerEvents(new PassiveBlockBreakListener(), this);
 */
public class PassiveBlockBreakListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        PassiveManager.getInstance().triggerBlockBreak(event.getPlayer(), event.getBlock());
    }
}