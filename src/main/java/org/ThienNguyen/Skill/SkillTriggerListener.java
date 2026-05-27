package org.ThienNguyen.Skill;

import net.Indyuce.mmocore.api.player.PlayerData;
import org.ThienNguyen.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillTriggerListener implements Listener {

    
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    
    private final Map<UUID, Long> lastSneakTime = new HashMap<>();
    private static final long DOUBLE_SNEAK_THRESHOLD_MS = 500; 

    
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private static final long MESSAGE_DEBOUNCE_MS = 1000;

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (event.getDamager().hasMetadata("IS_ABILITY")) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        checkAndCast(player, victim, "HIT");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        boolean isSneaking = player.isSneaking();
        String trigger = "";

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            trigger = isSneaking ? "SHIFT_RIGHT" : "RIGHT_CLICK";
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            trigger = isSneaking ? "SHIFT_LEFT" : "LEFT_CLICK";
        }

        if (trigger.isEmpty()) return;
        checkAndCast(player, null, trigger);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return; 

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        long lastSneak = lastSneakTime.getOrDefault(uuid, 0L);

        
        if (now - lastSneak < DOUBLE_SNEAK_THRESHOLD_MS) {
            
            checkAndCast(player, null, "DOUBLE_SNEAK");
            
            lastSneakTime.put(uuid, 0L);
        } else {
            
            checkAndCast(player, null, "SNEAK");
            lastSneakTime.put(uuid, now);
        }
    }

    private void checkAndCast(Player player, LivingEntity target, String currentTrigger) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), "item_skills");
        String data = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) return;

        String[] allSkills = data.split(",");
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        
        boolean hasMMOCore = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MMOCore");

        
        PlayerData mmoPlayerData = hasMMOCore ? PlayerData.get(uuid) : null;

        Map<String, Long> playerCd = playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());

        for (String skillEntry : allSkills) {
            try {
                String[] parts = skillEntry.split(":");
                if (parts.length < 4) continue;

                String skillName = parts[0].trim();
                String savedTrigger = parts[1].trim();
                int cooldownTime = Integer.parseInt(parts[2].trim());
                int level = Integer.parseInt(parts[3].trim());

                if (!savedTrigger.equalsIgnoreCase(currentTrigger)) continue;

                
                int manaCost = SkillManager.getManaCost(skillName);

                if (hasMMOCore && mmoPlayerData != null && manaCost > 0) {
                    if (mmoPlayerData.getMana() < manaCost) {
                        String msg = Main.getInstance().getLangManager().getMessage(
                                "item.no-mana",
                                "{mana}", String.valueOf((int)mmoPlayerData.getMana()),
                                "{cost}", String.valueOf(manaCost)
                        );
                        sendDebouncedMessage(player, msg);
                        continue; 
                    }
                }

                
                String cdKey = skillName + "_" + savedTrigger.toUpperCase();
                Long endTime = playerCd.get(cdKey);
                if (endTime != null && endTime > now) continue;

                
                ISkill skill = SkillManager.getSkill(skillName);
                if (skill != null) {
                    
                    if (hasMMOCore && mmoPlayerData != null && manaCost > 0) {
                        mmoPlayerData.setMana(mmoPlayerData.getMana() - manaCost);
                    }

                    player.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    try {
                        double baseDamage = 1.0;
                        var attackAttribute = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
                        if (attackAttribute != null) {
                            baseDamage = attackAttribute.getValue();
                        }
                        skill.execute(player, target, level, baseDamage);
                    } finally {
                        player.removeMetadata("IS_ABILITY", Main.getInstance());
                    }

                    
                    playerCd.put(cdKey, now + (cooldownTime * 1000L));
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Lỗi thực thi skill: " + skillEntry);
            }
        }
    }

    private void sendDebouncedMessage(Player player, String message) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastMsg = lastMessageTime.getOrDefault(uuid, 0L);

        if (now - lastMsg >= MESSAGE_DEBOUNCE_MS) {
            player.sendMessage(message);
            lastMessageTime.put(uuid, now);
        }
    }
}