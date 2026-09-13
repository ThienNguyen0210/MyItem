package org.ThienNguyen.EditItem;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
public class ItemFlagCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("windycraft.itemflag")) {
            player.sendMessage("§8[§bMyItem§8] §cYou do not have permission to use this command.");
            return true;
        }
        if (args.length < 3 || !args[0].equalsIgnoreCase("hide")) {
            player.sendMessage("§8[§bMyItem§8] §cUsage: /itemflag hide <type> <true/false>");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage("§8[§bMyItem§8] §cYou must hold an item in your hand!");
            return true;
        }
        String typeStr = args[1].toUpperCase();
        boolean enable = Boolean.parseBoolean(args[2]);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try {
                ItemFlag flag = convertToFlag(typeStr);
                if (enable) {
                    meta.addItemFlags(flag);
                    if (flag == ItemFlag.HIDE_ATTRIBUTES) {
                        double baseAttackDamage = getBaseAttackDamage(item.getType());
                        double finalDamage = baseAttackDamage + 0.2;
                        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
                                new AttributeModifier(UUID.randomUUID(), "generic.attack_damage", finalDamage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
                    }
                    player.sendMessage("§8[§bMyItem§8] §aSuccessfully §lHIDDEN §7information §e" + typeStr + " §7on the item.");
                } else {
                    meta.removeItemFlags(flag);
                    if (flag == ItemFlag.HIDE_ATTRIBUTES) {
                        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
                    }
                    player.sendMessage("§8[§bMyItem§8] §eSuccessfully §lSHOWN §7information §e" + typeStr + " §7on the item.");
                }
                item.setItemMeta(meta);
            } catch (IllegalArgumentException e) {
                player.sendMessage("§8[§bMyItem§8] §cInvalid Flag type! Example: ENCHANTS, ATTRIBUTES, UNBREAKABLE, TOOLTIP...");
            }
        }
        return true;
    }
    private double getBaseAttackDamage(Material material) {
        String name = material.name();
        if (name.contains("NETHERITE_SWORD")) return 8.0;
        if (name.contains("DIAMOND_SWORD")) return 7.0;
        if (name.contains("IRON_SWORD")) return 6.0;
        if (name.contains("STONE_SWORD") || name.contains("NETHERITE_AXE")) return 5.0;
        if (name.contains("WOODEN_SWORD") || name.contains("GOLDEN_SWORD") || name.contains("DIAMOND_AXE")) return 4.0;
        if (name.contains("IRON_AXE")) return 5.0;
        if (name.contains("STONE_AXE")) return 4.0;
        if (name.contains("WOODEN_AXE") || name.contains("GOLDEN_AXE")) return 3.0;
        return 1.0;
    }
    private ItemFlag convertToFlag(String input) {
        return switch (input) {
            case "ENCHANTS", "ENCHANT" -> ItemFlag.HIDE_ENCHANTS;
            case "ATTRIBUTES", "ATTRIBUTE" -> ItemFlag.HIDE_ATTRIBUTES;
            case "UNBREAKABLE" -> ItemFlag.HIDE_UNBREAKABLE;
            case "DESTROYS" -> ItemFlag.HIDE_DESTROYS;
            case "PLACED_ON" -> ItemFlag.HIDE_PLACED_ON;
            case "POTION", "EFFECTS" -> ItemFlag.HIDE_ADDITIONAL_TOOLTIP;
            case "TOOLTIP", "ADDITIONAL_TOOLTIP" -> ItemFlag.HIDE_ADDITIONAL_TOOLTIP;
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
            return Arrays.asList("ENCHANTS", "ATTRIBUTES", "UNBREAKABLE", "POTION", "TOOLTIP", "DYE", "DESTROYS");
        }
        if (args.length == 3) {
            return Arrays.asList("true", "false");
        }
        return new ArrayList<>();
    }
}