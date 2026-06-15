⚔️ RPG Stats PVP Plugin — by Thiện Dev


Bản quyền thuộc về: Thiện Dev
Không độc quyền source code, không tự nhận là tác giả. Mọi đóng góp & phát triển xin ghi rõ nguồn gốc.




📖 Giới thiệu

Plugin RPG Stats PVP là một hệ thống chiến đấu chuyên sâu cho Minecraft, hỗ trợ từ phiên bản 1.14 đến 1.21.x. Được xây dựng với thuật toán RAM Cache tối ưu hiệu năng, plugin giảm thiểu tối đa việc loop item mỗi lần đánh, giúp TPS ổn định ngay cả trên server đông player. Kết hợp với MyAttribute tạo nên hệ thống stats RPG PVP đa dạng, đầy đủ và cực kỳ mượt tay.


✨ Tính năng nổi bật


⚡ RAM Cache tối ưu — Stats được cache vào player khi join / đổi tay, EventDamage đọc trực tiếp từ cache thay vì loop lại item mỗi hit
🧬 Hệ thống Stats đa dạng — Dễ dàng mở rộng bằng cách tạo class mới trong folder stats/
💎 GemSocket System — Hệ thống socket đá quý gắn vào item, tăng stats linh hoạt
🤖 AI Integration — Folder AI/ hỗ trợ tích hợp AI để tạo item, logic boss, sự kiện tự động
🌐 Web API (Item Editor) — Liên kết với web editor để quản lý item từ xa, cập nhật version dễ dàng
🎮 Tương thích MyAttribute — Kết hợp hoàn hảo tạo lối chơi RPG PVP đa dạng
🔄 Đa phiên bản — Hỗ trợ từ 1.14 → 1.21.x



🗂️ Cấu trúc dự án

src/
├── stats/                        # Các class stats (mỗi stat = 1 class Java)
│   ├── StatAttack.java
│   ├── StatDefense.java
│   ├── StatCritChance.java
│   └── ...                       # Thêm stat mới tại đây
│
├── cache/
│   ├── PlayerCombatCache.java    # Nơi chứa biến stats của player (cache RAM)
│   └── CacheListener.java        # Lắng nghe sự kiện join/đổi tay → cập nhật cache
│
├── events/
│   └── EventDamage.java          # Xử lý toàn bộ logic PVP: damage, defense, crit...
│
├── gem/
│   └── GemSocket/
│       └── GemRemover.java       # ⚠️ Cần kiểm tra lỗi (xem mục Known Issues)
│
├── ai/
│   └── ...                       # Tích hợp AI: tạo item, logic mob, event tự động
│                                 # ⚠️ Cần tự điều chỉnh logic prompt cho phù hợp
│
└── webapi/
    └── ...                       # REST API liên kết với Web Item Editor
                                  # ⚠️ Cần tự dựng web (Node.js hoặc tương đương)


⚙️ Luồng hoạt động (Core Flow)

Player Join / Đổi tay
        │
        ▼
CacheListener.java
  └─ Đọc toàn bộ item đang cầm / mặc
  └─ Tính tổng stats (Attack, Defense, Crit,...)
  └─ Lưu vào PlayerCombatCache (RAM)
        │
        ▼
Player đánh nhau
        │
        ▼
EventDamage.java
  └─ Đọc stats từ PlayerCombatCache (không loop item → tiết kiệm TPS)
  └─ Xử lý logic: damage, giảm giáp, crit, lifesteal,...
  └─ Apply kết quả cuối lên sự kiện Damage


➕ Cách thêm Stats mới


Chỉ cần 3 bước đơn giản:



Bước 1 — Tạo class mới trong src/stats/:

java// Ví dụ: StatLifesteal.java
public class StatLifesteal {
    public static final String KEY = "lifesteal";
    // Logic đọc từ item NBT / lore
}

Bước 2 — Thêm biến vào PlayerCombatCache.java:

javapublic double lifesteal = 0;

Bước 3 — Vào CacheListener.java gán giá trị khi cache:

javacache.lifesteal += StatLifesteal.getFrom(item);

Bước 4 — Vào EventDamage.java xử lý logic:

