package org.ThienNguyen.EditItem;

import net.md_5.bungee.api.ChatColor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Essentials implements CommandExecutor, TabCompleter {

    // Bộ nhớ tạm để lưu lore khi copy
    private static List<String> copiedLore = null;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "setname" -> handleSetName(player, args);
            case "setlore" -> handleSetLore(player, args);
            case "unbreaking" -> handleUnbreaking(player);
        }

        return true;
    }

    private void handleUnbreaking(Player player) {
        if (!player.hasPermission("windycraft.unbreaking")) {
            player.sendMessage("§cBạn không có quyền dùng lệnh này.");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage("§cBạn phải cầm một vật phẩm trên tay!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean isUnbreakable = !meta.isUnbreakable();
            meta.setUnbreakable(isUnbreakable);

            if (isUnbreakable) {
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                player.sendMessage("§aVật phẩm hiện đã §lKHÔNG THỂ BỊ PHÁ HỦY§a.");
            } else {
                meta.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                player.sendMessage("§eVật phẩm đã trở lại trạng thái §lBÌNH THƯỜNG§e.");
            }

            item.setItemMeta(meta);
        }
    }

    private void handleSetName(Player player, String[] args) {
        if (args.length == 0) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(translateColor(String.join(" ", args)));
            item.setItemMeta(meta);
            player.sendMessage("§aĐã đổi tên vật phẩm!");
        }
    }

    private void handleSetLore(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cSử dụng: /setlore <add|set|remove|insert|copy|paste> [args...]");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage("§cBạn phải cầm một vật phẩm trên tay!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String action = args[0].toLowerCase();

        switch (action) {
            case "add" -> {
                if (args.length < 2) return;
                lore.add(translateColor(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
                player.sendMessage("§aĐã thêm dòng lore vào cuối!");
            }
            case "set" -> {
                if (args.length < 3) return;
                int index = getIndex(args[1], lore.size());
                if (index != -1) {
                    String content = translateColor(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                    if (index < lore.size()) {
                        lore.set(index, content);
                    } else {
                        lore.add(content);
                    }
                    player.sendMessage("§aĐã cập nhật dòng số " + (index + 1));
                }
            }
            case "remove" -> {
                if (args.length < 2) return;
                int index = getIndex(args[1], lore.size());
                if (index != -1 && index < lore.size()) {
                    lore.remove(index);
                    player.sendMessage("§aĐã xóa dòng số " + (index + 1));
                }
            }
            case "insert" -> {
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /setlore insert <số dòng> <nội dung>");
                    return;
                }
                int index = getIndex(args[1], lore.size() + 1); // cho phép insert sau dòng cuối
                if (index == -1) {
                    player.sendMessage("§cSố dòng không hợp lệ!");
                    return;
                }
                String content = translateColor(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));

                // Nếu index lớn hơn kích thước hiện tại → chèn cuối
                if (index >= lore.size()) {
                    lore.add(content);
                    player.sendMessage("§aĐã chèn dòng mới vào cuối!");
                } else {
                    lore.add(index, content);
                    player.sendMessage("§aĐã chèn dòng mới vào vị trí " + (index + 1));
                }
            }
            case "copy" -> {
                copiedLore = new ArrayList<>(lore);
                player.sendMessage("§aĐã sao chép " + copiedLore.size() + " dòng lore!");
                if (copiedLore.isEmpty()) {
                    player.sendMessage("§7(Lưu ý: Item này không có lore nào)");
                }
            }
            case "paste" -> {
                if (copiedLore == null) {
                    player.sendMessage("§cBạn chưa copy lore nào! Dùng /setlore copy trước.");
                    return;
                }
                lore.clear();
                lore.addAll(copiedLore);
                player.sendMessage("§aĐã dán " + copiedLore.size() + " dòng lore vào vật phẩm!");
            }
            default -> player.sendMessage("§cHành động không hợp lệ! Dùng: add, set, remove, insert, copy, paste");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private int getIndex(String input, int maxAllowed) {
        if (input.equalsIgnoreCase("last")) return maxAllowed - 1;
        try {
            int i = Integer.parseInt(input) - 1;
            return (i >= 0) ? i : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return null;
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("setlore")) {
            if (args.length == 1) {
                return Arrays.asList("add", "set", "remove", "insert", "copy", "paste");
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR || !item.hasItemMeta()) return null;

            List<String> lore = item.getItemMeta().getLore();
            if (lore == null) lore = new ArrayList<>();

            if (args.length == 2) {
                String action = args[0].toLowerCase();
                if (action.equals("set") || action.equals("remove") || action.equals("insert")) {
                    List<String> suggestions = new ArrayList<>();
                    int max = (action.equals("insert")) ? lore.size() + 1 : lore.size();
                    for (int i = 1; i <= max; i++) {
                        suggestions.add(String.valueOf(i));
                    }
                    if (max > 0) suggestions.add("last");
                    return suggestions;
                }
            }

            if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("insert"))) {
                int idx = getIndex(args[1], lore.size() + (args[0].equalsIgnoreCase("insert") ? 1 : 0));
                if (idx != -1 && idx < lore.size()) {
                    return Collections.singletonList(lore.get(idx).replace("§", "&"));
                }
            }
        }

        if (cmd.equals("setname") && args.length == 1) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return Collections.singletonList(item.getItemMeta().getDisplayName().replace("§", "&"));
            }
        }
        return null;
    }

    private String translateColor(String message) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }
}