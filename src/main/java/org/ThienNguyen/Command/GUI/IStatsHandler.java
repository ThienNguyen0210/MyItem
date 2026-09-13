package org.ThienNguyen.Command.GUI;
import org.bukkit.entity.Player;
/**
 * Cầu nối để GUIStats gọi lại đúng logic áp stat đang có sẵn của bạn
 * (statsHandler.handleCommand(player, args, slot)) mà không cần GUIStats
 * biết statsHandler thuộc lớp cụ thể nào.
 *
 * Gán 1 lần lúc khởi tạo statsHandler bằng method reference, ví dụ:
 *   GUIStats.setStatsHandler(statsHandler::handleCommand);
 */
@FunctionalInterface
public interface IStatsHandler {
    void handleCommand(Player player, String[] args, String slot);
}