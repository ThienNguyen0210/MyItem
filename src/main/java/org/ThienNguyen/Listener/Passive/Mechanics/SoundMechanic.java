package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;


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