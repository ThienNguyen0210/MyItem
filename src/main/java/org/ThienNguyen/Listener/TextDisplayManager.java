package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TextDisplayManager {

    private static final Random random = new Random();
    // THÊM field static ở đầu class:
    private static final java.util.regex.Pattern HEX_PATTERN = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");

    // SỬA hàm colorize:
    private static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";

        java.util.regex.Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "§x"
                    + "§" + group.charAt(0) + "§" + group.charAt(1)
                    + "§" + group.charAt(2) + "§" + group.charAt(3)
                    + "§" + group.charAt(4) + "§" + group.charAt(5));
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
        public static void displayAll(LivingEntity victim) {
            FileConfiguration config = Main.getInstance().getCustomConfig();
            if (config == null || !config.getBoolean("text-display.enabled", true)) {
                return;
            }

            double yBase = config.getDouble("text-display.offset.y-base", 0.8);
            Location baseLoc = victim.getLocation().add(0, victim.getHeight() + yBase, 0);

            handleSpecialStatus(victim, baseLoc, config);
            handleNormalDamage(victim, baseLoc, config);
            handleTrueDamage(victim, baseLoc, config);
            handleMultiElementDamage(victim, config);

            cleanupMetadata(victim);
        }

    private static void handleMultiElementDamage(LivingEntity victim, FileConfiguration config) {
        if (!config.getBoolean("text-display.multi-element.enabled", true)) {
            return;
        }

        String elementData = getMetaString(victim, "DISPLAY_ELEMENTS_DATA");
        if (elementData == null || elementData.trim().isEmpty()) {
            return;
        }

        String[] entries = elementData.split(",");
        Location footLoc = victim.getLocation();
        boolean shouldMerge = config.getBoolean("text-display.multi-element.merge", false);

        if (shouldMerge) {
            StringBuilder elementsPart = new StringBuilder();
            double totalDamage = 0.0;
            boolean hasValidElement = false;

            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length < 3) continue;

                String elementId = parts[0].trim().toUpperCase();
                String dmgStr = parts[2].trim();

                try {
                    double dmgValue = Double.parseDouble(dmgStr);
                    if (dmgValue <= 0) continue;
                    totalDamage += dmgValue;
                } catch (NumberFormatException e) {
                    continue;
                }

                hasValidElement = true;

                String format = config.getString("text-display.format." + elementId);
                if (format == null) {
                    format = config.getString("text-display.elements." + elementId, "&f✨ {value}");
                }

                // Sử dụng colorize cho từng element format
                String elemText = colorize(format.replace("{value}", dmgStr));

                if (elementsPart.length() > 0) {
                    String separator = config.getString("text-display.multi-element.merge-separator", " &8• ");
                    // Sử dụng colorize cho dấu phân cách
                    elementsPart.append(colorize(separator));
                }
                elementsPart.append(elemText);
            }

            if (!hasValidElement) return;

            String mergeFormat = config.getString("text-display.multi-element.merge-format", "{elements} &7-{total}");

            // Finalize text với hỗ trợ Hex
            String finalText = colorize(mergeFormat.replace("{elements}", elementsPart.toString())
                    .replace("{total}", String.format("%.1f", totalDamage)));

            double scale = config.getDouble("text-display.scale.merged-elements", 1.55);
            double yOffset = config.getDouble("text-display.multi-element.merge-y", 2.1);

            Location spawnLoc = footLoc.clone().add(0, yOffset, 0);

            if (!config.getBoolean("text-display.multi-element.merge-center-xz", true)) {
                spawnLoc.add((random.nextDouble() - 0.5) * 0.5, 0, (random.nextDouble() - 0.5) * 0.5);
            }

            spawnText(spawnLoc, finalText, scale, config, true);

        } else {
            double sphereRadius = config.getDouble("text-display.offset.element-sphere-radius", 1.6);
            double minY = config.getDouble("text-display.offset.element-min-y", 1.0);
            double maxY = config.getDouble("text-display.offset.element-max-y", 2.8);
            double scale = config.getDouble("text-display.scale.element", 1.7);

            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length < 3) continue;

                String elementId = parts[0].trim().toUpperCase();
                String valueStr = parts[2].trim();

                try {
                    if (Double.parseDouble(valueStr) <= 0) continue;
                } catch (Exception e) { continue; }

                String format = config.getString("text-display.format." + elementId);
                if (format == null) {
                    format = config.getString("text-display.elements." + elementId, "&f✨ {value}");
                }

                // Sử dụng colorize ở đây
                String finalText = colorize(format.replace("{value}", valueStr));

                double theta = random.nextDouble() * 2 * Math.PI;
                double phi = Math.acos(2 * random.nextDouble() - 1);

                double x = sphereRadius * Math.sin(phi) * Math.cos(theta);
                double z = sphereRadius * Math.sin(phi) * Math.sin(theta);
                double y = minY + (random.nextDouble() * (maxY - minY));

                Location spawnLoc = footLoc.clone().add(x, y, z);
                spawnText(spawnLoc, finalText, scale, config, true);
            }
        }
    }
    private static void handleSpecialStatus(LivingEntity victim, Location baseLoc, FileConfiguration config) {
        
        String special = getMetaString(victim, "DISPLAY_SPECIAL_STATUS");
        if (special == null || special.isEmpty()) return;

        
        String key = special.toLowerCase();

        
        String format = config.getString("text-display.format." + key);
        if (format == null) {
            
            format = "&f" + special;
        }
        format = ChatColor.translateAlternateColorCodes('&', format);

        
        double scale = config.getDouble("text-display.scale." + key, 1.4);

        
        double sideX = config.getDouble("text-display.offset.side-x", 0.7);
        double xOffset = special.equalsIgnoreCase("DODGE") ? sideX : -sideX;

        Location sideLoc = baseLoc.clone().add(xOffset, 0.3, 0);

        
        spawnText(sideLoc, format, scale, config, true);
    }

    private static void handleNormalDamage(LivingEntity victim, Location baseLoc, FileConfiguration config) {
        if (!victim.hasMetadata("DISPLAY_NORMAL_DAMAGE")) return;
        double dmg = getMetaDouble(victim, "DISPLAY_NORMAL_DAMAGE");
        if (dmg <= 0) return;

        boolean crit = victim.hasMetadata("LAST_HIT_CRIT");
        String key = crit ? "critical" : "normal";
        String format = config.getString("text-display.format." + key, crit ? "&e&l✦ {value} ✦" : "&f{value}");

        // Thay thế ChatColor bằng hàm colorize hỗ trợ Hex
        format = colorize(format);

        double scale = config.getDouble("text-display.scale." + key, crit ? 1.6 : 1.1);

        String text = format.replace("{value}", String.format("%.1f", dmg));
        spawnText(baseLoc, text, scale, config, true);
    }

    private static void handleTrueDamage(LivingEntity victim, Location baseLoc, FileConfiguration config) {
        if (!victim.hasMetadata("DISPLAY_TRUE_DAMAGE")) return;
        double dmg = getMetaDouble(victim, "DISPLAY_TRUE_DAMAGE");
        if (dmg <= 0) return;

        double extraY = config.getDouble("text-display.offset.true-dmg-extra-y", 0.8);
        String format = config.getString("text-display.format.true-damage", "&d&l᎕ {value}");

        // Đã chuyển sang sử dụng colorize để hỗ trợ mã màu Hex (&#RRGGBB)
        format = colorize(format);

        double scale = config.getDouble("text-display.scale.true-damage", 1.4);

        Location loc = baseLoc.clone().add(0, extraY, 0);
        String text = format.replace("{value}", String.format("%.1f", dmg));
        spawnText(loc, text, scale, config, true);
    }

    private static void spawnText(Location loc, String text, double targetScale, FileConfiguration config, boolean usePhysics) {
        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);

        display.setText(text);
        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowed(true);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

        Transformation trans = display.getTransformation();
        trans.getScale().set((float) targetScale, (float) targetScale, (float) targetScale);
        display.setTransformation(trans);

        int appearTicks = config.getInt("text-display.animation.appearance-ticks", 5);
        int stayTicks   = config.getInt("text-display.animation.stay-ticks", 12);
        int fadeTicks   = config.getInt("text-display.animation.fade-ticks", 14);
        int totalTicks  = appearTicks + stayTicks + fadeTicks;

        if (!usePhysics) {
            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (tick >= totalTicks || !display.isValid()) {
                        display.remove();
                        cancel();
                        return;
                    }

                    if (tick >= appearTicks + stayTicks) {
                        float progress = (float) (totalTicks - tick) / fadeTicks;
                        Transformation t = display.getTransformation();
                        float scale = (float) (targetScale * progress);
                        t.getScale().set(scale, scale, scale);
                        display.setTransformation(t);
                    }

                    tick++;
                }
            }.runTaskTimer(Main.getInstance(), 0L, 1L);
            return;
        }

        
        Vector velocity = new Vector(
                (random.nextDouble() - 0.5) * config.getDouble("text-display.multi-element.physics.side-spread", 0.15),
                config.getDouble("text-display.multi-element.physics.up-velocity-base", 0.22)
                        + random.nextDouble() * config.getDouble("text-display.multi-element.physics.up-velocity-random", 0.08),
                (random.nextDouble() - 0.5) * config.getDouble("text-display.multi-element.physics.side-spread", 0.15)
        );

        final double gravity = config.getDouble("text-display.multi-element.physics.gravity", 0.012);
        final double drag    = config.getDouble("text-display.multi-element.physics.air-drag", 0.965);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!display.isValid() || tick >= totalTicks) {
                    display.remove();
                    cancel();
                    return;
                }

                Location current = display.getLocation();
                current.add(velocity);
                display.teleport(current);

                velocity.setY(velocity.getY() - gravity);
                velocity.multiply(drag);

                if (tick >= appearTicks + stayTicks) {
                    float progress = (float) (totalTicks - tick) / fadeTicks;
                    Transformation t = display.getTransformation();
                    float scaleNow = (float) (targetScale * progress);
                    t.getScale().set(scaleNow, scaleNow, scaleNow);
                    display.setTransformation(t);
                }

                tick++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private static void cleanupMetadata(LivingEntity victim) {
        List<String> keys = Arrays.asList(
                "DISPLAY_SPECIAL_STATUS",
                "DISPLAY_NORMAL_DAMAGE",
                "DISPLAY_TRUE_DAMAGE",
                "LAST_HIT_CRIT",
                "DISPLAY_ELEMENTS_DATA",
                "SPECIAL_STATUS_PROCESSED"
        );
        for (String key : keys) {
            victim.removeMetadata(key, Main.getInstance());
        }
    }

    private static String getMetaString(LivingEntity entity, String key) {
        if (!entity.hasMetadata(key)) return null;
        return entity.getMetadata(key).get(0).asString();
    }

    private static double getMetaDouble(LivingEntity entity, String key) {
        if (!entity.hasMetadata(key)) return 0;
        return entity.getMetadata(key).get(0).asDouble();
    }
}