javadouble heal = finalDamage * cache.lifesteal / 100;
attacker.setHealth(Math.min(attacker.getHealth() + heal, attacker.getMaxHealth()));


⚠️ Known Issues & TODO

🔴 GemSocket — GemRemover.java


Cần kiểm tra lại logic xóa gem khỏi item (có thể gây mất item hoặc lỗi NBT)
TODO: Validate slot trống trước khi remove, thêm cooldown tránh spam


🟡 AI Folder (/ai)


Các class AI chưa có prompt hoàn chỉnh
Dev cần tự điều chỉnh system prompt cho phù hợp với server của mình
Gợi ý: dùng OpenAI API hoặc Gemini để generate item description / boss script


🟡 Web API (/webapi)


Phần backend API chưa được implement hoàn chỉnh
Cần tự dựng web riêng — Gợi ý stack: Node.js (Express) + MongoDB / MySQL
API endpoints cần có:

GET /items — Lấy danh sách item
POST /items — Tạo item mới
PUT /items/:id — Cập nhật item
GET /version — Kiểm tra phiên bản plugin



Kết nối với plugin qua HTTP request trong Java (dùng HttpURLConnection hoặc OkHttp)


🟡 GemStone Stats — Chưa hỗ trợ % (phần trăm)


Hiện tại GemStone chỉ cộng giá trị flat (ví dụ +50 ATK)
Chưa hỗ trợ dạng +5% ATK, +3% CritDMG
Hướng xử lý gợi ý:


java// Trong GemStone parser — phân biệt flat vs percent
if (statStr.endsWith("%")) {
    double percent = Double.parseDouble(statStr.replace("%", ""));
    cache.attackPercent += percent;
} else {
    double flat = Double.parseDouble(statStr);
    cache.attackFlat += flat;
}
// Trong EventDamage.java
double finalAtk = (cache.attackFlat) * (1 + cache.attackPercent / 100);


🌐 Web API — Hướng dẫn tự dựng

Gợi ý stack Node.js + Express:

bashmkdir rpg-item-editor && cd rpg-item-editor
npm init -y
npm install express mongoose cors dotenv

Cấu trúc đơn giản:

rpg-item-editor/
├── routes/
│   ├── items.js       # CRUD item
│   └── version.js     # Version check
├── models/
│   └── Item.js        # Schema MongoDB
├── app.js
└── .env

Ví dụ endpoint version check:

jsrouter.get('/version', (req, res) => {
    res.json({ version: "1.0.5", changelog: "Fix gem socket bug" });
});


🤖 AI Integration — Hướng dẫn

Trong folder ai/, bạn có thể dùng AI để:


Auto-generate item lore & stats dựa trên tên item
Tạo script boss behavior động
Gợi ý build cho player dựa trên stats hiện tại


Ví dụ gọi OpenAI trong Java:

javaString prompt = "Tạo cho tôi một thanh kiếm huyền thoại tên: " + itemName + 
                ". Trả về JSON gồm: description, stats (attack, critChance, lifesteal).";
// Gọi API và parse JSON response


⚠️ Nhớ điều chỉnh prompt sao cho phù hợp với format item của server bạn!




📦 Cài đặt


Build plugin bằng Maven / Gradle
Bỏ file .jar vào thư mục plugins/
Khởi động server, cấu hình file config.yml
(Tuỳ chọn) Dựng Web API riêng và điền URL vào config



🔧 Yêu cầu

Thành phầnPhiên bảnMinecraft Server1.14 — 1.21.xJava11+MyAttributePhiên bản mới nhất(Tùy chọn) Node.js Web18+


🤝 Đóng góp

Pull Request luôn được chào đón! Khi fork hoặc sử dụng source:


Ghi rõ: "Dựa trên source của Thiện Dev"
Không xóa credit trong file header
Không tự nhận là tác giả gốc



📜 License

Source này không độc quyền — bạn được tự do sử dụng, chỉnh sửa, phát triển thêm với điều kiện giữ nguyên credit cho tác giả gốc: Thiện Dev.


<div align="center">
Made with ❤️ by Thiện Dev

"Stats đa dạng — PVP đã tay — TPS không tụt"

</div>
