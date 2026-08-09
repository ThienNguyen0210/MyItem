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
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TextDisplayManager {

    private static final Random random = new Random();
    private static final java.util.regex.Pattern HEX_PATTERN =
            java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");

    // ─── Global runnable ────────────────────────────────────────────────────
    /**
     * Tất cả TextDisplay đang active được quản lý trong 1 danh sách duy nhất.
     * 1 BukkitRunnable duy nhất chạy mỗi tick xử lý toàn bộ — không spawn N runnable riêng.
     */
    private static final List<ActiveDisplay> ACTIVE = new ArrayList<>();
    private static boolean schedulerRunning = false;

    private static void ensureScheduler() {
        if (schedulerRunning) return;
        schedulerRunning = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ACTIVE.isEmpty()) return;
                ACTIVE.removeIf(ActiveDisplay::tick);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // ─── ActiveDisplay wrapper ───────────────────────────────────────────────
    private static class ActiveDisplay {
        final TextDisplay entity;
        final double targetScale;
        final int appearTicks;
        final int stayTicks;
        final int fadeTicks;
        final int totalTicks;
        // physics (null = no physics)
        Vector velocity;
        final double gravity;
        final double drag;
        int tick = 0;

        ActiveDisplay(TextDisplay entity, double targetScale,
                      int appearTicks, int stayTicks, int fadeTicks,
                      Vector velocity, double gravity, double drag) {
            this.entity      = entity;
            this.targetScale = targetScale;
            this.appearTicks = appearTicks;
            this.stayTicks   = stayTicks;
            this.fadeTicks   = fadeTicks;
            this.totalTicks  = appearTicks + stayTicks + fadeTicks;
            this.velocity    = velocity;
            this.gravity     = gravity;
            this.drag        = drag;
        }

        /**
         * @return true nếu đã xong (cần xoá khỏi list)
         */
        boolean tick() {
            if (!entity.isValid() || tick >= totalTicks) {
                entity.remove();
                return true;
            }

            // Physics: teleport chỉ khi có velocity
            if (velocity != null) {
                Location cur = entity.getLocation();
                cur.add(velocity);
                entity.teleport(cur);
                velocity.setY(velocity.getY() - gravity);
                velocity.multiply(drag);
            }

            // Fade out: scale nhỏ dần trong fadeTicks cuối
            if (tick >= appearTicks + stayTicks) {
                float progress = (float)(totalTicks - tick) / fadeTicks;
                float s = (float)(targetScale * progress);
                Transformation t = entity.getTransformation();
                t.getScale().set(s, s, s);
                entity.setTransformation(t);
            }

            tick++;
            return false;
        }
    }

    // ─── Colorize ────────────────────────────────────────────────────────────
    private static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        java.util.regex.Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String g = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + g.charAt(0) + "§" + g.charAt(1)
                    + "§" + g.charAt(2) + "§" + g.charAt(3)
                    + "§" + g.charAt(4) + "§" + g.charAt(5));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    // ─── Public entry point ──────────────────────────────────────────────────
    public static void displayAll(LivingEntity victim) {
        FileConfiguration config = Main.getInstance().getCustomConfig();
        if (config == null || !config.getBoolean("text-display.enabled", true)) return;

        ensureScheduler();

        double yBase   = config.getDouble("text-display.offset.y-base", 0.8);
        Location base  = victim.getLocation().add(0, victim.getHeight() + yBase, 0);

        handleSpecialStatus(victim, base, config);
        handleNormalDamage(victim, base, config);
        handleTrueDamage(victim, base, config);
        handleMultiElementDamage(victim, config);

        cleanupMetadata(victim);
    }

    // ─── Handlers ────────────────────────────────────────────────────────────
    private static void handleSpecialStatus(LivingEntity victim, Location baseLoc, FileConfiguration config) {
        String special = getMetaString(victim, "DISPLAY_SPECIAL_STATUS");
        if (special == null || special.isEmpty()) return;

        String key    = special.toLowerCase();
        String format = config.getString("text-display.format." + key, "&f" + special);
        format        = ChatColor.translateAlternateColorCodes('&', format);

        double scale  = config.getDouble("text-display.scale." + key, 1.4);
        double sideX  = config.getDouble("text-display.offset.side-x", 0.7);
        double xOff   = special.equalsIgnoreCase("DODGE") ? sideX : -sideX;

        spawnText(baseLoc.clone().add(xOff, 0.3, 0), format, scale, config, true);
    }

    private static void handleNormalDamage(LivingEntity victim, Location baseLoc, FileConfiguration config) {
        if (!victim.hasMetadata("DISPLAY_NORMAL_DAMAGE")) return;
        double dmg = getMetaDouble(victim, "DISPLAY_NORMAL_DAMAGE");
        if (dmg <= 0) return;

        boolean crit  = victim.hasMetadata("LAST_HIT_CRIT");
        String key    = crit ? "critical" : "normal";
        String format = colorize(config.getString("text-display.format." + key,
                crit ? "&e&l✦ {value} ✦" : "&f{value}"));
        double scale  = config.getDouble("text-display.scale." + key, crit ? 1.6 : 1.1);
        String text   = format.replace("{value}", String.format("%.1f", dmg));

        spawnText(baseLoc, text, scale, config, true);
    }

    private static void handleTrueDamage(LivingEntity victim, Location baseLoc, FileConfiguration config) {
        if (!victim.hasMetadata("DISPLAY_TRUE_DAMAGE")) return;
        double dmg = getMetaDouble(victim, "DISPLAY_TRUE_DAMAGE");
        if (dmg <= 0) return;

        double extraY = config.getDouble("text-display.offset.true-dmg-extra-y", 0.8);
        String format = colorize(config.getString("text-display.format.true-damage", "&d&l᎕ {value}"));
        double scale  = config.getDouble("text-display.scale.true-damage", 1.4);
        String text   = format.replace("{value}", String.format("%.1f", dmg));

        spawnText(baseLoc.clone().add(0, extraY, 0), text, scale, config, true);
    }

    private static void handleMultiElementDamage(LivingEntity victim, FileConfiguration config) {
        if (!config.getBoolean("text-display.multi-element.enabled", true)) return;

        String elementData = getMetaString(victim, "DISPLAY_ELEMENTS_DATA");
        if (elementData == null || elementData.isBlank()) return;

        String[]  entries = elementData.split(",");
        Location  foot    = victim.getLocation();
        boolean   merge   = config.getBoolean("text-display.multi-element.merge", false);

        if (merge) {
            StringBuilder elementsPart = new StringBuilder();
            double totalDamage = 0;
            boolean hasValid   = false;

            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length < 3) continue;
                String elementId = parts[0].trim().toUpperCase();
                String dmgStr    = parts[2].trim();
                double dmgValue;
                try { dmgValue = Double.parseDouble(dmgStr); } catch (Exception e) { continue; }
                if (dmgValue <= 0) continue;
                totalDamage += dmgValue;
                hasValid = true;

                String format = config.getString("text-display.format." + elementId,
                        config.getString("text-display.elements." + elementId, "&f✨ {value}"));
                String elemText = colorize(format.replace("{value}", dmgStr));

                if (!elementsPart.isEmpty()) {
                    elementsPart.append(colorize(config.getString(
                            "text-display.multi-element.merge-separator", " &8• ")));
                }
                elementsPart.append(elemText);
            }
            if (!hasValid) return;

            String mergeFormat = config.getString("text-display.multi-element.merge-format", "{elements} &7-{total}");
            String finalText   = colorize(mergeFormat
                    .replace("{elements}", elementsPart.toString())
                    .replace("{total}", String.format("%.1f", totalDamage)));

            double scale  = config.getDouble("text-display.scale.merged-elements", 1.55);
            double yOff   = config.getDouble("text-display.multi-element.merge-y", 2.1);
            Location loc  = foot.clone().add(0, yOff, 0);
            if (!config.getBoolean("text-display.multi-element.merge-center-xz", true)) {
                loc.add((random.nextDouble() - 0.5) * 0.5, 0, (random.nextDouble() - 0.5) * 0.5);
            }
            spawnText(loc, finalText, scale, config, true);

        } else {
            double sphereR = config.getDouble("text-display.offset.element-sphere-radius", 1.6);
            double minY    = config.getDouble("text-display.offset.element-min-y", 1.0);
            double maxY    = config.getDouble("text-display.offset.element-max-y", 2.8);
            double scale   = config.getDouble("text-display.scale.element", 1.7);

            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length < 3) continue;
                String elementId = parts[0].trim().toUpperCase();
                String valueStr  = parts[2].trim();
                try { if (Double.parseDouble(valueStr) <= 0) continue; }
                catch (Exception e) { continue; }

                String format = config.getString("text-display.format." + elementId,
                        config.getString("text-display.elements." + elementId, "&f✨ {value}"));
                String finalText = colorize(format.replace("{value}", valueStr));

                double theta = random.nextDouble() * 2 * Math.PI;
                double phi   = Math.acos(2 * random.nextDouble() - 1);
                double x     = sphereR * Math.sin(phi) * Math.cos(theta);
                double z     = sphereR * Math.sin(phi) * Math.sin(theta);
                double y     = minY + random.nextDouble() * (maxY - minY);

                spawnText(foot.clone().add(x, y, z), finalText, scale, config, true);
            }
        }
    }

    // ─── Spawn + register (KHÔNG tạo BukkitRunnable riêng nữa) ─────────────
    private static void spawnText(Location loc, String text, double targetScale,
                                  FileConfiguration config, boolean usePhysics) {
        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        display.setText(text);
        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowed(true);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

        // Đặt scale ban đầu
        Transformation trans = display.getTransformation();
        trans.getScale().set((float) targetScale, (float) targetScale, (float) targetScale);
        display.setTransformation(trans);

        int appearTicks = config.getInt("text-display.animation.appearance-ticks", 5);
        int stayTicks   = config.getInt("text-display.animation.stay-ticks", 12);
        int fadeTicks   = config.getInt("text-display.animation.fade-ticks", 14);

        Vector vel = null;
        double gravity = 0, drag = 1;

        if (usePhysics) {
            double sideSpread = config.getDouble("text-display.multi-element.physics.side-spread", 0.15);
            double upBase     = config.getDouble("text-display.multi-element.physics.up-velocity-base", 0.22);
            double upRandom   = config.getDouble("text-display.multi-element.physics.up-velocity-random", 0.08);
            gravity           = config.getDouble("text-display.multi-element.physics.gravity", 0.012);
            drag              = config.getDouble("text-display.multi-element.physics.air-drag", 0.965);

            vel = new Vector(
                    (random.nextDouble() - 0.5) * sideSpread,
                    upBase + random.nextDouble() * upRandom,
                    (random.nextDouble() - 0.5) * sideSpread
            );
        }

        // Đăng ký vào global list — KHÔNG tạo runnable riêng
        ACTIVE.add(new ActiveDisplay(display, targetScale,
                appearTicks, stayTicks, fadeTicks, vel, gravity, drag));
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────
    private static void cleanupMetadata(LivingEntity victim) {
        for (String key : Arrays.asList(
                "DISPLAY_SPECIAL_STATUS", "DISPLAY_NORMAL_DAMAGE", "DISPLAY_TRUE_DAMAGE",
                "LAST_HIT_CRIT", "DISPLAY_ELEMENTS_DATA", "SPECIAL_STATUS_PROCESSED")) {
            victim.removeMetadata(key, Main.getInstance());
        }
    }

    private static String getMetaString(LivingEntity e, String key) {
        return e.hasMetadata(key) ? e.getMetadata(key).get(0).asString() : null;
    }

    private static double getMetaDouble(LivingEntity e, String key) {
        return e.hasMetadata(key) ? e.getMetadata(key).get(0).asDouble() : 0;
    }
}