package org.ThienNguyen.EditItem;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AttributeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length < 2) {
            player.sendMessage("§6§l[Attribute] §eCách dùng:");
            player.sendMessage("§7- /attribute add <loại> <giá trị> <number/scalar> [slot]");
            player.sendMessage("§7- /attribute remove <loại>");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§cBạn phải cầm một vật phẩm trên tay!");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;

        String action = args[0].toLowerCase();

        try {
            if (action.equals("remove")) {
                Attribute attr = findAttribute(args[1]);
                if (attr == null) throw new IllegalArgumentException("Không tìm thấy thuộc tính!");
                meta.removeAttributeModifier(attr);
                item.setItemMeta(meta);
                player.sendMessage("§aĐã xóa thuộc tính §e" + args[1].toUpperCase());
                return true;
            }

            if (action.equals("add") && args.length >= 4) {
                Attribute attr = findAttribute(args[1]);
                if (attr == null) throw new IllegalArgumentException("Loại thuộc tính không tồn tại!");

                double value = Double.parseDouble(args[2]);
                AttributeModifier.Operation operation = args[3].equalsIgnoreCase("scalar") ?
                        AttributeModifier.Operation.ADD_SCALAR : AttributeModifier.Operation.ADD_NUMBER;

                
                EquipmentSlot slot = null;
                if (args.length >= 5) {
                    slot = parseSlot(args[4]);
                }

                AttributeModifier modifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "WindyAttribute",
                        value,
                        operation,
                        slot
                );

                meta.addAttributeModifier(attr, modifier);
                item.setItemMeta(meta);

                String slotMsg = (slot == null) ? "Tất cả vị trí" : slot.name();
                player.sendMessage("§aĐã thêm §e" + attr.name() + " §avới giá trị §e" + value + " §7(Slot: " + slotMsg + ")");
                return true;
            }
        } catch (Exception e) {
            player.sendMessage("§cLỗi: " + e.getMessage());
        }

        return true;
    }

    private Attribute findAttribute(String name) {
        try { return Attribute.valueOf(name.toUpperCase()); } catch (Exception e) { return null; }
    }

    private EquipmentSlot parseSlot(String input) {
        return switch (input.toLowerCase()) {
            case "mainhand" -> EquipmentSlot.HAND;
            case "offhand" -> EquipmentSlot.OFF_HAND;
            case "helmet", "head" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "leggings" -> EquipmentSlot.LEGS;
            case "boots", "feet" -> EquipmentSlot.FEET;
            default -> null; 
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            for (String s : Arrays.asList("add", "remove"))
                if (s.startsWith(args[0].toLowerCase())) suggestions.add(s);
            return suggestions;
        }

        if (args.length == 2) {
            String input = args[1].toUpperCase();
            for (Attribute attr : Attribute.values())
                if (attr.name().startsWith(input)) suggestions.add(attr.name());
            return suggestions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            suggestions.add("1.0");
            return suggestions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            for (String op : Arrays.asList("number", "scalar"))
                if (op.startsWith(args[3].toLowerCase())) suggestions.add(op);
            return suggestions;
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("add")) {
            List<String> slots = Arrays.asList("any", "mainhand", "offhand", "helmet", "chest", "leggings", "boots");
            for (String s : slots)
                if (s.startsWith(args[4].toLowerCase())) suggestions.add(s);
            return suggestions;
        }

        return suggestions;
    }
}