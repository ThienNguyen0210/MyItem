package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.ThienNguyen.Lore.StatsLore;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class DurabilityListener implements Listener {

    private static final NamespacedKey DUR_KEY = new NamespacedKey(Main.getInstance(), "durability");
    private static final NamespacedKey MAX_KEY = new NamespacedKey(Main.getInstance(), "max_durability");
    private final Random random = new Random();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDurabilityChange(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(DUR_KEY, PersistentDataType.DOUBLE)) {
            
            event.setDamage(0);

            
            int unbreakingLevel = meta.getEnchantLevel(Enchantment.DURABILITY);
            if (unbreakingLevel > 0) {
                
                
                double chance = 100.0 / (unbreakingLevel + 1);
                if (random.nextDouble() * 100 > chance) {
                    return; 
                }
            }

            double current = pdc.get(DUR_KEY, PersistentDataType.DOUBLE);
            double max = pdc.getOrDefault(MAX_KEY, PersistentDataType.DOUBLE, current);

            
            double next = current - 1;

            if (next <= 0) {
                handleBrokenItem(event.getPlayer(), item);
            } else {
                pdc.set(DUR_KEY, PersistentDataType.DOUBLE, next);
                updateVanillaBar(item, meta, next, max);

                item.setItemMeta(meta);
                StatsLore.updateLore(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMending(PlayerItemMendEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(DUR_KEY, PersistentDataType.DOUBLE)) {
            double current = pdc.get(DUR_KEY, PersistentDataType.DOUBLE);
            double max = pdc.getOrDefault(MAX_KEY, PersistentDataType.DOUBLE, current);

            
            double repairAmount = event.getRepairAmount();
            double next = Math.min(max, current + repairAmount);

            pdc.set(DUR_KEY, PersistentDataType.DOUBLE, next);
            updateVanillaBar(item, meta, next, max);

            item.setItemMeta(meta);
            StatsLore.updateLore(item);

            
            event.setRepairAmount(0);
        }
    }

    @EventHandler
    public void onAnvilRepair(PrepareAnvilEvent event) {
        ItemStack leftItem = event.getInventory().getItem(0);
        ItemStack result = event.getResult();

        if (result == null || leftItem == null || !leftItem.hasItemMeta()) return;

        PersistentDataContainer leftPdc = leftItem.getItemMeta().getPersistentDataContainer();

        if (leftPdc.has(DUR_KEY, PersistentDataType.DOUBLE)) {
            ItemMeta resultMeta = result.getItemMeta();
            PersistentDataContainer resultPdc = resultMeta.getPersistentDataContainer();

            double current = leftPdc.get(DUR_KEY, PersistentDataType.DOUBLE);
            double max = leftPdc.getOrDefault(MAX_KEY, PersistentDataType.DOUBLE, current);

            
            if (resultMeta instanceof Damageable resDamageable) {
                short vMax = result.getType().getMaxDurability();
                
                double ratio = 1.0 - ((double) resDamageable.getDamage() / vMax);
                double newDur = max * ratio;

                resultPdc.set(DUR_KEY, PersistentDataType.DOUBLE, newDur);
                resultPdc.set(MAX_KEY, PersistentDataType.DOUBLE, max);

                updateVanillaBar(result, resultMeta, newDur, max);
                result.setItemMeta(resultMeta);
                StatsLore.updateLore(result);

                event.setResult(result);
            }
        }
    }

    private void updateVanillaBar(ItemStack item, ItemMeta meta, double current, double max) {
        if (meta instanceof Damageable damageable) {
            short vMax = item.getType().getMaxDurability();
            if (vMax > 0) {
                
                int damageToSet = (int) (vMax * (1.0 - (current / max)));
                
                damageable.setDamage(Math.min(damageToSet, vMax - 1));
            }
        }
    }

    private void handleBrokenItem(Player player, ItemStack item) {
        
        item.setAmount(0);
    }
}