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
            sender.sendMessage("§cChỉ người chơi mới có thể dùng lệnh này!");
            return true;
        }

        if (!player.hasPermission("myitem.admin")) {
            player.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§cBạn phải cầm một vật phẩm trên tay!");
            return true;
        }

        
        if (command.getName().equalsIgnoreCase("setmodel")) {
            if (args.length < 1) {
                player.sendMessage("§cSử dụng: /setmodel <id>");
                return true;
            }
            try {
                int modelId = Integer.parseInt(args[0]);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(modelId);
                    item.setItemMeta(meta);
                    player.sendMessage("§a[MyItem] Đã đặt Custom Model Data thành: §f" + modelId);
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cID Model phải là một số nguyên!");
            }
        }

        
        else if (command.getName().equalsIgnoreCase("material")) {
            if (args.length < 1) {
                player.sendMessage("§cSử dụng: /material <vật_liệu>");
                return true;
            }
            Material mat = Material.matchMaterial(args[0].toUpperCase());
            if (mat == null || !mat.isItem()) {
                player.sendMessage("§cLoại vật liệu §f" + args[0] + " §ckhông hợp lệ!");
                return true;
            }
            item.setType(mat);
            player.sendMessage("§a[MyItem] Đã chuyển phôi vật phẩm sang: §f" + mat.name());
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