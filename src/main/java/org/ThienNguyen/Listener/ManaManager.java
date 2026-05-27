package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ManaManager {

    private static final String MODIFIER_KEY = "windy_custom_stats";

    public static void applyMana(Player player, double maxMana, double manaRegen) {
        if (player == null || !player.isOnline()) return;

        boolean applied = false;

        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            if (applyMMOCoreMana(player, maxMana, manaRegen)) applied = true;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Fabled")) {
            if (applyFabledMana(player, maxMana, manaRegen)) applied = true;
        }

        if (applied && (maxMana > 5 || manaRegen > 0)) {
            Bukkit.getLogger().info("§b[MyItem] Mana applied for " + player.getName()
                    + " → MaxMana: +" + maxMana + " | Regen: +" + manaRegen);
        }
    }

    
    private static boolean applyMMOCoreMana(Player player, double maxMana, double manaRegen) {
        try {
            net.Indyuce.mmocore.api.player.PlayerData data =
                    net.Indyuce.mmocore.api.player.PlayerData.get(player);

            if (data == null || data.getStats() == null) return false;

            var statsMap = data.getStats().getMap();

            
            var maxManaInst = statsMap.getInstance("MAX_MANA");
            if (maxManaInst != null) {
                maxManaInst.removeIf(key -> key.equals(MODIFIER_KEY));
                if (maxMana != 0) {
                    maxManaInst.registerModifier(new io.lumine.mythic.lib.api.stat.modifier.StatModifier(
                            MODIFIER_KEY, "MAX_MANA", maxMana,
                            io.lumine.mythic.lib.player.modifier.ModifierType.FLAT,
                            io.lumine.mythic.lib.api.player.EquipmentSlot.OTHER,
                            io.lumine.mythic.lib.player.modifier.ModifierSource.OTHER));
                }
            }

            
            var regenInst = statsMap.getInstance("MANA_REGENERATION");
            if (regenInst != null) {
                regenInst.removeIf(key -> key.equals(MODIFIER_KEY));
                if (manaRegen != 0) {
                    regenInst.registerModifier(new io.lumine.mythic.lib.api.stat.modifier.StatModifier(
                            MODIFIER_KEY, "MANA_REGENERATION", manaRegen,
                            io.lumine.mythic.lib.player.modifier.ModifierType.FLAT,
                            io.lumine.mythic.lib.api.player.EquipmentSlot.OTHER,
                            io.lumine.mythic.lib.player.modifier.ModifierSource.OTHER));
                }
            }

            data.getStats().updateStats();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    
    private static boolean applyFabledMana(Player player, double maxMana, double manaRegen) {
        try {
            
            com.sucy.skill.api.player.PlayerData skillPlayer = com.sucy.skill.SkillAPI.getPlayerData(player);
            if (skillPlayer == null) return false;

            studio.magemonkey.fabled.api.player.PlayerData data = skillPlayer.getWrapped();
            if (data == null) return false;

            
            data.getStatModifiers().forEach((key, list) -> {
                if (list != null) {
                    list.removeIf(mod -> MODIFIER_KEY.equals(mod.getName()));
                }
            });

            
            if (maxMana != 0) {
                data.addStatModifier("mana",
                        new studio.magemonkey.fabled.api.player.PlayerStatModifier(
                                MODIFIER_KEY, maxMana,
                                studio.magemonkey.fabled.api.enums.Operation.ADD_NUMBER,
                                false),
                        true);
            }

            if (manaRegen != 0) {
                data.addStatModifier("mana-regen",
                        new studio.magemonkey.fabled.api.player.PlayerStatModifier(
                                MODIFIER_KEY, manaRegen,
                                studio.magemonkey.fabled.api.enums.Operation.ADD_NUMBER,
                                false),
                        true);
            }

            data.updatePlayerStat(player);
            return true;

        } catch (Throwable ignored) {
            return false;
        }
    }
}