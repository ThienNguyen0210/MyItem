package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

/**
 * Phát 1 Sound chuẩn Bukkit tại vị trí target.
 *
 * yml:
 * - type: SOUND
 *   target: VICTIM              # SELF | VICTIM — vị trí phát âm thanh
 *   sound: ENTITY_GENERIC_EXPLODE # tên Sound enum chuẩn Bukkit
 *   volume: 1.0                  # mặc định 1.0
 *   pitch: 1.0                    # mặc định 1.0 (0.5 - 2.0)
 *
 * Dùng World.playSound() (phát cho MỌI người chơi nghe được trong phạm vi tự nhiên
 * của Minecraft) thay vì Player.playSound() (chỉ 1 người) — vì âm thanh nổ/combat nên
 * mọi người gần đó đều nghe thấy, giống hành vi vanilla.
 *
 * "Thành công" = tên Sound hợp lệ và phát được tại vị trí entity còn valid.
 */
public class SoundMechanic extends AbstractMechanic {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SoundMechanic(ConfigurationSection cfg) {
        super(cfg);

        Sound parsed = null;
        try {
            parsed = Sound.valueOf(cfg.getString("sound", "").toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // giữ null -> doExecute() trả false, không phát âm thanh sai
        }
        this.sound = parsed;

        this.volume = (float) cfg.getDouble("volume", 1.0);
        this.pitch = (float) cfg.getDouble("pitch", 1.0);
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (sound == null) return false;

        LivingEntity entity = resolveTarget(ctx);
        if (entity == null || !entity.isValid()) return false;

        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) return false;

        world.playSound(loc, sound, volume, pitch);
        return true;
    }
}