package org.ThienNguyen.EditItem;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModelAndMaterial implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§8[§bMyItem§8] §cOnly players can use this command!");
            return true;
        }

        if (!player.hasPermission("myitem.admin")) {
            player.sendMessage("§8[§bMyItem§8] §cYou do not have permission to use this command!");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§8[§bMyItem§8] §cYou must hold an item in your hand!");
            return true;
        }


        if (command.getName().equalsIgnoreCase("setmodel")) {
            if (args.length < 1) {
                player.sendMessage("§8[§bMyItem§8] §cUsage: /setmodel <id>");
                return true;
            }
            try {
                int modelId = Integer.parseInt(args[0]);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(modelId);
                    item.setItemMeta(meta);
                    player.sendMessage("§8[§bMyItem§8] §aSet Custom Model Data to: §f" + modelId);
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§8[§bMyItem§8] §cModel ID must be an integer!");
            }
        }


        else if (command.getName().equalsIgnoreCase("material")) {
            if (args.length < 1) {
                player.sendMessage("§8[§bMyItem§8] §cUsage: /material <material>");
                return true;
            }
            Material mat = Material.matchMaterial(args[0].toUpperCase());
            if (mat == null || !mat.isItem()) {
                player.sendMessage("§8[§bMyItem§8] §cInvalid material §f" + args[0] + "§c!");
                return true;
            }
            item.setType(mat);
            player.sendMessage("§8[§bMyItem§8] §aChanged item material to: §f" + mat.name());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("material") && args.length == 1) {
            List<String> materials = new ArrayList<>();
            for (Material m : Material.values()) {
                if (m.isItem()) materials.add(m.name().toLowerCase());
            }
            String currentArg = args[0].toLowerCase();
            return materials.stream()
                    .filter(s -> s.startsWith(currentArg))
                    .collect(Collectors.toList());
        }

        if (command.getName().equalsIgnoreCase("setmodel") && args.length == 1) {
            return List.of("<id>");
        }

        return new ArrayList<>();
    }
}