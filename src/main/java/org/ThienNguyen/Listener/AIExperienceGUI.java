package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AIExperienceGUI implements Listener {

    private static final String GUI_TITLE = "§0§lĐIỀU KHOẢN SỬ DỤNG AI";

    public static void openEulaGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta paperMeta = paper.getItemMeta();
        paperMeta.setDisplayName("§e§lChính Sách & Điều Khoản AI");
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§f 1. §7Không spawn prompt liên tục nếu bạn dùng API của plugins");
        lore.add("§f 2. §7Không nhập các nội dung toxic hoặc phá hoại.");
        lore.add("§f 3. §7Dữ liệu item của bạn tạo ra sẽ được gửi về server của author.");
        lore.add("§f 4. §7Nên sử dụng API cá nhân của bạn vì API plugins là API công cộng dễ quá tải");
        lore.add("§f 5. §7Vi một cộng đồng lành mạnh");
        lore.add("§f 6. §7AI có thể mắc sai lầm, sửa item AI tạo trong AI/Item.yml nhé");
        lore.add("§f 7. §7Tác giả:§c Nhà văn viết code");
        lore.add("§7");
        lore.add("§b» Hãy cân nhắc trước khi đồng ý.");
        paperMeta.setLore(lore);
        paper.setItemMeta(paperMeta);

        
        ItemStack accept = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta aMeta = accept.getItemMeta();
        aMeta.setDisplayName("§a§l[ CHẤP NHẬN ]");
        List<String> aLore = new ArrayList<>();
        aLore.add("§7Click để đồng ý và bắt đầu sử dụng.");
        aMeta.setLore(aLore);
        accept.setItemMeta(aMeta);

        
        ItemStack decline = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta dMeta = decline.getItemMeta();
        dMeta.setDisplayName("§c§l[ TỪ CHỐI ]");
        List<String> dLore = new ArrayList<>();
        dLore.add("§7Hủy bỏ và quay lại.");
        dMeta.setLore(dLore);
        decline.setItemMeta(dMeta);

        
        ItemStack gray = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = gray.getItemMeta();
        gMeta.setDisplayName(" ");
        gray.setItemMeta(gMeta);

        for (int i = 0; i < 27; i++) gui.setItem(i, gray);

        gui.setItem(13, paper);
        gui.setItem(11, accept);
        gui.setItem(15, decline);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        if (slot == 11) { 
            setEulaStatus(player, true);
            player.closeInventory();
            player.sendMessage("§a§l✔ §7Cảm ơn bạn đã chấp nhận điều khoản. Bây giờ bạn có thể dùng lệnh AI!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } else if (slot == 15) { 
            player.closeInventory();
            player.sendMessage("§c§l✘ §7Bạn đã từ chối điều khoản. Không thể sử dụng tính năng AI.");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }
    }

    
    public static boolean hasAccepted(Player player) {
        File file = new File(Main.getInstance().getDataFolder() + "/AI", "eula.yml");
        if (!file.exists()) return false;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        return config.getBoolean("accepted." + player.getUniqueId(), false);
    }

    
    public static void setEulaStatus(Player player, boolean status) {
        File folder = new File(Main.getInstance().getDataFolder(), "AI");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, "eula.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("accepted." + player.getUniqueId(), status);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}