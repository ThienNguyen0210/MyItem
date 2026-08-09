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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TextDisplayManager {

    private static final Random random = new Random();
    private static final java.util.regex.Pattern HEX_PATTERN =
            java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");

    
    
    private static final List<ActiveDisplay> ACTIVE = new ArrayList<>();
    private static boolean schedulerRunning = false;

    
    
    private static final Map<UUID, DisplayData> DISPLAY_META = new HashMap<>();

    private static class DisplayData {
        String specialStatus;
        Double normalDamage;
        Double trueDamage;
        Double magicDamage;
        String elementsData;
        boolean lastHitCrit;
        boolean pending;
        
        
        
        
        
        
        Double healthBeforeHit;
    }

    private static DisplayData data(LivingEntity e) {
        return DISPLAY_META.computeIfAbsent(e.getUniqueId(), k -> new DisplayData());
    }

    private static DisplayData peek(LivingEntity e) {
        return DISPLAY_META.get(e.getUniqueId());
    }

    static void setSpecialStatus(LivingEntity e, String status) { data(e).specialStatus = status; }
    static void setNormalDamage(LivingEntity e, double v)       { data(e).normalDamage = v; }
    static void setTrueDamage(LivingEntity e, double v)         { data(e).trueDamage = v; }
    static void setMagicDamage(LivingEntity e, double v)        { data(e).magicDamage = v; }
    static void setElementsData(LivingEntity e, String v)       { data(e).elementsData = v; }
    static void setLastHitCrit(LivingEntity e)                  { data(e).lastHitCrit = true; }
    static void setPending(LivingEntity e)                      { data(e).pending = true; }
    static void clearPending(LivingEntity e) {
        DisplayData d = peek(e);
        if (d != null) d.pending = false;
    }

    
    static void setHealthBeforeHit(LivingEntity e, double hp)   { data(e).healthBeforeHit = hp; }
    static Double getHealthBeforeHit(LivingEntity e) { DisplayData d = peek(e); return d != null ? d.healthBeforeHit : null; }

    
    static void scaleDisplayToActualDamage(LivingEntity e, double actualDamage) {
        DisplayData d = peek(e);
        if (d == null) return;

        double normal = d.normalDamage != null ? d.normalDamage : 0;
        double trueDmg = d.trueDamage != null ? d.trueDamage : 0;
        double magic = d.magicDamage != null ? d.magicDamage : 0;
        double theoreticalSum = normal + trueDmg + magic;

        if (actualDamage <= 0) {
            d.normalDamage = 0.0;
            d.trueDamage = 0.0;
            d.magicDamage = 0.0;
            return;
        }
        if (theoreticalSum <= 0) {
            
            
            d.normalDamage = actualDamage;
            return;
        }

        double scale = actualDamage / theoreticalSum;
        d.normalDamage = normal * scale;
        d.trueDamage = trueDmg * scale;
        d.magicDamage = magic * scale;
    }

    static boolean hasNormalDamage(LivingEntity e) { DisplayData d = peek(e); return d != null && d.normalDamage != null; }
    static boolean hasTrueDamage(LivingEntity e)   { DisplayData d = peek(e); return d != null && d.trueDamage != null; }
    static boolean hasMagicDamage(LivingEntity e)  { DisplayData d = peek(e); return d != null && d.magicDamage != null; }
    static boolean isPending(LivingEntity e)       { DisplayData d = peek(e); return d != null && d.pending; }
    static boolean isLastHitCrit(LivingEntity e)   { DisplayData d = peek(e); return d != null && d.lastHitCrit; }

    static double getNormalDamage(LivingEntity e) { DisplayData d = peek(e); return (d != null && d.normalDamage != null) ? d.normalDamage : 0; }
    static double getTrueDamage(LivingEntity e)   { DisplayData d = peek(e); return (d != null && d.trueDamage != null) ? d.trueDamage : 0; }
    static double getMagicDamage(LivingEntity e)  { DisplayData d = peek(e); return (d != null && d.magicDamage != null) ? d.magicDamage : 0; }
    static String getSpecialStatus(LivingEntity e) { DisplayData d = peek(e); return d != null ? d.specialStatus : null; }
    static String getElementsData(LivingEntity e)  { DisplayData d = peek(e); return d != null ? d.elementsData : null; }

    
    static void clearDisplayData(LivingEntity e) { DISPLAY_META.remove(e.getUniqueId()); }

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

    
    private static class ActiveDisplay {
        final TextDisplay entity;
        final double targetScale;
        final int appearTicks;
        final int stayTicks;
        final int fadeTicks;
        final int totalTicks;
        
        Vector velocity;
        final double gravity;
        final double drag;
        int tick = 0;

        
        
        
        
        
        
        
        final Vector pendingMove = new Vector();
        final int moveIntervalTicks;
        int ticksSincePositionFlush = 0;

        
        
        boolean fadeStarted = false;

        ActiveDisplay(TextDisplay entity, double targetScale,
                      int appearTicks, int stayTicks, int fadeTicks,
                      Vector velocity, double gravity, double drag, int moveIntervalTicks) {
            this.entity      = entity;
            this.targetScale = targetScale;
            this.appearTicks = appearTicks;
            this.stayTicks   = stayTicks;
            this.fadeTicks   = fadeTicks;
            this.totalTicks  = appearTicks + stayTicks + fadeTicks;
            this.velocity    = velocity;
            this.gravity     = gravity;
            this.drag        = drag;
            this.moveIntervalTicks = Math.max(1, moveIntervalTicks);
        }

        
        boolean tick() {
            if (!entity.isValid() || tick >= totalTicks) {
                
                
                if (velocity != null && (pendingMove.getX() != 0 || pendingMove.getY() != 0 || pendingMove.getZ() != 0)) {
                    entity.teleport(entity.getLocation().add(pendingMove));
                }
                entity.remove();
                return true;
            }

            
            if (velocity != null) {
                pendingMove.add(velocity);
                velocity.setY(velocity.getY() - gravity);
                velocity.multiply(drag);

                ticksSincePositionFlush++;
                if (ticksSincePositionFlush >= moveIntervalTicks) {
                    entity.teleport(entity.getLocation().add(pendingMove));
                    pendingMove.setX(0).setY(0).setZ(0);
                    ticksSincePositionFlush = 0;
                }
            }

            
            
            
            if (!fadeStarted && tick >= appearTicks + stayTicks) {
                fadeStarted = true;
                Transformation t = entity.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                entity.setTransformation(t);
            }

            tick++;
            return false;
        }
    }

    
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

    
    private static void handleSpecialStatus(LivingEntity victim, Location baseLoc, FileConfiguration config) {
        String special = getSpecialStatus(victim);
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
        if (!hasNormalDamage(victim)) return;
        double dmg = getNormalDamage(victim);
        if (dmg <= 0) return;

        boolean crit  = isLastHitCrit(victim);
        String key    = crit ? "critical" : "normal";
        String format = colorize(config.getString("text-display.format." + key,
                crit ? "&e&l✦ {value} ✦" : "&f{value}"));
        double scale  = config.getDouble("text-display.scale." + key, crit ? 1.6 : 1.1);
        String text   = format.replace("{value}", String.format("%.1f", dmg));

        spawnText(baseLoc, text, scale, config, true);
    }

    private static void handleTrueDamage(LivingEntity victim, Location baseLoc, FileConfiguration config) {
        if (!hasTrueDamage(victim)) return;
        double dmg = getTrueDamage(victim);
        if (dmg <= 0) return;

        double extraY = config.getDouble("text-display.offset.true-dmg-extra-y", 0.8);
        String format = colorize(config.getString("text-display.format.true-damage", "&d&l᎕ {value}"));
        double scale  = config.getDouble("text-display.scale.true-damage", 1.4);
        String text   = format.replace("{value}", String.format("%.1f", dmg));

        spawnText(baseLoc.clone().add(0, extraY, 0), text, scale, config, true);
    }

    private static void handleMultiElementDamage(LivingEntity victim, FileConfiguration config) {
        if (!config.getBoolean("text-display.multi-element.enabled", true)) return;

        String elementData = getElementsData(victim);
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

    
    private static void spawnText(Location loc, String text, double targetScale,
                                  FileConfiguration config, boolean usePhysics) {
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

        
        
        
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(Math.max(1, fadeTicks));

        Vector vel = null;
        double gravity = 0, drag = 1;
        int moveIntervalTicks = Math.max(1, config.getInt("text-display.multi-element.physics.move-interval-ticks", 3));

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

            
            
            
            try {
                display.setTeleportDuration(moveIntervalTicks);
            } catch (NoSuchMethodError ignored) {
                
                
                
                
            }
        }

        
        ACTIVE.add(new ActiveDisplay(display, targetScale,
                appearTicks, stayTicks, fadeTicks, vel, gravity, drag, moveIntervalTicks));
    }

    
    private static void cleanupMetadata(LivingEntity victim) {
        
        
        
        clearDisplayData(victim);

        
        
        
        victim.removeMetadata("SPECIAL_STATUS_PROCESSED", Main.getInstance());
    }
}