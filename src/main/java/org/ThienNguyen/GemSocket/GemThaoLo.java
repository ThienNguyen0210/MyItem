package org.ThienNguyen.GemSocket;

import org.ThienNguyen.Main;
import org.ThienNguyen.Lore.LoreGenerator;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GemThaoLo ("socket remover"): a new item type that lets a player "re-drill"
 * a previously drilled socket — it deletes the empty socket (EMPTY_<type>)
 * from the item entirely, along with its matching lore line.
 *
 * Unlike GemRemover (which pulls a GEM out of a filled socket and returns
 * the gem, leaving the socket behind as EMPTY_<type>), this item removes the
 * socket itself — the item ends up with one fewer socket after use. Because
 * of that, it only ever acts on a socket that is currently EMPTY. If the
 * socket already holds a gem, this item ignores it completely — the gem
 * must be removed first with GemRemover before the now-empty socket can be
 * taken out.
 *
 * Configured in Gem.yml the same way as REMOVER items: each item of this
 * type can only remove sockets of ONE specific rarity (read from "type"),
 * or "ANY" to remove any empty socket regardless of rarity. Applied via
 * drag-and-drop (SWAP_WITH_CURSOR), same as every other tool item in this
 * system.
 */
public class GemThaoLo implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onSocketRemoverApply(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;

        ItemStack removerItem = event.getCursor();
        ItemStack targetItem = event.getCurrentItem();

        if (removerItem == null || targetItem == null || targetItem.getType() == Material.AIR) return;

        ItemMeta removerMeta = removerItem.getItemMeta();
        if (removerMeta == null) return;

        NamespacedKey typeKey = new NamespacedKey(Main.getInstance(), "gem_item_type");
        String itemType = removerMeta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

        // Its own item type — separate from REMOVER (removes a gem) and DRILL (adds a socket).
        if (itemType == null || !itemType.equals("SOCKET_REMOVER")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        var lang = Main.getInstance().getLangManager();

        if (targetItem.getAmount() >= 2) {
            player.sendMessage(lang.getMessage("item.no-stack-allowed"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return;
        }

        NamespacedKey idKey = new NamespacedKey(Main.getInstance(), "gem_item_id");
        String removerId = removerMeta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);

        // Socket-remover tool items are defined in Tools.yml (split out of the old Gem.yml).
        FileConfiguration toolsConfig = Main.getInstance().getGemToolsConfig();
        if (removerId == null || !toolsConfig.contains(removerId)) return;

        String targetType = toolsConfig.contains(removerId + ".type")
                ? toolsConfig.getString(removerId + ".type")
                : null;

        if (targetType == null) {
            if (Main.getInstance().isGemDebugEnabled()) {
                Main.getInstance().getLogger().warning(
                        "[MyItem] Socket remover '" + removerId + "' is missing 'type' in Tools.yml — unclear which socket rarity it should remove.");
            }
            player.sendMessage(lang.getMessage("item.socket-remover-misconfigured"));
            return;
        }

        ItemMeta targetMeta = targetItem.getItemMeta();
        if (targetMeta == null) return;

        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String socketData = targetMeta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);

        if (socketData == null || socketData.isEmpty()) {
            player.sendMessage(lang.getMessage("item.no-sockets-found"));
            return;
        }

        String[] sockets = socketData.split("\\|");

        // Only target EMPTY sockets (EMPTY_<type>) — a socket that already
        // holds a gem is skipped entirely, matching the requirement that
        // this item cannot be used on a filled socket.
        List<Integer> matchingIndexes = new ArrayList<>();
        for (int i = 0; i < sockets.length; i++) {
            if (!sockets[i].startsWith("EMPTY_")) continue;
            String socketType = sockets[i].substring("EMPTY_".length());
            if (targetType.equalsIgnoreCase("ANY") || socketType.equalsIgnoreCase(targetType)) {
                matchingIndexes.add(i);
            }
        }

        if (matchingIndexes.isEmpty()) {
            if (targetType.equalsIgnoreCase("ANY")) {
                player.sendMessage(lang.getMessage("item.no-empty-socket-remove-any"));
            } else {
                player.sendMessage(lang.getMessage("item.no-empty-socket-remove-type", "{type}", targetType));
            }
            return;
        }

        int removeIndex = matchingIndexes.get(random.nextInt(matchingIndexes.size()));
        String removedSocketType = sockets[removeIndex].substring("EMPTY_".length());

        if (removeSocketFromItem(targetItem, removeIndex, removedSocketType)) {
            player.sendMessage(lang.getMessage("item.socket-remove-success", "{type}", removedSocketType));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            new org.ThienNguyen.Listener.CacheListener().refreshCache(player);
        } else {
            player.sendMessage(lang.getMessage("item.socket-remove-failed"));
            return;
        }

        removerItem.setAmount(removerItem.getAmount() - 1);
    }

    /**
     * Removes one empty socket from item_sockets by INDEX (avoids removing
     * the wrong socket if the item has multiple empty sockets of the same
     * type), and deletes the matching lore line — both updates are applied
     * to the SAME ItemMeta and committed once (matches the fix documented
     * in GemRemover, which avoided overwriting lore via a double setItemMeta).
     */
    private boolean removeSocketFromItem(ItemStack item, int socketIndex, String socketType) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String currentData = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);
        if (currentData == null || currentData.isEmpty()) return false;

        String[] sockets = currentData.split("\\|");
        if (socketIndex < 0 || socketIndex >= sockets.length) return false;

        List<String> remaining = new ArrayList<>();
        for (int i = 0; i < sockets.length; i++) {
            if (i != socketIndex) remaining.add(sockets[i]);
        }
        String newData = String.join("|", remaining);
        meta.getPersistentDataContainer().set(socketKey, PersistentDataType.STRING, newData);

        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");
        String formatId = meta.getPersistentDataContainer().get(formatKey, PersistentDataType.STRING);

        if (formatId != null && !formatId.isEmpty()) {
            // Item uses dynamic lore (LoreGenerator) — commit the PDC first,
            // then rebuild the whole lore from the new socket data instead
            // of manually deleting a line.
            item.setItemMeta(meta);
            LoreGenerator.rebuild(item);
        } else {
            removeEmptySocketLoreLine(meta, socketType);
            item.setItemMeta(meta);
        }

        return true;
    }

    /**
     * Deletes the lore line for the empty socket that was removed, based on
     * the real format string from GemType.yml.
     *
     * Uses CONTAINS matching, not exact-line equality: if the lore line has
     * extra characters/whitespace/prefix wrapped around the base format
     * (e.g. the format is "Empty Socket" but the actual line is "dsadsa Empty
     * Socketdassa"), it must still be detected and the ENTIRE LINE deleted —
     * same approach GemKham uses (originalLine.contains(...)) when socketing
     * a gem, just applied in reverse (delete instead of replace).
     * lore.remove(i) removes the element outright, so the total lore line
     * count drops by 1 (e.g. 18 -> 17) with nothing left behind.
     */
    private void removeEmptySocketLoreLine(ItemMeta meta, String socketType) {
        if (!meta.hasLore()) return;

        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();
        String emptyFormat = typeConfig.getString(socketType + ".format", "&7[ ○ ] Empty Socket");
        String coloredEmpty = ChatColor.translateAlternateColorCodes('&', emptyFormat);

        List<String> lore = new ArrayList<>(meta.getLore());
        boolean removed = false;

        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(coloredEmpty)) {
                lore.remove(i);
                removed = true;
                break;
            }
        }

        // Fallback: compare with color codes stripped, in case color codes
        // drifted but the underlying text is the same (mirrors the fallback
        // in GemRemover) — still uses contains() to catch a line with extra
        // characters around it.
        if (!removed) {
            String strippedEmpty = ChatColor.stripColor(coloredEmpty);
            for (int i = 0; i < lore.size(); i++) {
                if (ChatColor.stripColor(lore.get(i)).contains(strippedEmpty)) {
                    lore.remove(i);
                    removed = true;
                    break;
                }
            }
        }

        if (removed) {
            meta.setLore(lore);
        } else if (Main.getInstance().isGemDebugEnabled()) {
            Main.getInstance().getLogger().warning(
                    "[MyItem] Could not find the lore line for empty socket type '" + socketType
                            + "' to remove — lore may be out of sync with item_sockets.");
        }
    }
}