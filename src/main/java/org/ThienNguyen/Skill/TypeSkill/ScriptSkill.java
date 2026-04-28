package org.ThienNguyen.Skill.TypeSkill;

import bsh.Interpreter;
import org.ThienNguyen.Main;
import org.ThienNguyen.Skill.ISkill;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class ScriptSkill implements ISkill {
    private final String name;
    private final String trigger;
    private final String script;

    public ScriptSkill(String name, String trigger, String script) {
        this.name = name;
        this.trigger = trigger;
        this.script = script;
    }

    @Override public String getName() { return name; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return trigger; }

    @Override
    public void execute(Player player, LivingEntity target, int level, double baseDamage) {
        if (script == null || script.isEmpty()) return;

        try {
            Interpreter bsh = new Interpreter();
            bsh.set("player", player);
            bsh.set("target", target);
            bsh.set("level", level);
            bsh.set("baseDamage", baseDamage);
            bsh.set("api", this);
            // Truyền thêm instance plugin để dùng trong Script nếu cần (ví dụ lấy metadata)
            bsh.set("plugin", Main.getInstance());

            bsh.eval(script);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[MyItem] Lỗi thực thi script skill '" + name + "': " + e.getMessage());
            e.printStackTrace(); // Nên in ra để dễ debug lỗi cú pháp trong YML
        }
    }

    // --- CÁC HÀM HỖ TRỢ (API cho User gọi trong YML) ---

    // 1. Hàm Delay cực kỳ quan trọng (Sửa lỗi Method Not Found)
    public void delay(int ticks, Runnable r) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), r, (long) ticks);
    }

    // 2. Hàm gây sát thương chuẩn
    public void damage(Player source, LivingEntity victim, double amount) {
        if (victim == null || victim.isDead()) return;

        // 1. Dán nhãn giá trị sát thương (Giống bên ScriptAbility)
        // Để EventDamage có thể lấy ra và ghi đè vào event.setDamage()
        victim.setMetadata("SKILL_DAMAGE_VALUE", new FixedMetadataValue(Main.getInstance(), amount));

        // 2. Dán nhãn IS_ABILITY để tránh lặp kỹ năng
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

        // 3. Thực thi gây damage (Dùng 1.0 để kích hoạt event onDamage)
        victim.damage(1.0, source);

        // 4. Dọn dẹp metadata
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (victim.isValid()) {
                victim.removeMetadata("IS_ABILITY", Main.getInstance());
                victim.removeMetadata("SKILL_DAMAGE_VALUE", Main.getInstance());
            }
        }, 2L);
    }

    // 3. Hàm hỗ trợ tìm mục tiêu gần nhất (Rất cần cho skill lốc xoáy/đạn bay)
    public LivingEntity getNearest(Player player, double range) {
        List<Entity> entities = player.getNearbyEntities(range, range, range);
        LivingEntity nearest = null;
        double minDist = range + 1.0;
        for (Entity e : entities) {
            if (e instanceof LivingEntity le && e != player) {
                double dist = e.getLocation().distance(player.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = le;
                }
            }
        }
        return nearest;
    }

    public void sendMessage(Player p, String msg) {
        if (p != null) p.sendMessage(msg.replace("&", "§"));
    }
}