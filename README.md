💎 MyItem Core - Advanced RPG Stats & Combat System
MyItem là hệ thống RPG Combat cao cấp, được tối ưu hóa cho các máy chủ Minecraft hiệu năng cao (Paper/Folia). Với cơ chế RAM Cache tiên tiến, dự án loại bỏ hoàn toàn tình trạng tụt TPS khi xử lý tính toán sát thương phức tạp, cho phép mở rộng hệ thống stats không giới hạn.

🚀 Đặc điểm kỹ thuật
Hiệu năng vượt trội: Sử dụng kiến trúc Cache-Driven, toàn bộ dữ liệu người chơi được lưu trong cache, loại bỏ việc lặp (loop) qua các item mỗi khi gây sát thương.

Hỗ trợ đa phiên bản: Tương thích hoàn hảo từ 1.14 đến 1.21.x.

Khả năng mở rộng: Hệ thống stats theo modular, dễ dàng thêm mới các chỉ số RPG chỉ với 3 bước đơn giản.

Tích hợp: Hỗ trợ sâu với MyAttribute để xây dựng lối chơi RPG/PVP chuyên sâu.

⚙️ Luồng hoạt động (Architecture)
Hệ thống vận hành dựa trên cơ chế đồng bộ dữ liệu thông minh:

CacheListener.java: Lắng nghe các sự kiện (Join, Swap Hand, Equip), tự động gán và cập nhật stats vào bộ nhớ đệm của người chơi.

EventDamage.java: Trái tim của hệ thống PVP. Thay vì truy vấn item, nó đọc trực tiếp từ cache đã được nạp sẵn, đảm bảo tốc độ phản hồi tức thì.

WEBAPI (Coming Soon): Cung cấp endpoint kết nối với Dashboard Web, cho phép quản lý Item Editor và update phiên bản từ xa.

🛠️ Hướng dẫn phát triển (Developer Guide)
Để thêm một chỉ số (stats) mới, bạn thực hiện quy trình sau:

Create: Tạo Class Java mới trong thư mục stats/.

Register: Khai báo biến stats mới trong PlayerCombatCache.

Update: Cập nhật logic thu thập dữ liệu trong CacheListener.java.

Logic: Xử lý tính toán công thức tại EventDamage.java.

Lưu ý: Hệ thống hiện tại đang trong quá trình phát triển (WIP). Cộng đồng dev cần chú ý kiểm tra logic tại GemRemover.java và các module AI để tối ưu hóa hành vi thực tế. Hệ thống Gemstone hiện chưa hỗ trợ tính toán theo phần trăm (%), đây là cơ hội để bạn đóng góp code cải tiến.

📜 Thông tin bản quyền
Tác giả: Thiện Dev (ThienNguyen)

Giấy phép: Dự án này là mã nguồn mở, phi độc quyền. Bạn được phép thoải mái tùy chỉnh, cải tiến và sử dụng cho mục đích cá nhân hoặc server.

Yêu cầu: Vui lòng giữ nguyên credit nguồn gốc khi chia sẻ hoặc phát triển tiếp.

MyItem - Xây dựng trải nghiệm PVP hoàn hảo cho máy chủ của bạn.
