package org.ThienNguyen.EditItem;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemFlagCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("windycraft.itemflag")) {
            player.sendMessage("§cBạn không có quyền dùng lệnh này.");
            return true;
        }

        // Kiểm tra cấu trúc: /itemflag hide <type> <true/false>
        if (args.length < 3 || !args[0].equalsIgnoreCase("hide")) {
            player.sendMessage("§cCách dùng: /itemflag hide <type> <true/false>");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage("§cBạn phải cầm một vật phẩm trên tay!");
            return true;
        }

        String typeStr = args[1].toUpperCase();
        boolean enable = Boolean.parseBoolean(args[2]);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            try {
                // Chuyển đổi string sang Enum ItemFlag (ví dụ: attribute -> HIDE_ATTRIBUTES)
                ItemFlag flag = convertToFlag(typeStr);

                if (enable) {
                    meta.addItemFlags(flag);
                    player.sendMessage("§aĐã §lẨN §7thông tin §e" + typeStr + " §7trên vật phẩm.");
                } else {
                    meta.removeItemFlags(flag);
                    player.sendMessage("§eĐã §lHIỆN §7thông tin §e" + typeStr + " §7trên vật phẩm.");
                }

                item.setItemMeta(meta);
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cLoại Flag không hợp lệ! Ví dụ: ENCHANTS, ATTRIBUTES, UNBREAKABLE...");
            }
        }

        return true;
    }

    private ItemFlag convertToFlag(String input) {
        // Hỗ trợ gõ tắt hoặc gõ đầy đủ
        return switch (input) {
            case "ENCHANTS", "ENCHANT" -> ItemFlag.HIDE_ENCHANTS;
            case "ATTRIBUTES", "ATTRIBUTE" -> ItemFlag.HIDE_ATTRIBUTES;
            case "UNBREAKABLE" -> ItemFlag.HIDE_UNBREAKABLE;
            case "DESTROYS" -> ItemFlag.HIDE_DESTROYS;
            case "PLACED_ON" -> ItemFlag.HIDE_PLACED_ON;
            case "POTION", "EFFECTS" -> ItemFlag.HIDE_POTION_EFFECTS;
            case "DYE" -> ItemFlag.HIDE_DYE;
            default -> ItemFlag.valueOf("HIDE_" + input);
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("hide");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("hide")) {
            return Arrays.asList("ENCHANTS", "ATTRIBUTES", "UNBREAKABLE", "POTION", "DYE", "DESTROYS");
        }
        if (args.length == 3) {
            return Arrays.asList("true", "false");
        }
        return new ArrayList<>();
    }
}