package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Expire extends BukkitRunnable {

    private static Expire instance;
    private final NamespacedKey expireKey;

    public Expire() {
        instance = this;
        this.expireKey = new NamespacedKey(Main.getInstance(), "item_expiry_time");
    }

    public static Expire getInstance() {
        return instance;
    }

    public NamespacedKey getExpireKey() {
        return this.expireKey;
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();

        
        FileConfiguration config = Main.getInstance().getExpireConfig();
        var lang = Main.getInstance().getLangManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;

            PlayerInventory inv = player.getInventory();
            boolean changed = false;

            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) continue;

                var pdc = item.getItemMeta().getPersistentDataContainer();

                if (pdc.has(expireKey, PersistentDataType.LONG)) {
                    Long expireTime = pdc.get(expireKey, PersistentDataType.LONG);

                    if (expireTime != null && currentTime >= expireTime) {
                        var meta = item.getItemMeta();
                        String itemName = meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();

                        inv.setItem(i, null);
                        changed = true;

                        player.sendMessage(lang.getMessage("expire.expired", "{item}", itemName));
                    }
                }
            }

            if (changed) {
                CacheListener.refreshCache(player);
                new StatsListener().updatePlayerStats(player);
            }
        }

        
        int seconds = config.getInt("scan-interval-seconds", 1);
        if (seconds < 1) seconds = 1; 

        long delayTicks = seconds * 20L; 

        if (Main.getInstance().isEnabled()) {
            new Expire().runTaskLater(Main.getInstance(), delayTicks);
        }
    }

    public static long parseDuration(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String input = sb.toString().toLowerCase().trim();

        long totalMs = 0;
        boolean found = false;

        Pattern pattern = Pattern.compile("(\\d+)\\s*(mo|d|h|m|s)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "mo" -> totalMs += value * 30L * 24L * 60L * 60L * 1000L;
                case "d"  -> totalMs += value * 24L * 60L * 60L * 1000L;
                case "h"  -> totalMs += value * 60L * 60L * 1000L;
                case "m"  -> totalMs += value * 60L * 1000L;
                case "s"  -> totalMs += value * 1000L;
            }
        }

        return found ? totalMs : -1;
    }
}