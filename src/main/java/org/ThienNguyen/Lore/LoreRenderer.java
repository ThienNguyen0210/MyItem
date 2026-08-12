package org.ThienNguyen.Lore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LoreRenderer
 * ------------
 * Group-based lore rendering engine (MMOItems-style "section" behaviour),
 * inferred automatically from the lore-format - no <section> tags needed.
 *
 * Rendering rule:
 *   1. Scan the format top -> bottom.
 *   2. Every STATIC line (header text, dividers, {bar}/{sbar}) belongs to
 *      the FIRST placeholder found below it.
 *   3. A group = [pending static header lines] + [one or more consecutive
 *      placeholder lines]. It keeps absorbing placeholder lines until the
 *      next static line appears - that static line becomes the header of
 *      the NEXT group (the "anchor" for whatever follows it).
 *   4. If a group renders at least one placeholder successfully, its header
 *      is printed followed by only the placeholder lines that rendered.
 *   5. If NONE of a group's placeholders render, the whole group (header +
 *      body) is dropped silently. Empty groups never appear in the output.
 *
 * This class only orchestrates - it does not know about ItemStacks, stats,
 * abilities, etc. Those stay in StatsLore / AbilityLore / EffectLore / ...
 * exactly as before. LoreGenerator wires them in via a PlaceholderResolver,
 * so the existing placeholder system and public API are untouched.
 */
public class LoreRenderer {

    /** Classic bracket placeholders: {stats}, {stats:foo}, {ability}, {bar}, {sbar}, {tier}, {sockets} ... */
    private static final Pattern BRACE_PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)(:[a-zA-Z0-9_]+)?\\}");

    /** MMOItems-style placeholders: #item-type#, #required-level#, #profession-mining# ... */
    private static final Pattern HASH_PLACEHOLDER = Pattern.compile("#([a-zA-Z0-9_\\-]+)#");

    /**
     * Tokens that are purely decorative (dividers/bars). They use the same
     * {} syntax as real placeholders but never gate a group - they're
     * treated as STATIC even though they're technically "{...}".
     */
    private static final Set<String> DECORATIVE_TOKENS = Set.of("bar", "sbar");

    //    /**
