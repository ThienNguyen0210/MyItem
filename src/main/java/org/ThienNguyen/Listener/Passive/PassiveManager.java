package org.ThienNguyen.Listener.Passive;

import org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger;
import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
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

/**
 * Điều phối toàn bộ hệ thống passive:
 * - load / reload <id>.yml từ plugins/<Plugin>/Listener/Passives/
 * - lưu cooldown in-memory
 * - cung cấp API trigger() cho EventDamage
 */
public class PassiveManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static PassiveManager INSTANCE;
    public static PassiveManager getInstance() { return INSTANCE; }
    public static void init() { INSTANCE = new PassiveManager(); INSTANCE.loadAll(); }

    // ── Data ──────────────────────────────────────────────────────────────────
    /** id → def */
    private final Map<String, PassiveDef> registry = new ConcurrentHashMap<>();

    /** UUID player → (passiveId → expireTimestamp) */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * CACHE HIỆU NĂNG: passiveIds theo UUID, tránh quét lại 6 slot equipment + clone ItemMeta
     * mỗi lần trigger() được gọi. Mỗi đòn đánh trong EventDamage gọi trigger() 3-4 lần
     * (ON_HIT, ON_TAKE_DAMAGE, ON_KILL) — nếu không cache, đó là 18-24 lần ItemMeta.clone()
     * cho MỖI đòn đánh, dù equipment không đổi giữa các đòn liên tiếp.
     *
     * Cache được invalidate (xoá) bởi PassiveCacheListener khi player đổi item
     * (tương tự PlayerCombatCache + CacheListener.refreshCache() đã có sẵn trong hệ thống).
     */
    private final Map<UUID, Set<String>> passiveIdsCache = new ConcurrentHashMap<>();

    /** PDC key để đọc list passive id từ item */
    private static final NamespacedKey PDC_KEY = new NamespacedKey(Main.getInstance(), "passive_ids");

    // ── Load ──────────────────────────────────────────────────────────────────
    public void loadAll() {
        registry.clear();

        File folder = new File(Main.getInstance().getDataFolder(), "Listener/Passives");
        if (!folder.exists()) {
            folder.mkdirs();
            // Tạo file ví dụ để dev tham khảo
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

    // ── API trigger ───────────────────────────────────────────────────────────
    /**
     * Gọi từ EventDamage. Quét trang bị của actor → tìm passive_ids → trigger.
     *
     * @param triggerType loại trigger
     * @param actor       người tấn công / chủ passive
     * @param victim      mục tiêu thực thể sống (có thể null)
     * @param damage      damage hiện tại
     * @param isCrit      đòn có phải crit không
     * @param event       combat event gốc (có thể null)
     */
    public void trigger(PassiveTrigger triggerType,
                        Player actor,
                        LivingEntity victim,
                        double damage,
                        boolean isCrit,
                        EntityDamageByEntityEvent event) {

        if (actor == null) return;

        // Chặn đệ quy vô hạn: nếu damage này phát sinh từ chính DamageMechanic (NORMAL mode)
        // gọi entity.damage() lồng vào EventDamage, bỏ qua luôn — không trigger passive nào nữa.
        // Check cả actor và victim vì tuỳ trigger mà bên "bị đánh" có thể là actor (ON_TAKE_DAMAGE)
        // hoặc victim (ON_HIT/ON_KILL).
        if (actor.hasMetadata(org.ThienNguyen.Listener.Passive.Mechanics.DamageMechanic.META_KEY_NORMAL_SOURCE)) return;
        if (victim != null && victim.hasMetadata(org.ThienNguyen.Listener.Passive.Mechanics.DamageMechanic.META_KEY_NORMAL_SOURCE)) return;

        Set<String> passiveIds = getCachedPassiveIds(actor);
        if (passiveIds.isEmpty()) return;

        PassiveContext ctx = new PassiveContext(actor, victim, damage, event);

        for (String pid : passiveIds) {
            PassiveDef def = registry.get(pid);
            if (def == null) continue;
            if (def.getTrigger() != triggerType) continue;
            if (!checkConditions(def, actor, victim, isCrit)) continue;
            if (!rollChance(def.getChance())) continue;
            if (isOnCooldown(actor.getUniqueId(), pid)) continue;

            // Mỗi mechanic top-level trong "actions" chạy độc lập (không phụ thuộc nhau).
            // Quan hệ cha-con (children) chỉ áp dụng GIỮA 1 mechanic và children của riêng nó,
            // xử lý bên trong AbstractMechanic.execute() — không phải giữa các mechanic top-level.
            for (PassiveMechanic m : def.getMechanics()) {
                m.execute(ctx);
            }

            applyCooldown(actor.getUniqueId(), pid, def.getCooldownSeconds());
        }
    }

    /**
     * Overload cho các trigger KHÔNG phát sinh từ 1 EntityDamageByEntityEvent cụ thể
     * (ví dụ ON_DEATH: player có thể chết do /kill, void, suffocation... không phải lúc nào
     * cũng có 1 EntityDamageByEntityEvent đi kèm). event sẽ là null trong PassiveContext;
     * mechanic nào cần event (hiếm) phải tự null-check.
     */
    public void trigger(PassiveTrigger triggerType,
                        Player actor,
                        LivingEntity victim,
                        double damage,
                        boolean isCrit) {
        trigger(triggerType, actor, victim, damage, isCrit, null);
    }

    /**
     * Overload riêng cho ON_BLOCK_BREAK — không có victim/damage/isCrit đúng nghĩa combat,
     * thay vào đó truyền Block vừa bị actor phá. Dùng tên riêng (không trùng overload trigger())
     * để rõ ràng về ngữ nghĩa, tránh gọi nhầm signature combat cho 1 sự kiện không phải combat.
     */
    public void triggerBlockBreak(Player actor, org.bukkit.block.Block block) {
        if (actor == null || block == null) return;

        Set<String> passiveIds = getCachedPassiveIds(actor);
        if (passiveIds.isEmpty()) return;

        PassiveContext ctx = new PassiveContext(actor, null, 0, null, block);

        for (String pid : passiveIds) {
            PassiveDef def = registry.get(pid);
            if (def == null) continue;
            if (def.getTrigger() != PassiveTrigger.ON_BLOCK_BREAK) continue;
            // checkConditions() đọc victim để lọc target-type/HP-threshold — không áp dụng
            // cho ON_BLOCK_BREAK (không có "victim" theo nghĩa combat), nên bỏ qua, chỉ check
            // must-be-crit (luôn false ở đây, an toàn) và roll chance/cooldown như bình thường.
            if (def.isMustBeCrit()) continue; // ON_BLOCK_BREAK không có khái niệm crit
            if (!rollChance(def.getChance())) continue;
            if (isOnCooldown(actor.getUniqueId(), pid)) continue;

            for (PassiveMechanic m : def.getMechanics()) {
                m.execute(ctx);
            }

            applyCooldown(actor.getUniqueId(), pid, def.getCooldownSeconds());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Trả về passiveIds đã cache, tự build (lần đầu) nếu chưa có trong cache. */
    private Set<String> getCachedPassiveIds(Player player) {
        return passiveIdsCache.computeIfAbsent(player.getUniqueId(), uuid -> collectPassiveIds(player));
    }

    /**
     * Xoá cache passiveIds của 1 player — PHẢI gọi mỗi khi player đổi equipment
     * (cầm item khác, mặc/tháo giáp, click inventory...), tương tự cách CacheListener
     * gọi refreshCache() cho PlayerCombatCache. Xem PassiveCacheListener.
     */
    public void invalidatePassiveCache(UUID uuid) {
        passiveIdsCache.remove(uuid);
    }

    /** Lấy toàn bộ passive id từ tất cả equipment slots của player (quét thật, không cache) */
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

    private boolean checkConditions(PassiveDef def, Player actor, LivingEntity victim, boolean isCrit) {
        // must-be-crit
        if (def.isMustBeCrit() && !isCrit) return false;

        // 1. Kiểm tra bộ lọc loại Target-Type
        TargetType filter = def.getTargetType();

        if (filter == TargetType.SELF) {
            // SELF: chỉ pass khi victim CHÍNH LÀ actor (tự gây damage lên bản thân,
            // ví dụ fall/fire/lava tự gây — lúc đó EventDamage truyền victim=actor).
            // Không liên quan PLAYER/MOB, nên check riêng và return ngay, không rơi vào
            // nhánh PLAYER/MOB ở dưới.
            return victim != null && victim.equals(actor);
        }

        if (victim != null) {
            boolean isPlayer = victim instanceof Player;

            if (filter == TargetType.PLAYER && !isPlayer) return false; // Yêu cầu Player nhưng victim là Quái
            if (filter == TargetType.MOB && isPlayer) return false;    // Yêu cầu Quái nhưng victim là Player
        }

        // 2. Kiểm tra phần trăm máu (Hoạt động tốt cho cả Quái lẫn Player)
        double hpThreshold = def.getTargetHpPercentBelow();
        if (hpThreshold > 0 && victim != null) {
            if (victim.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                double maxHp = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                double pct = (victim.getHealth() / maxHp) * 100.0;
                if (pct >= hpThreshold) return false;
            }
        }
        return true;
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

    /** Xoá cooldown + cache passiveIds khi player quit */
    public void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
        passiveIdsCache.remove(uuid);
    }

    /** Trả về tên hiển thị để tooltip / debug */
    public Optional<PassiveDef> getDef(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    public Collection<PassiveDef> getAllDefs() { return registry.values(); }

    // ── Example yml ───────────────────────────────────────────────────────────
    private void writeExample(File folder) {
        File ex = new File(folder, "example_execute.yml");
        if (ex.exists()) return;
        String content =
                "# Passive mẫu: Tử Thần — instant kill khi HP victim dưới 5%\n" +
                        "id: execute_5pct\n" +
                        "display-name: \"&c[Tử Thần]\"\n" +
                        "trigger: ON_HIT\n" +
                        "condition:\n" +
                        "  target-hp-percent-below: 5\n" +
                        "  target-type: BOTH\n" +
                        "chance: 100\n" +
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