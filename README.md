# 💎 MyItem Core — Advanced RPG Stats & Combat System

**MyItem** là hệ thống RPG Combat cao cấp cho Minecraft, được tối ưu hóa riêng cho các máy chủ hiệu năng cao chạy **Paper/Folia**. Nhờ cơ chế **RAM Cache** tiên tiến, plugin loại bỏ hoàn toàn tình trạng tụt TPS khi xử lý các công thức sát thương phức tạp, cho phép mở rộng hệ thống stats gần như không giới hạn.

---

## 🚀 Đặc điểm kỹ thuật

| Tính năng | Mô tả |
|---|---|
| **Hiệu năng vượt trội** | Kiến trúc **Cache-Driven** — toàn bộ dữ liệu người chơi được lưu trong cache, loại bỏ hoàn toàn việc lặp (loop) qua từng item mỗi khi tính sát thương. |
| **Hỗ trợ đa phiên bản** | Tương thích từ **Minecraft 1.14 → 1.21.x**. |
| **Khả năng mở rộng** | Hệ thống stats thiết kế theo hướng **modular** — thêm chỉ số RPG mới chỉ với 3 bước đơn giản. |
| **Tích hợp sâu** | Hỗ trợ tích hợp với **MyAttribute** để xây dựng lối chơi RPG/PVP chuyên sâu. |

---

## ⚙️ Luồng hoạt động (Architecture)

Hệ thống vận hành dựa trên cơ chế đồng bộ dữ liệu thông minh, gồm 3 thành phần chính:

- **`CacheListener.java`**
  Lắng nghe các sự kiện gameplay (Join, Swap Hand, Equip...), tự động gán và cập nhật stats của người chơi vào bộ nhớ đệm (cache).

- **`EventDamage.java`**
  Trái tim của hệ thống PVP. Thay vì truy vấn lại item mỗi lần đánh, module này đọc trực tiếp từ cache đã được nạp sẵn — đảm bảo tốc độ phản hồi tức thì, không gây lag dù số lượng người chơi lớn.

- **WEBAPI** *(Coming Soon)*
  Cung cấp endpoint kết nối với Dashboard Web, cho phép quản lý Item Editor và cập nhật phiên bản từ xa.

```
Player Action (Join/Swap/Equip)
        │
        ▼
  CacheListener.java  ──►  PlayerCombatCache (RAM)
                                  │
                                  ▼
                          EventDamage.java
                          (đọc cache, tính damage)
```

---

## 🛠️ Hướng dẫn phát triển (Developer Guide)

Để thêm một chỉ số (stat) RPG mới vào hệ thống, thực hiện theo quy trình 4 bước:

1. **Create** — Tạo class Java mới trong thư mục `stats/`.
2. **Register** — Khai báo biến stat mới trong `PlayerCombatCache`.
3. **Update** — Cập nhật logic thu thập dữ liệu trong `CacheListener.java`.
4. **Logic** — Xử lý công thức tính toán tại `EventDamage.java`.

---

## ⚠️ Lưu ý cho contributor (WIP)

Hệ thống hiện đang trong giai đoạn phát triển (**Work In Progress**). Một số điểm cần lưu ý khi đóng góp code:

- Cần kiểm tra kỹ logic tại **`GemRemover.java`** và các module AI liên quan để tối ưu hành vi thực tế trong game.
- Hệ thống **Gemstone** hiện **chưa hỗ trợ tính toán theo phần trăm (%)** — đây là một hạng mục đang mở, rất hoan nghênh PR cải tiến.

---

## 📜 Thông tin bản quyền

- **Tác giả:** Thiện Dev (ThienNguyen)
- **Giấy phép:** Mã nguồn mở, phi độc quyền — được phép tự do tùy chỉnh, cải tiến và sử dụng cho mục đích cá nhân hoặc server.
- **Yêu cầu duy nhất:** Giữ nguyên credit nguồn gốc khi chia sẻ hoặc phát triển tiếp dự án.

---

<p align="center"><i>MyItem — Xây dựng trải nghiệm PVP hoàn hảo cho máy chủ của bạn.</i></p>
