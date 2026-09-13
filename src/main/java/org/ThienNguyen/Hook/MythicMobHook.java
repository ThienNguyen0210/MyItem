package org.ThienNguyen.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
public class MythicMobHook {
    public static boolean isMythicMobsEnabled() {
        return Bukkit.getPluginManager().getPlugin("MythicMobs") != null &&
                Bukkit.getPluginManager().getPlugin("MythicMobs").isEnabled();
    }
    public static String getMythicName(Entity entity) {
        if (!isMythicMobsEnabled()) {
            return null;
        }
        return MythicHandler.getInternalName(entity);
    }
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