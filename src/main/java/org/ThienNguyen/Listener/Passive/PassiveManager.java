package org.ThienNguyen.Listener.Passive;

import org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger;
import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;


public class PassiveManager {

    
    private static PassiveManager INSTANCE;
    public static PassiveManager getInstance() { return INSTANCE; }
    public static void init() { INSTANCE = new PassiveManager(); INSTANCE.loadAll(); }

    
    
    private final Map<String, PassiveDef> registry = new ConcurrentHashMap<>();

    
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    
    private final Map<UUID, Set<String>> passiveIdsCache = new ConcurrentHashMap<>();

    
    private static final NamespacedKey PDC_KEY = new NamespacedKey(Main.getInstance(), "passive_ids");

    
    public void loadAll() {
        registry.clear();

        File folder = new File(Main.getInstance().getDataFolder(), "Listener/Passives");
        if (!folder.exists()) {
            folder.mkdirs();
            writeExample(folder);
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            Main.getInstance().getLogger().info("[Passive] Không tìm thấy file passive nào.");
            return;
        }

        int loaded = 0;
        for (File f : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            PassiveDef def = PassiveDef.fromYaml(cfg, f.getName());
            if (def != null) {
                registry.put(def.getId(), def);
                loaded++;
            }
        }
        Main.getInstance().getLogger().info("[Passive] Đã load " + loaded + " passive(s).");
    }

    

    
    public void trigger(PassiveTrigger triggerType,
                        Player actor,
                        LivingEntity victim,
                        double damage,
                        boolean isCrit,
                        EntityDamageByEntityEvent event) {

        if (actor == null) return;

        
        
        if (actor.hasMetadata(org.ThienNguyen.Listener.Passive.Mechanics.DamageMechanic.META_KEY_NORMAL_SOURCE)) return;
        if (victim != null && victim.hasMetadata(org.ThienNguyen.Listener.Passive.Mechanics.DamageMechanic.META_KEY_NORMAL_SOURCE)) return;

        Set<String> passiveIds = getCachedPassiveIds(actor);
        if (passiveIds.isEmpty()) return;

        PassiveContext ctx = new PassiveContext(actor, victim, damage, event);

        for (String pid : passiveIds) {
            PassiveDef def = registry.get(pid);
            if (def == null) continue;
            if (def.getTrigger() != triggerType) continue;
            
            if (!def.checkConditions(ctx, isCrit)) continue;
            if (!rollChance(def.getChance(ctx))) continue;
            if (isOnCooldown(actor.getUniqueId(), pid)) continue;

            for (PassiveMechanic m : def.getMechanics()) {
                m.execute(ctx);
            }

            applyCooldown(actor.getUniqueId(), pid, def.getCooldownSeconds());
        }
    }

    
    public void trigger(PassiveTrigger triggerType,
                        Player actor,
                        LivingEntity victim,
                        double damage,
                        boolean isCrit) {
        trigger(triggerType, actor, victim, damage, isCrit, null);
    }

    
    public void triggerBlockBreak(Player actor, org.bukkit.block.Block block) {
        if (actor == null || block == null) return;

        Set<String> passiveIds = getCachedPassiveIds(actor);
        if (passiveIds.isEmpty()) return;

        PassiveContext ctx = new PassiveContext(actor, null, 0, null, block);

        for (String pid : passiveIds) {
            PassiveDef def = registry.get(pid);
            if (def == null) continue;
            if (def.getTrigger() != PassiveTrigger.ON_BLOCK_BREAK) continue;
            
            
            if (!def.checkConditions(ctx)) continue;
            if (!rollChance(def.getChance(ctx))) continue;
            if (isOnCooldown(actor.getUniqueId(), pid)) continue;

            for (PassiveMechanic m : def.getMechanics()) {
                m.execute(ctx);
            }

            applyCooldown(actor.getUniqueId(), pid, def.getCooldownSeconds());
        }
    }

    

    
    private Set<String> getCachedPassiveIds(Player player) {
        return passiveIdsCache.computeIfAbsent(player.getUniqueId(), uuid -> collectPassiveIds(player));
    }

    
    public void invalidatePassiveCache(UUID uuid) {
        passiveIdsCache.remove(uuid);
    }

    
    private Set<String> collectPassiveIds(Player player) {
        Set<String> ids = new HashSet<>();
        ItemStack[] equip = {
                player.getInventory().getHelmet(),
                player.getInventory().getChestplate(),
                player.getInventory().getLeggings(),
                player.getInventory().getBoots(),
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand()
        };
        for (ItemStack item : equip) {
            if (item == null) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String raw = meta.getPersistentDataContainer().get(PDC_KEY, PersistentDataType.STRING);
            if (raw == null || raw.isEmpty()) continue;
            for (String id : raw.split(",")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) ids.add(trimmed);
            }
        }
        return ids;
    }

    private boolean rollChance(int chance) {
        if (chance >= 100) return true;
        if (chance <= 0)   return false;
        return ThreadLocalRandom.current().nextInt(100) < chance;
    }

    private boolean isOnCooldown(UUID uuid, String passiveId) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) return false;
        Long expire = map.get(passiveId);
        return expire != null && System.currentTimeMillis() < expire;
    }

    private void applyCooldown(UUID uuid, String passiveId, int seconds) {
        if (seconds <= 0) return;
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(passiveId, System.currentTimeMillis() + (long) seconds * 1000L);
    }

    
    public void handlePlayerDeath(Player player) {
        if (player == null) return;
        for (PassiveDef def : registry.values()) {
            for (PassiveMechanic m : def.getMechanics()) {
                if (m instanceof DeathAware aware) {
                    aware.onPlayerDeath(player);
                }
            }
        }
    }

    
    public void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
        passiveIdsCache.remove(uuid);

        for (PassiveDef def : registry.values()) {
            for (PassiveMechanic m : def.getMechanics()) {
                clearPlayerAware(m, uuid);  
            }
        }
    }

    private void clearPlayerAware(PassiveMechanic mechanic, UUID uuid) {
        if (mechanic instanceof PlayerAware aware) {
            aware.onPlayerQuit(uuid);   
        }
    }

    
    public Optional<PassiveDef> getDef(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    public Collection<PassiveDef> getAllDefs() { return registry.values(); }

    
    private void writeExample(File folder) {
        File ex = new File(folder, "example_execute.yml");
        if (ex.exists()) return;
        String content =
                "# Passive mẫu: Tử Thần — instant kill khi HP victim dưới 5%\n" +
                        "id: execute_5pct\n" +
                        "display-name: \"&c[Tử Thần]\"\n" +
                        "trigger: ON_HIT\n" +
                        "condition:\n" +
                        "  target-hp-percent-below: \"5\"        # hỗ trợ expression: \"%player_level% * 0.5\"\n" +
                        "  target-type: BOTH\n" +
                        "  # expressions:                       # MỚI: điều kiện tùy ý\n" +
                        "  #   - \"%player_level% >= 10\"\n" +
                        "  #   - \"{damage} > 5.0\"\n" +
                        "chance: \"100\"                          # hỗ trợ expression: \"%player_level% * 2\"\n" +
                        "cooldown: 0\n" +
                        "actions:\n" +
                        "  - type: DAMAGE\n" +
                        "    target: VICTIM\n" +
                        "    amount: 99999\n" +
                        "    damage-type: TRUE\n\n" +
                        "# Passive mẫu: Kill drop vàng 25%\n" +
                        "# id: kill_drop_gold\n" +
                        "# trigger: ON_KILL\n" +
                        "# chance: 25\n" +
                        "# cooldown: 0\n" +
                        "# actions:\n" +
                        "#   - type: DROP_ITEM\n" +
                        "#     location: VICTIM\n" +
                        "#     material: GOLD_NUGGET\n" +
                        "#     amount: 1-3\n";
        try {
            ex.getParentFile().mkdirs();
            java.nio.file.Files.writeString(ex.toPath(), content);
        } catch (Exception ignored) {}
    }
}