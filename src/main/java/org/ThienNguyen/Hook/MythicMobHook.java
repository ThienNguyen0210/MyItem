package org.ThienNguyen.Hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class MythicMobHook {

    // Kiểm tra xem Plugin MythicMobs có đang bật hay không
    public static boolean isMythicMobsEnabled() {
        return Bukkit.getPluginManager().getPlugin("MythicMobs") != null &&
                Bukkit.getPluginManager().getPlugin("MythicMobs").isEnabled();
    }

    public static String getMythicName(Entity entity) {
        // Nếu không có plugin thì trả về null luôn, không chạy code bên dưới
        if (!isMythicMobsEnabled()) {
            return null;
        }

        // Gọi hàm xử lý ở một class con để tránh lỗi load class khi khởi động
        return MythicHandler.getInternalName(entity);
    }

    // Class nội bộ này chỉ được load khi thực sự có MythicMobs
    private static class MythicHandler {
        private static String getInternalName(Entity entity) {
            if (io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().isMythicMob(entity)) {
                return io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager()
                        .getMythicMobInstance(entity).getType().getInternalName();
            }
            return null;
        }
    }
}