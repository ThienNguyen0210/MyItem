package org.ThienNguyen.Listener.Particle;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleManager {
    // Sử dụng Map để lưu người chơi và ID hiệu ứng tương ứng
    private static final Map<UUID, String> activeParticles = new HashMap<>();

    public static void loadConfig() {
        // Có thể dùng để reload hoặc thông báo nếu cần trong tương lai
    }

    public static void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeParticles.isEmpty()) return;

                activeParticles.forEach((uuid, effectId) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        spawnEffect(p, effectId);
                    }
                });
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    public static void setEffect(Player p, String effectId) {
        activeParticles.put(p.getUniqueId(), effectId);
    }

    public static void removeEffect(Player p) {
        activeParticles.remove(p.getUniqueId());
    }

    private static void spawnEffect(Player p, String id) {
        FileConfiguration config = Main.getInstance().getParticleConfig();
        String path = "effects." + id;
        if (config == null || !config.contains(path)) return;

        try {
            // --- 1. Core Options ---
            Particle particle = Particle.valueOf(config.getString(path + ".particle", "FLAME").toUpperCase());
            int amount = config.getInt(path + ".amount", 1);
            double speed = config.getDouble(path + ".speed", 0.01);
            double freq = config.getDouble(path + ".frequency", 1.0);
            double ticks = (double) Bukkit.getCurrentTick() * freq;

            // --- 2. Coordinate System với option mới ---
            Location origin = p.getLocation();
            boolean useViewDirection = config.getBoolean(path + ".use-view-direction", true);

            Vector dir;
            Vector right;
            Vector localUp;

            if (useViewDirection) {
                // Chế độ cũ: theo hướng nhìn đầy đủ (yaw + pitch)
                dir = origin.getDirection().normalize();
                Vector worldUp = new Vector(0, 1, 0);
                right = dir.clone().crossProduct(worldUp).normalize();
                localUp = right.clone().crossProduct(dir).normalize();
            } else {
                // Chế độ mới: fixed quanh người (chỉ xoay theo yaw, up thẳng đứng)
                double yawRad = Math.toRadians(origin.getYaw() + 90); // +90 để forward khớp hướng đứng
                dir = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize(); // forward theo yaw
                localUp = new Vector(0, 1, 0); // luôn lên theo world
                right = dir.clone().crossProduct(localUp).normalize(); // right vuông góc
            }

            // Tính tâm dựa trên Offset
            Location center = origin.clone()
                    .add(dir.multiply(     config.getDouble(path + ".forward-offset", 0.0)))
                    .add(localUp.multiply( config.getDouble(path + ".upward-offset", 1.0)))
                    .add(right.multiply(   config.getDouble(path + ".right-offset", 0.0)));

            // --- 3. Arrangement Logic ---
            String arrangement = config.getString(path + ".arrangement", "SINGLE").toUpperCase();
            double radius = config.getDouble(path + ".radius", 1.0);
            int points = config.getInt(path + ".points", 1);

            switch (arrangement) {
                case "ATOM": // Quỹ đạo nguyên tử xoay quanh người
                    for (int i = 0; i < 3; i++) { // 3 quỹ đạo
                        double angle = (ticks * 0.2) + (i * Math.PI / 3);
                        double x = Math.cos(angle) * radius;
                        double y = Math.sin(angle) * radius * Math.cos(angle + i);
                        double z = Math.sin(angle) * radius * Math.sin(angle + i);
                        p.getWorld().spawnParticle(particle, center.clone().add(x, y, z), 1, 0, 0, 0, speed);
                    }
                    break;

                case "WINGS": // Hình cánh đối xứng
                    double wingTicks = Math.sin(ticks * 0.1) * 30; // Cánh đập
                    for (double i = 0.1; i <= radius; i += 0.1) {
                        for (int side_val : new int[]{-1, 1}) {
                            double angle = Math.toRadians(side_val * (45 + wingTicks));
                            Vector v = right.clone().multiply(Math.cos(angle) * i)
                                    .add(localUp.clone().multiply(Math.sin(angle) * i));
                            p.getWorld().spawnParticle(particle, center.clone().add(v), 1, 0, 0, 0, speed);
                        }
                    }
                    break;

                case "POLYGON": // Đa giác
                    int edges = config.getInt(path + ".edges", 3);
                    for (int i = 0; i < edges; i++) {
                        double angle1 = Math.toRadians(i * (360.0 / edges) + ticks);
                        double angle2 = Math.toRadians((i + 1) * (360.0 / edges) + ticks);

                        Location p1 = center.clone().add(Math.cos(angle1) * radius, 0, Math.sin(angle1) * radius);
                        Location p2 = center.clone().add(Math.cos(angle2) * radius, 0, Math.sin(angle2) * radius);

                        Vector link = p2.toVector().subtract(p1.toVector());
                        double distance = p1.distance(p2);
                        for (double d = 0; d < distance; d += 0.2) {
                            p.getWorld().spawnParticle(particle, p1.clone().add(link.clone().normalize().multiply(d)), 1, 0, 0, 0, speed);
                        }
                    }
                    break;

                case "SPIRAL": // Xoắn ốc
                    for (int i = 0; i < points; i++) {
                        double angle = (ticks * 0.5) + (i * (Math.PI * 2 / points));
                        double y = (ticks * 0.05 + (i * 0.1)) % radius;
                        double r = (y / radius) * radius;
                        p.getWorld().spawnParticle(particle, center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r), 1, 0, 0, 0, speed);
                    }
                    break;

                default:
                    handleLegacyShapes(p, center, arrangement, particle, amount, radius, ticks, speed, config, path);
                    break;
            }
        } catch (Exception ignored) {
            // Nên log lỗi trong dev: ignored.printStackTrace();
        }
    }

    private static void handleLegacyShapes(Player p, Location center, String shape, Particle particle, int amount, double radius, double ticks, double speed, FileConfiguration config, String path) {
        if (shape.equals("CIRCLE")) {
            for (int i = 0; i < 360; i += 360 / Math.max(1, config.getInt(path + ".points", 20))) {
                double rad = Math.toRadians(i + ticks);
                p.getWorld().spawnParticle(particle, center.clone().add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius), 1, 0, 0, 0, speed);
            }
        } else if (shape.equals("SPHERE")) {
            int pts = config.getInt(path + ".points", 15);
            for (int i = 0; i < pts; i++) {
                double phi = Math.acos(1 - 2 * (i + 0.5) / pts);
                double theta = Math.PI * (1 + Math.sqrt(5)) * (i + 0.5);
                p.getWorld().spawnParticle(particle, center.clone().add(
                        radius * Math.cos(theta) * Math.sin(phi),
                        radius * Math.sin(theta) * Math.sin(phi),
                        radius * Math.cos(phi)
                ), 1, 0, 0, 0, speed);
            }
        } else {
            // SINGLE hoặc legacy khác
            p.getWorld().spawnParticle(particle, center, amount,
                    config.getDouble(path + ".dx", 0),
                    config.getDouble(path + ".dy", 0),
                    config.getDouble(path + ".dz", 0), speed);
        }
    }
}