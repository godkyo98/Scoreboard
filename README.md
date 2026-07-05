# 🏆 Kyo Scoreboard & TabList
**Một siêu phẩm Scoreboard và TabList 100% Server-side dành cho Minecraft Fabric (Phiên bản 26.2+). Được thiết kế và tối ưu riêng cho TEA Server.**

![Fabric](https://img.shields.io/badge/Modloader-Fabric-lightgray?style=for-the-badge&logo=fabric)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x_|_26.2-success?style=for-the-badge&logo=minecraft)
![License](https://img.shields.io/badge/License-CC0-blue?style=for-the-badge)

Kyo Scoreboard không chỉ là một mod hiển thị bảng điểm thông thường. Đây là một hệ thống được lập trình lại từ đầu với kiến trúc **Gửi gói tin cá nhân (Packet-based)**, loại bỏ hoàn toàn hiện tượng nhấp nháy (Anti-Flicker) và rác bộ nhớ (Memory Leak). Kết hợp sức mạnh của **Placeholder API V3**, mod hỗ trợ hoàn hảo hệ màu Hex, thẻ định dạng STF và biểu tượng Custom vô hạn!

---

## ✨ Tính Năng Nổi Bật

- 🚀 **100% Server-side:** Người chơi không cần cài đặt bất kỳ mod nào ở client. Chỉ cần ném vào thư mục `mods` của server là chạy.
- ⚡ **Tối Ưu Hiệu Năng Tuyệt Đối:** Vòng lặp cập nhật thông minh (1 giây/lần cho Scoreboard, nửa giây/lần cho Animation Tablist). Không dùng hàm Vanilla gây lag, cập nhật dữ liệu ngầm cực mượt.
- 🎨 **Sức Mạnh Placeholder API V3:** - Hỗ trợ kiến trúc xếp chồng (Stacking Parsers) đỉnh cao.
    - Render mượt mà màu Hex (`<#FF3300>`), thẻ in đậm (`<bold>`), và nội suy biến động.
- 🎬 **TabList Animation:** Hỗ trợ tạo khung hình (frames) chuyển động rực rỡ cho tiêu đề (Header) của Tablist.
- 👑 **Hệ Thống Thời Gian Chơi Thông Minh (Progressive Tier RPG):**
    - Tự động rút gọn đơn vị thông minh (VD: `7d 10h 11m` -> `7d 10h`, `1y 2mo`).
    - Đổi màu và thêm Huy Hiệu (Badges) tự động dựa trên số ngày cày cuốc (Tân binh -> Thần thoại).

---

## 📦 Yêu Cầu Cài Đặt (Dependencies)

Để Kyo Scoreboard hoạt động hoàn hảo, Server cần cài đặt sẵn các mod sau:
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Placeholder API](https://modrinth.com/mod/placeholder-api) (Phiên bản V3 trở lên)
- *(Tùy chọn)* [Kyo Economy](#) - Để liên kết hệ thống tiền tệ.

---

## ⚙️ Cấu Hình (Configuration)

Lần đầu chạy server, mod sẽ tự động sinh ra file cấu hình tại `config/kyo_scoreboard.json`. Sếp có thể tự do chỉnh sửa bảng điểm theo ý muốn.

**Ví dụ cấu hình (JSON):**
```json
{
  "animatedTitle": [
    "<bold><#FF3300>❖ TEA SERVER ❖</bold>",
    "<bold><#FF6600>❖ TEA SERVER ❖</bold>",
    "<bold><#FF9900>❖ TEA SERVER ❖</bold>"
  ],
  "animatedTabHeader": [
    "<bold><aqua>Chào mừng đến với TEA SERVER!</aqua></bold>"
  ],
  "tabFooter": "<gray>Ping của bạn: %player:ping% ms</gray>",
  "boardLines": [
    " ",
    "<bold><white>👤 Thông Tin</white></bold>",
    " <gray>Tên:</gray> %player:name%",
    " <gray>Chơi:</gray> %kyoscoreboard:playtime%",
    " ",
    "<bold><white>💰 Kinh Tế</white></bold>",
    " <gray>Số dư:</gray> <gold>%kyoeconomy:balance% Xu</gold>"
  ]
}
```
👨‍💻 Lời Cảm Ơn
Được phát triển và tối ưu dành riêng cho TEA Server.
Cảm ơn thư viện Placeholder API (Patbox) vì hệ thống Node Parser cực kỳ linh hoạt!