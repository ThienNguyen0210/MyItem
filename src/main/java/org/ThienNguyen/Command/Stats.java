package org.ThienNguyen.Command;

import org.ThienNguyen.Main;
import org.ThienNguyen.Stat.*;
import org.ThienNguyen.Lore.StatsLore;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

public class Stats {

    /**
     * Cập nhật lại toàn bộ Lore dựa trên PDC hiện có của item.
     */
    public void updateItemLore(ItemStack item) {
        org.ThienNguyen.Lore.StatsLore.updateLore(item);
    }

    /**
     * HÀM GỐC (2 tham số): Để tránh lỗi COMPILATION ERROR trong MyItemCommand hoặc các class cũ.
     */
    public void handleCommand(Player player, String[] args) {
        // Tự động gọi hàm 3 tham số với slot mặc định là "any"
        handleCommand(player, args, "any");
    }

    /**
     * HÀM MỚI (3 tham số): Hỗ trợ xử lý slot từ lệnh /mi stats <type> <value> [slot]
     */
    public void handleCommand(Player player, String[] args, String slot) {
        if (args.length < 3) {
            player.sendMessage("§cSử dụng: /mi stats <loại> <giá trị> [slot]");
            return;
        }

        String type = args[1].toLowerCase();
        String rawValue = args[2];

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§cBạn phải cầm vật phẩm trên tay!");
            return;
        }

        if (type.equals("class_require")) {
            ClassRequire.set(item, rawValue);
        } else {
            try {
                double value = Double.parseDouble(rawValue);
                // Cập nhật giá trị và lưu kèm thông tin slot vào PDC
                updatePDCNumeric(item, type, value, slot);
            } catch (NumberFormatException e) {
                player.sendMessage("§cGiá trị cho chỉ số này phải là một con số!");
                return;
            }
        }

        // Vẽ lại Lore (Sử dụng hệ thống tự động quét PDC của StatsLore)
        updateItemLore(item);

        player.sendMessage("§a[MyItem] Đã cập nhật §f" + type + " §athành §e" + rawValue + " §7(Slot: §b" + slot + "§7)");
    }

    /**
     * Hàm vẽ Lore cũ: Giữ lại để đảm bảo tương thích ngược, không gây lỗi logic cũ.
     */
    public void updateItemLore(ItemStack item, String type, String value) {
        updateItemLore(item);
    }

    /**
     * Cập nhật chỉ số vào PDC và lưu kèm Key quy định Slot sử dụng.
     */
    public void updatePDCNumeric(ItemStack item, String type, double value, String slot) {
        // 1. Gọi các class Stat để lưu giá trị (Logic cũ)
        switch (type) {
            case "damage" -> Damage.setDamage(item, value);
            case "health" -> Health.setHealth(item, value);
            case "armor" -> Armor.setArmor(item, value);
            case "pve_damage" -> PveDamage.set(item, value);
            case "pvp_damage" -> PvpDamage.set(item, value);
            case "pve_defense" -> PveDefense.set(item, value);
            case "pvp_defense" -> PvpDefense.set(item, value);
            case "critical_chance" -> CriticalChance.set(item, value);
            case "critical_damage" -> CriticalDamage.set(item, value);
            case "lifesteal" -> Lifesteal.set(item, value);
            case "dodge_rate" -> DodgeRate.set(item, value);
            case "block_rate" -> BlockRate.set(item, value);
            case "penetration" -> Penetration.set(item, value);
            case "level_require" -> LevelRequire.set(item, (int) value);
            case "true_damage" -> TrueDamage.set(item, value);
            case "thorns" -> Thorns.set(item, value);
            case "max_mana" -> MaxMana.set(item, value);
            case "mana_regen" -> ManaRegen.set(item, value);
            case "exp_bonus" -> ExpBonus.set(item, value);
            case "attack_speed" -> AttackSpeed.set(item, value);
            case "death_damage" -> DeathDamage.set(item, value);
            case "movement_speed" -> MovementSpeed.set(item, value);
            case "armor_pen" -> ArmorPen.set(item, value);
            case "health_regen" -> HealthRegen.set(item, value);
            case "all_damage" -> AllDamage.set(item, value); // Sát thương toàn phần (%)
            case "all_defense" -> AllDefense.set(item, value); // Giảm damage toàn phần (%)
            case "bow_damage" -> BowDamage.set(item, value); // Sát thương cung tên
            case "knockback_resistance" -> KnockbackResistance.set(item, value);
            case "accuracy" -> Accuracy.set(item, value); // Thêm dòng này
            case "durability" -> {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                    // 1. Lưu vào PDC (Số hiển thị trong Lore)
                    var pdc = meta.getPersistentDataContainer();
                    pdc.set(new NamespacedKey(Main.getInstance(), "durability"), PersistentDataType.DOUBLE, value);
                    pdc.set(new NamespacedKey(Main.getInstance(), "max_durability"), PersistentDataType.DOUBLE, value);

                    // 2. Set độ bền Vanilla (Minecraft mặc định)
                    damageable.setDamage(0); // 0 damage = Thanh độ bền đầy 100%
                    item.setItemMeta(damageable);
                }
            }
            case "magic_damage" -> MagicDamage.set(item, value); // Giả sử bạn đã tạo class Stat tương ứng
            case "magic_defense" -> MagicDefense.set(item, value);
            default -> {}
        }

        // 2. Lưu Slot quy định cho chỉ số này vào PDC (Ví dụ: slot_damage = "mainhand")
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey slotKey = new NamespacedKey(Main.getInstance(), "slot_" + type);
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.STRING, slot.toLowerCase());
            item.setItemMeta(meta);
        }
    }

    /**
     * Overload hàm updatePDCNumeric cũ để không lỗi các chỗ gọi khác.
     */
    public void updatePDCNumeric(ItemStack item, String type, double value) {
        updatePDCNumeric(item, type, value, "any");
    }
}