//     * Resolves one placeholder occurrence into zero or more finished lore
//     * lines (pre-colorize). Return null or an empty list to signal "this
//     * placeholder has nothing to show" (e.g. the item lacks that stat).
//     *
//     * @param token    placeholder name, lower-cased (e.g. "stats", "item-type")
//     * @param argument the ":xxx" argument for brace placeholders (e.g. "damage"), or null
//     * @param rawLine  the untouched source line, in case a resolver needs full context
//     */
    public interface PlaceholderResolver {
        List<String> resolve(String token, String argument, String rawLine);
    }

    private final PlaceholderResolver resolver;
    private final Function<String, String> decorativeRenderer;

    /**
     * @param resolver           resolves data placeholders ({@code #xxx#} / {@code {xxx}} / {@code {xxx:yyy}})
     * @param decorativeRenderer resolves purely visual tokens ("bar", "sbar") into their replacement text;
     *                           pass {@code null} (or a no-op) to just strip them
     */
    public LoreRenderer(PlaceholderResolver resolver, Function<String, String> decorativeRenderer) {
        this.resolver = resolver;
        this.decorativeRenderer = decorativeRenderer;
    }

    /** One parsed section: its static header line(s) and the placeholder lines that follow. */
    private static class Group {
        final List<String> headerLines = new ArrayList<>();
        final List<String> placeholderLines = new ArrayList<>();
    }

    /**
     * Renders the full lore-format into final display lines, applying the
     * group-drop rule. This is the only entry point callers need.
     */
    public List<String> render(List<String> formatLines) {
        List<String> result = new ArrayList<>();

        for (Group group : parseGroups(formatLines)) {

            // Decorative line ({bar}/{sbar}) luôn được render
            for (String header : group.headerLines) {
                if (containsDecorativeToken(header)) {
                    result.add(renderStaticLine(header));
                }
            }

            List<String> body = new ArrayList<>();

            for (String line : group.placeholderLines) {
                List<String> rendered = renderPlaceholderLine(line);

                if (rendered != null && !rendered.isEmpty()) {
                    body.addAll(rendered);
                }
            }

            // Nếu không có placeholder nào render được
            // thì chỉ giữ lại các dòng decorative
            if (body.isEmpty()) {
                continue;
            }

            for (String header : group.headerLines) {
                if (!containsDecorativeToken(header)) {
                    result.add(renderStaticLine(header));
                }
            }

            result.addAll(body);
        }

        return result;
    }

    // ------------------------------------------------------------------
    // Grouping
    // ------------------------------------------------------------------

    private List<Group> parseGroups(List<String> formatLines) {
        List<Group> groups = new ArrayList<>();
        Group current = new Group();
        boolean currentHasPlaceholders = false;

        for (String line : formatLines) {
            if (line == null) continue;

            if (isDynamicPlaceholderLine(line)) {
                current.placeholderLines.add(line);
                currentHasPlaceholders = true;
            } else {
                if (currentHasPlaceholders) {
                    // The current group already collected placeholders - this
                    // static line is the anchor header for a brand new group.
                    groups.add(current);
                    current = new Group();
                    currentHasPlaceholders = false;
                }
                // Still in the "header accumulation" phase for the group
                // that hasn't found its first placeholder yet.
                current.headerLines.add(line);
            }
        }

        if (!current.headerLines.isEmpty() || !current.placeholderLines.isEmpty()) {
            groups.add(current);
        }

        return groups;
    }
    private boolean containsDecorativeToken(String line) {
        Matcher matcher = BRACE_PLACEHOLDER.matcher(line);

        while (matcher.find()) {
            String token = matcher.group(1).toLowerCase();

            if (DECORATIVE_TOKENS.contains(token)) {
                return true;
            }
        }

        return false;
    }
    /**
     * A line "gates" a group (i.e. counts as dynamic) only if it contains a
     * real data placeholder: any {@code #xxx#} token, or a {@code {xxx}}/
     * {@code {xxx:yyy}} token that is NOT decorative (bar/sbar).
     */

    private boolean isDynamicPlaceholderLine(String line) {
        if (HASH_PLACEHOLDER.matcher(line).find()) return true;

        Matcher brace = BRACE_PLACEHOLDER.matcher(line);
        while (brace.find()) {
            String token = brace.group(1).toLowerCase();
            // NẾU TOKEN LÀ bar HOẶC sbar, TA BỎ QUA KHÔNG TÍNH LÀ DÒNG ĐỘNG
            if (DECORATIVE_TOKENS.contains(token)) {
                continue;
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Rendering individual lines
    // ------------------------------------------------------------------

    /** Renders a header/static line: resolves decorative {bar}/{sbar} tokens, then colorizes. */
    private String renderStaticLine(String line) {
        Matcher brace = BRACE_PLACEHOLDER.matcher(line);
        StringBuilder sb = new StringBuilder();
        int last = 0;

        while (brace.find()) {
            String token = brace.group(1).toLowerCase();
            sb.append(line, last, brace.start());
            if (DECORATIVE_TOKENS.contains(token)) {
                sb.append(decorativeRenderer != null ? decorativeRenderer.apply(token) : "");
            } else {
                sb.append(brace.group());
            }
            last = brace.end();
        }
        sb.append(line.substring(last));

        return LoreGenerator.colorize(sb.toString());
    }

    /**
     * Renders one placeholder-bearing line.
     * - If the *entire* (trimmed) line is a single placeholder, that
     *   placeholder may expand into multiple final lore lines (e.g. {stats},
     *   {ability}, {effect} list-style placeholders).
     * - Otherwise the line is treated as literal text with inline
     *   placeholders substituted in place; the line renders if at least one
     *   inline placeholder resolved (unresolved ones are left as-is, same
     *   as the legacy behaviour).
     * Returns an empty list if nothing in the line resolved.
     */
    private List<String> renderPlaceholderLine(String line) {
        String trimmed = line.trim();

        Matcher soloHash = HASH_PLACEHOLDER.matcher(trimmed);
        if (soloHash.matches()) {
            return colorizeAll(resolve(soloHash.group(1), null, line));
        }

        Matcher soloBrace = BRACE_PLACEHOLDER.matcher(trimmed);
        if (soloBrace.matches() && !DECORATIVE_TOKENS.contains(soloBrace.group(1).toLowerCase())) {
            String token = soloBrace.group(1).toLowerCase();
            String arg = soloBrace.group(2) != null ? soloBrace.group(2).substring(1) : null;
            return colorizeAll(resolve(token, arg, line));
        }

        boolean[] resolvedAny = {false};
        String working = replaceTokens(line, HASH_PLACEHOLDER, resolvedAny);
        working = replaceTokens(working, BRACE_PLACEHOLDER, resolvedAny);

        if (!resolvedAny[0]) return Collections.emptyList();

        List<String> out = new ArrayList<>();
        out.add(LoreGenerator.colorize(working));
        return out;
    }

    private String replaceTokens(String line, Pattern pattern, boolean[] resolvedAny) {
        Matcher m = pattern.matcher(line);
        StringBuilder sb = new StringBuilder();
        int last = 0;

        while (m.find()) {
            String token;
            String arg = null;

            if (pattern == BRACE_PLACEHOLDER) {
                token = m.group(1).toLowerCase();
                if (DECORATIVE_TOKENS.contains(token)) continue; // handled elsewhere, leave as-is
                arg = m.group(2) != null ? m.group(2).substring(1) : null;
            } else {
                token = m.group(1);
            }

            List<String> resolved = resolve(token, arg, line);
            sb.append(line, last, m.start());

            if (resolved != null && !resolved.isEmpty()) {
                sb.append(resolved.get(0));
                resolvedAny[0] = true;
            } else {
                sb.append(m.group()); // legacy behaviour: leave unresolved token text untouched
            }
            last = m.end();
        }
        sb.append(line.substring(last));
        return sb.toString();
    }

    private List<String> resolve(String token, String arg, String rawLine) {
        if (resolver == null) return null;
        try {
            return resolver.resolve(token, arg, rawLine);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> colorizeAll(List<String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String s : raw) out.add(LoreGenerator.colorize(s));
        return out;
    }
}