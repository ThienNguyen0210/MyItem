package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.ThienNguyen.Hook.MythicMobHook;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MobDrop implements Listener {
    private final Random random = new Random();

    
    private final List<String> blockedPlainPrefixes = List.of(
            "[💰 Tiền Vàng]",
            "[🎁] Vật phẩm đặc biệt",
            " "
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        FileConfiguration config = Main.getInstance().getMobDropConfig();
        if (config == null) return;

        boolean autoInv = config.getBoolean("settings.auto-inventory", false);
        boolean autoAll = config.getBoolean("settings.auto-all-drops", false);

        String mythicName = MythicMobHook.getMythicName(entity);
        String path = (mythicName != null) ? "MythicMobs." + mythicName : "Vanilla." + entity.getType().name();

        List<ItemStack> customDrops = getCustomDrops(config, path);

        if (killer != null && autoInv) {
            
            for (ItemStack item : customDrops) {
                giveItem(killer, item);
            }

            if (autoAll) {
                
                List<ItemStack> currentDrops = new ArrayList<>(event.getDrops());
                for (ItemStack item : currentDrops) {
                    if (!isTrashItem(item)) {
                        giveItem(killer, item);
                    }
                }
                event.getDrops().clear();

                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!killer.isOnline()) return;

                        
                        for (Entity nearby : entity.getNearbyEntities(1.5, 1.5, 1.5)) {
                            if (nearby instanceof Item itemEntity) {
                                
                                if (itemEntity.getTicksLived() < 2) {
                                    ItemStack stack = itemEntity.getItemStack();

                                    if (isTrashItem(stack)) {
                                        continue;
                                    }

                                    giveItem(killer, stack);
                                    itemEntity.remove();
                                }
                            }
                        }
                    }
                }.runTaskLater(Main.getInstance(), 1L);
            }
        } else {
            event.getDrops().addAll(customDrops);
        }
    }

    private boolean isTrashItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return true;

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                
                String plainName = ChatColor.stripColor(meta.getDisplayName());

                for (String prefix : blockedPlainPrefixes) {
                    if (plainName.startsWith(prefix)) return true;
                }
            }
        }

        
        String type = item.getType().name();
        boolean isArmorOrTool = type.contains("LEATHER_") || type.contains("CHAINMAIL_") ||
                type.contains("IRON_") || type.contains("GOLDEN_") ||
                type.contains("DIAMOND_");

        if (isArmorOrTool && item.getDurability() > 0) {
            return true;
        }

        return false;
    }

    private List<ItemStack> getCustomDrops(FileConfiguration config, String path) {
        List<ItemStack> items = new ArrayList<>();
        if (!config.contains(path)) return items;

        List<Map<?, ?>> drops = config.getMapList(path);
        for (Map<?, ?> drop : drops) {
            String itemId = (String) drop.get("id");
            Object chanceObj = drop.get("chance");
            if (itemId == null || chanceObj == null) continue;

            double chance = ((Number) chanceObj).doubleValue();
            if (random.nextDouble() * 100 <= chance) {
                ItemStack item = Main.getInstance().getItemDatabase().loadItem(itemId);
                if (item != null) items.add(item);
            }
        }
        return items;
    }

    private void giveItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.8f);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }
}