package org.ThienNguyen.Listener.Passive.Mechanics;

import me.clip.placeholderapi.PlaceholderAPI;
import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class TitleMechanic extends AbstractMechanic {

    private final String rawTitle;
    private final String rawSubtitle;
    private final String rawFadeIn;
    private final String rawStay;
    private final String rawFadeOut;

    public TitleMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawTitle    = cfg.getString("title",    "");
        this.rawSubtitle = cfg.getString("subtitle", "");
        this.rawFadeIn   = cfg.getString("fade-in",  "10");
        this.rawStay     = cfg.getString("stay",     "70");
        this.rawFadeOut  = cfg.getString("fade-out", "20");
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity targetEntity = resolveTarget(ctx);
        if (!(targetEntity instanceof Player receiver)) return false;
        if (rawTitle.isBlank() && rawSubtitle.isBlank()) return false;

        
        String title    = colorize(PlaceholderAPI.setPlaceholders(receiver, rawTitle));
        String subtitle = colorize(PlaceholderAPI.setPlaceholders(receiver, rawSubtitle));

        
        int fadeIn  = ExpressionResolver.resolveInt(rawFadeIn,  ctx.getActor(), 10);
        int stay    = ExpressionResolver.resolveInt(rawStay,    ctx.getActor(), 70);
        int fadeOut = ExpressionResolver.resolveInt(rawFadeOut, ctx.getActor(), 20);

        receiver.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        return true;
    }

    private static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}