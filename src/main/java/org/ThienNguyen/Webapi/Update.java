package org.ThienNguyen.Webapi;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Material;
import java.net.URL;
import java.util.Scanner;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Update {

    private static final String DOWNLOAD_URL_TEMPLATE = "http://103.188.83.137/api/downloads/MyItem-%s.jar";
    public static NamespacedKey GUI_KEY;

    // GUI 1: Thông tin phiên bản hiện tại
    public static void openVersionGUI(Player player, Main plugin) {
        GUI_KEY = new NamespacedKey(plugin, "update_gui_locked");
        Inventory gui = Bukkit.createInventory(null, 27, "§0MyItem - Plugin Version");

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName("§b§lThông tin phiên bản");
        List<String> lore = new ArrayList<>();
        lore.add("§fPhiên bản hiện tại: §a" + plugin.getDescription().getVersion());
        lore.add("§fTác giả: §eThienNguyen");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(GUI_KEY, PersistentDataType.BYTE, (byte) 1);
        info.setItemMeta(meta);

        ItemStack updateIcon = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta upMeta = updateIcon.getItemMeta();
        upMeta.setDisplayName("§6§lCập nhật bản mới nhất");
        List<String> upLore = new ArrayList<>();
        upLore.add("§7Click để xem danh sách phiên bản");
        upLore.add("§7Tên file sẽ tải: §f" + getJarName(plugin));
        upMeta.setLore(upLore);
        upMeta.getPersistentDataContainer().set(GUI_KEY, PersistentDataType.BYTE, (byte) 1);
        // Đánh dấu hành động mở List
        upMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action_open_list"), PersistentDataType.BYTE, (byte) 1);
        updateIcon.setItemMeta(upMeta);

        gui.setItem(13, info);
        gui.setItem(26, updateIcon);
        player.openInventory(gui);
    }

    public static void openUpdateListGUI(Player player, Main plugin) {
        player.sendMessage("§e[MyItem] Đang kết nối tới máy chủ cập nhật...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("http://103.188.83.137/api/list-versions");
                StringBuilder jsonContent = new StringBuilder();
                try (Scanner s = new Scanner(url.openStream(), "UTF-8")) {
                    while (s.hasNextLine()) jsonContent.append(s.nextLine());
                }

                String data = jsonContent.toString();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    // SỬA: Đổi kích thước thành 54 ô
                    Inventory gui = Bukkit.createInventory(null, 54, "§0MyItem - Danh sách Update");

                    try {
                        String cleanData = data.replace("\r", "").replace("\n", "").trim();
                        int startArr = cleanData.indexOf("[");
                        int endArr = cleanData.lastIndexOf("]");

                        if (startArr != -1 && endArr != -1) {
                            String versionsPart = cleanData.substring(startArr + 1, endArr);
                            String[] objects = versionsPart.split("\\}( ?), ?\\{");

                            // Bắt đầu từ slot 10 (hàng 2)
                            int slot = 10;
                            for (String obj : objects) {
                                if (slot >= 44) break; // Giới hạn không đè vào hàng cuối

                                String jsonObject = obj;
                                if (!jsonObject.startsWith("{")) jsonObject = "{" + jsonObject;
                                if (!jsonObject.endsWith("}")) jsonObject = jsonObject + "}";

                                String ver = extractValue(jsonObject, "ver");
                                String date = extractValue(jsonObject, "date");
                                String changelog = extractValue(jsonObject, "changelog");

                                ItemStack item = new ItemStack(Material.CLOCK);
                                ItemMeta meta = item.getItemMeta();
                                meta.setDisplayName("§a§lPhiên bản: §e" + ver);

                                List<String> lore = new ArrayList<>();
                                lore.add("§fNgày: §e" + date);
                                lore.add("§fGhi chú:");

                                String[] lines = changelog.split("\\||\n|\\\\n");
                                for (String line : lines) {
                                    if (!line.trim().isEmpty()) {
                                        lore.add("  §7- " + org.bukkit.ChatColor.translateAlternateColorCodes('&', line.trim()));
                                    }
                                }

                                lore.add("");
                                lore.add("§a▶ Click để tải MyItem/Update");
                                meta.setLore(lore);
                                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "update_ver"), PersistentDataType.STRING, ver);
                                item.setItemMeta(meta);

                                gui.setItem(slot++, item);
                                // Nhảy hàng nếu chạm mép phải (slot 17 -> 19)
                                if (slot % 9 == 8) slot += 2;
                            }
                        }

                        // --- THÊM Ô DONATE Ở CUỐI GUI (SLOT 53) ---
                        ItemStack donate = new ItemStack(Material.PAPER);
                        ItemMeta dMeta = donate.getItemMeta();
                        dMeta.setDisplayName("§d§l❤ DONATE AUTHOR §d§l❤");
                        List<String> dLore = new ArrayList<>();
                        dLore.add("§7Nếu bạn yêu thích plugin, hãy ủng hộ tác giả!");
                        dLore.add("");
                        dLore.add("§fNgân hàng: §e§lTECHCOMBANK");
                        dLore.add("§fSTK: §b§l5089206015240");
                        dLore.add("§fChủ tài khoản: §e§lNGUYEN CHI THIEN");
                        dLore.add("");
                        dLore.add("§d§oCảm ơn sự đóng góp của bạn!");
                        dMeta.setLore(dLore);
                        // Đánh dấu để không cho lấy item ra khỏi GUI
                        dMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "update_gui_locked"), PersistentDataType.BYTE, (byte) 1);
                        donate.setItemMeta(dMeta);
                        gui.setItem(53, donate);
                        // -----------------------------------------

                    } catch (Exception ex) {
                        player.sendMessage("§c[MyItem] Lỗi hiển thị: JSON không đúng định dạng!");
                    }
                    player.openInventory(gui);
                });

            } catch (Exception e) {
                player.sendMessage("§c[MyItem] Lỗi khi lấy danh sách: " + e.getMessage());
            }
        });
    }

    private static String extractValue(String input, String key) {
        try {
            String pattern = "\"" + key + "\":\"";
            int start = input.indexOf(pattern);
            if (start == -1) return "Không rõ";

            start += pattern.length();
            int end = input.indexOf("\"", start);
            return input.substring(start, end);
        } catch (Exception e) {
            return "Lỗi";
        }
    }
    // Sửa lại hàm này: Thêm tham số String version
    public static void downloadAndUpdate(Main plugin, String version, Player player) {
        String downloadUrl = String.format(DOWNLOAD_URL_TEMPLATE, version);
        player.sendMessage("§e[MyItem] Đang tiến hành tải bản " + version + "...");

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        // TRUYỀN version vào hàm save
                        saveToUpdateFolder(plugin, response.body(), player, version);
                    } else {
                        player.sendMessage("§c[MyItem] Không thể tải! Lỗi API: " + response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    player.sendMessage("§c[MyItem] Lỗi kết nối: " + ex.getMessage());
                    return null;
                });
    }

    // Sửa lại hàm này: Đổi tên file theo version được truyền vào
    private static void saveToUpdateFolder(Main plugin, InputStream inputStream, Player player, String version) {
        try {
            // TỰ ĐỊNH NGHĨA TÊN FILE THEO VERSION CLICK TRÊN GUI
            String jarName = "MyItem-" + version + ".jar";

            File updateFolder = new File(plugin.getDataFolder(), "Update");
            if (!updateFolder.exists()) updateFolder.mkdirs();

            File targetFile = new File(updateFolder, jarName);

            // Luồng dữ liệu (InputStream) từ Web sẽ được ghi trực tiếp vào file có tên MyItem-{version}.jar
            Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            player.sendMessage("");
            player.sendMessage("§b§l[MyItem] §a§lTẢI THÀNH CÔNG!");
            player.sendMessage("§fPhiên bản: §e" + version);
            player.sendMessage("§fFile: §e" + jarName);
            player.sendMessage("§fVị trí: §7/plugins/MyItem/Update/");
            player.sendMessage("§c§nLưu ý:§f Hãy copy đè vào folder plugins và Restart.");
            player.sendMessage("");

        } catch (Exception e) {
            player.sendMessage("§c[MyItem] Lỗi khi lưu file: " + e.getMessage());
        }
    }



    private static String getJarName(Main plugin) {
        try {
            File file = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            String fileName = file.getName();
            if (file.getPath().contains(".paper-remapped") || fileName.matches(".*-\\d{10,}.*")) {
                return plugin.getName() + "-" + plugin.getDescription().getVersion() + ".jar";
            }
            return fileName;
        } catch (Exception e) {
            return "MyItem-1.0-SNAPSHOT.jar";
        }
    }
}