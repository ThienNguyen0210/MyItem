package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PlayerAware;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;


public class AddValueMechanic extends AbstractMechanic implements PlayerAware {

    private final String key;
    private final String rawAmount;
    private final String rawDuration;
    private final String targetKeyRaw;

    public AddValueMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.key          = cfg.getString("key", "");
        this.rawAmount    = cfg.getString("amount", "1");

        this.rawDuration  = cfg.getString("duration", "0");
        this.targetKeyRaw = cfg.getString("target", "VICTIM").toUpperCase();
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (key.isEmpty()) return false;

        LivingEntity target = resolveValueTarget(ctx);
        if (target == null) return false;

        Player actor = ctx.getActor();

        double amount          = ExpressionResolver.resolve(rawAmount, actor, 1);
        double durationSeconds = ExpressionResolver.resolve(rawDuration, actor, 0);
        long durationMillis    = durationSeconds > 0 ? (long) (durationSeconds * 1000L) : 0L;


        double current = parseNumber(PlayerValueStore.get(target.getUniqueId(), key));
        double updated = current + amount;


        PlayerValueStore.set(target.getUniqueId(), key, formatNumber(updated), durationMillis);
        return true;
    }

    private double parseNumber(String raw) {
        if (raw == null) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private String formatNumber(double value) {
        if (!Double.isInfinite(value) && value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private LivingEntity resolveValueTarget(PassiveContext ctx) {
        return switch (targetKeyRaw) {
            case "ACTOR", "SELF" -> ctx.getActor();
            case "VICTIM"        -> ctx.getVictim();
            default               -> ctx.getActor();
        };
    }

    @Override
    public void onPlayerQuit(UUID uuid) {
        PlayerValueStore.clear(uuid);
    }
}