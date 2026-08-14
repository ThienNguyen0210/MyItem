package org.ThienNguyen.Listener;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Cooldown Reduction stat hook.
 *
 * On every skill cast (MMOCore or Fabled), reads the caster's effective
 * "cooldown_reduction" combat stat (item PDC + jewelry + combo/gems + any
 * temp buffs, all already aggregated into PlayerCombatCache) and shortens
 * the cooldown of the skill that was just cast accordingly.
 *
 * ── Formula ──────────────────────────────────────────────────────────────
 * Same pattern as EventDamage's armor-formula / magic-defense-formula: a
 * single exp4j expression string in listener.yml, with one variable —
 * "value" — bound to the caster's cooldown_reduction stat. It must evaluate
 * to the PERCENTAGE to shave off the skill's cooldown.
 *
 *     cooldown-reduction-formula: "value * 2"
 *
 * means each point of the stat is worth 2% cooldown reduction. Change this
 * one line to reshape the curve, e.g. "min(value * 2, 80)" to cap it at
 * 80% (recommended - an uncapped formula lets a high enough stat push the
 * result past 100%, which just floors to a fully-zeroed cooldown but wastes
 * the extra points), or "20 * log(value + 1)" for diminishing returns. No
 * config key = falls back to "value * 2".
 *
 *     newCooldown = originalCooldown * (1 - reductionPercent / 100)
 * ─────────────────────────────────────────────────────────────────────────
 *
 * ── Integration notes ───────────────────────────────────────────────────
 * Fabled's hook (FabledHook, bottom of this file) uses a normal compile-time
 * @EventHandler. PlayerCastSkillEvent is a pre-cast, cancellable event -
 * Fabled only calls PlayerSkill#startCooldown() in its own code AFTER the
 * event finishes dispatching, so the actual subtractCooldown() call is
 * deferred to the next tick (see FabledHook#onCast) to avoid being
 * overwritten by that startCooldown() call.
 *
 * MMOCore's hook is different: MMOCore-API's package for "a skill was cast"
 * has moved between versions, so it's resolved via reflection at runtime
 * (see MMOCORE_EVENT_CANDIDATES + registerMmoCoreReflective()) instead of a
 * compile-time import. This keeps the build green even when the class name
 * is wrong, but it means the MMOCore side won't actually do anything until
 * you fill in the TODOs there with your real MMOCore-API's class/method
 * names. Only plugins that are actually installed get a hook registered, so
 * this class is safe to ship on a server running just one of the two (or
 * neither).
 */
public class CooldownReductionListener {

    private static String cachedFormula = null;
    private static Expression cachedExpression = null;

    /** Call once from Main#onEnable(), after MMOCore/Fabled (if present) have loaded. */
    public static void register() {
        boolean any = false;

        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            any |= registerMmoCoreReflective();
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Fabled")) {
            try {
                Bukkit.getPluginManager().registerEvents(new FabledHook(), Main.getInstance());
                any = true;
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[" + Main.getInstance().getName() +
                        "] Failed to register FabledHook - likely a Fabled API version mismatch: " + t);
            }
        }

        if (!any) {
            Bukkit.getLogger().info("[" + Main.getInstance().getName() +
                    "] cooldown_reduction stat loaded, but no supported skill plugin hook could be registered.");
        }
    }

    /**
     * Finds the real skill-cast event class at runtime (see MMOCORE_EVENT_CANDIDATES)
     * and registers a raw event executor for it. Returns false (and logs a warning)
     * if none of the candidate class names exist in the MMOCore-API you're running -
     * in which case fill in the TODOs above and add the right class name to the list.
     */
    @SuppressWarnings("unchecked")
    private static boolean registerMmoCoreReflective() {
        Class<?> eventClass = null;
        for (String candidate : MMOCORE_EVENT_CANDIDATES) {
            try {
                eventClass = Class.forName(candidate);
                break;
            } catch (ClassNotFoundException ignored) {
                // try the next candidate
            }
        }

        if (eventClass == null) {
            Bukkit.getLogger().warning("[" + Main.getInstance().getName() +
                    "] Could not find MMOCore's skill-cast event class. cooldown_reduction will NOT apply to MMOCore skills " +
                    "until CooldownReductionListener.MMOCORE_EVENT_CANDIDATES is updated with the correct class name for your MMOCore-API version.");
            return false;
        }

        try {
            Bukkit.getPluginManager().registerEvent(
                    (Class<? extends org.bukkit.event.Event>) eventClass,
                    new Listener() {},
                    EventPriority.MONITOR,
                    (listener, event) -> onMmoCoreCastReflective(event),
                    Main.getInstance(),
                    true
            );
            return true;
        } catch (Exception ex) {
            Bukkit.getLogger().warning("[" + Main.getInstance().getName() +
                    "] Found MMOCore's cast event class but failed to register a listener for it: " + ex.getMessage());
            return false;
        }
    }

    /**
     * TODO: this body is a placeholder. Once you've confirmed the real event class,
     * replace the reflective getPlayer()/getCooldown()/setCooldown() lookups below
     * with direct typed calls (like FabledHook does) for clarity and performance -
     * reflection on every single cast is not something you want to keep long-term.
     */
    private static void onMmoCoreCastReflective(org.bukkit.event.Event event) {
        try {
            Player player = extractPlayer(event);
            if (player == null) return;

            Double originalCooldown = extractOriginalCooldown(event);
            if (originalCooldown == null || originalCooldown <= 0) return;

            double reduced = applyReduction(player.getUniqueId(), originalCooldown);
            if (reduced < originalCooldown) {
                writeReducedCooldown(event, reduced);
            }
        } catch (Exception ex) {
            Bukkit.getLogger().warning("[" + Main.getInstance().getName() +
                    "] cooldown_reduction MMOCore hook failed - update the reflective getters in CooldownReductionListener: " + ex.getMessage());
        }
    }

    // TODO: implement using the real API once known, e.g.:
    //     return ((PlayerCastSkillEvent) event).getPlayerData().getPlayer();
    private static Player extractPlayer(org.bukkit.event.Event event) throws Exception {
        Object result = event.getClass().getMethod("getPlayer").invoke(event);
        return (result instanceof Player) ? (Player) result : null;
    }

    // TODO: implement using the real API once known, e.g.:
    //     return event.getSkill().getSkill().getCooldown(event.getSkill().getLevel());
    private static Double extractOriginalCooldown(org.bukkit.event.Event event) throws Exception {
        Object result = event.getClass().getMethod("getCooldown").invoke(event);
        return (result instanceof Number) ? ((Number) result).doubleValue() : null;
    }

    // TODO: implement using the real API once known, e.g.:
    //     event.getPlayerData().getCooldownMap().setCooldown(event.getSkill().getSkill(), reduced);
    private static void writeReducedCooldown(org.bukkit.event.Event event, double reduced) throws Exception {
        event.getClass().getMethod("setCooldown", double.class).invoke(event, reduced);
    }

    /** Evaluates the listener.yml formula against the caster's stat value. */
    private static double calculateReductionPercent(double statValue) {
        Expression expr = getExpression();
        try {
            double result = expr.setVariable("value", statValue).evaluate();
            return Math.max(0.0, result);
        } catch (Exception ex) {
            // Bad formula in config - fail safe to no reduction rather than crash the cast.
            return 0.0;
        }
    }

    /**
     * Shared entry point used by both hooks: given the caster and the skill's
     * un-modified base cooldown (in whatever unit the source plugin uses -
     * seconds for both MMOCore and Fabled), returns the reduced cooldown.
     */
    public static double applyReduction(UUID casterUuid, double originalCooldown) {
        if (originalCooldown <= 0 || casterUuid == null) return originalCooldown;

        double statValue = PlayerCombatCache.getEffectiveByStatName(casterUuid, "cooldown_reduction");
        if (statValue <= 0) return originalCooldown;

        double reductionPercent = calculateReductionPercent(statValue);
        if (reductionPercent <= 0) return originalCooldown;

        return Math.max(0.0, originalCooldown * (1.0 - reductionPercent / 100.0));
    }

    private static Expression getExpression() {
        if (cachedExpression == null) {
            cachedFormula = Main.getInstance().getCustomListenerConfig()
                    .getString("cooldown-reduction-formula", "value * 2");
            try {
                cachedExpression = new ExpressionBuilder(cachedFormula)
                        .variable("value")
                        .build();
            } catch (Exception ex) {
                cachedFormula = "value * 2";
                cachedExpression = new ExpressionBuilder(cachedFormula)
                        .variable("value")
                        .build();
            }
        }
        return cachedExpression;
    }

    /** Mirrors EventDamage.resetFormulaCache() - call from your /mi reload handler. */
    public static void resetFormulaCache() {
        cachedFormula = null;
        cachedExpression = null;
    }

    // ============================================================
    // MMOCore hook
    // ============================================================
    // Registered reflectively (see registerMmoCoreReflective() above) instead of a
    // normal Listener class, because the exact event class name/package for "a
    // skill was just cast" has moved between MMOCore-API versions and this project
    // doesn't know which one you're compiling against. This means the plugin JAR
    // builds successfully even before the TODOs below are filled in - it just won't
    // actually reduce anything until the class/method names are corrected.
    //
    // TODO: replace MMOCORE_EVENT_CANDIDATES with the real class name from your
    // IDE's autocomplete on "net.Indyuce.mmocore.api.event.", then fill in
    // extractPlayer / extractOriginalCooldown / writeReducedCooldown above using
    // that class's real getters/setters. Once you're sure of the class, you can
    // delete all this reflection machinery and go back to a plain @EventHandler
    // like the Fabled hook has - reflection here is only a stopgap to unblock
    // your build.
    private static final String[] MMOCORE_EVENT_CANDIDATES = {
            "net.Indyuce.mmocore.api.event.PlayerCastSkillEvent",
            "net.Indyuce.mmocore.api.event.SkillCastEvent",
            "net.Indyuce.mmocore.skill.cast.event.PlayerCastSkillEvent"
    };

    // ============================================================
    // Fabled hook
    // ============================================================
    private static class FabledHook implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onCast(studio.magemonkey.fabled.api.event.PlayerCastSkillEvent event) {
            Player player = event.getPlayer();
            if (player == null) return;

            studio.magemonkey.fabled.api.player.PlayerSkill playerSkill = event.getSkill();
            if (playerSkill == null) return;

            double originalCooldown = playerSkill.getData().getCooldown(playerSkill.getLevel());
            if (originalCooldown <= 0) return;

            double reduced = applyReduction(player.getUniqueId(), originalCooldown);
            double secondsToRemove = originalCooldown - reduced;
            if (secondsToRemove <= 0) return;

            // PlayerSkill stores its cooldown internally as an expiry TIMESTAMP,
            // not a duration - there's no setCooldown(double). PlayerCastSkillEvent
            // is a pre-cast event: Fabled only calls PlayerSkill#startCooldown()
            // AFTER this event finishes dispatching, so the subtraction is deferred
            // to the next tick, once the cooldown timer is guaranteed to be live.
            Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                    playerSkill.subtractCooldown(secondsToRemove));
        }
    }
}