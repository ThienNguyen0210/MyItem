package org.ThienNguyen.Hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.ThienNguyen.Stat.ClassRequire;
import org.ThienNguyen.Stat.LevelRequire;

// Import MMOCore
import net.Indyuce.mmocore.api.player.PlayerData;

// Import Fabled (Cập nhật đúng Class của Studio MageMonkey)
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fabled.api.player.PlayerClass;

public class MMOCORE {

    /**
     * Kiểm tra điều kiện sử dụng item (Level + Class)
     */
    public static boolean canUse(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return true;
        }

        // --- 1. KIỂM TRA LEVEL REQUIRE ---
        int requiredLevel = LevelRequire.get(item);
        int currentPlayerLevel = 0;

        if (isPluginEnabled("MMOCore")) {
            currentPlayerLevel = getPlayerLevelMMOCore(player);
        } else if (isPluginEnabled("Fabled")) {
            currentPlayerLevel = getPlayerLevelFabled(player);
        } else {
            currentPlayerLevel = player.getLevel(); // Vanilla Level
        }

        if (currentPlayerLevel < requiredLevel) {
            return false;
        }

        // --- 2. KIỂM TRA CLASS REQUIRE ---
        String requiredClass = ClassRequire.get(item);
        if (requiredClass == null || requiredClass.isEmpty() || requiredClass.equalsIgnoreCase("None")) {
            return true;
        }

        String playerClassId = null;
        if (isPluginEnabled("MMOCore")) {
            playerClassId = getPlayerClassIdMMOCore(player);
        } else if (isPluginEnabled("Fabled")) {
            playerClassId = getPlayerClassIdFabled(player);
        }

        if (playerClassId == null) {
            return !isPluginEnabled("MMOCore") && !isPluginEnabled("Fabled");
        }

        return playerClassId.equalsIgnoreCase(requiredClass);
    }

    private static boolean isPluginEnabled(String name) {
        return Bukkit.getPluginManager().isPluginEnabled(name);
    }

    // --- LOGIC CHO FABLED ---
    private static int getPlayerLevelFabled(Player player) {
        try {
            studio.magemonkey.fabled.api.player.PlayerData fabledData = Fabled.getData(player);
            if (fabledData != null && fabledData.getMainClass() != null) {
                return fabledData.getMainClass().getLevel();
            }
        } catch (NoClassDefFoundError | Exception ignored) {}
        return 0;
    }

    private static String getPlayerClassIdFabled(Player player) {
        try {
            studio.magemonkey.fabled.api.player.PlayerData fabledData = Fabled.getData(player);
            if (fabledData != null && fabledData.getMainClass() != null) {
                PlayerClass mainClass = fabledData.getMainClass();
                // Lấy tên Class (Ví dụ: Warrior)
                return mainClass.getData().getName();
            }
        } catch (NoClassDefFoundError | Exception ignored) {}
        return null;
    }

    // --- LOGIC CHO MMOCORE ---
    private static int getPlayerLevelMMOCore(Player player) {
        try {
            PlayerData data = PlayerData.get(player);
            return data.getLevel();
        } catch (NoClassDefFoundError | Exception ignored) {}
        return 0;
    }

    private static String getPlayerClassIdMMOCore(Player player) {
        try {
            PlayerData data = PlayerData.get(player);
            if (data.getProfess() != null) {
                return data.getProfess().getId();
            }
        } catch (NoClassDefFoundError | Exception ignored) {}
        return null;
    }
}