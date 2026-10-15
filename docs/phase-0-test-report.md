# Phase 0 — Test Checklist & Report

Checklist 20 case cho test 2 thiết bị (Editor + Android, cùng Wi-Fi), kèm kết quả test tự động
và load test đã chạy. Cột **Kết quả** để điền khi test tay.

---

## 1. Test tay 2 thiết bị (20 case)

Chuẩn bị: server chạy trên PC, port 8080 mở trong Windows Firewall. Thiết bị A = Unity Editor
(`http://localhost:8080`), thiết bị B = điện thoại (`http://<IP LAN PC>:8080`). Hai tài khoản khác nhau.

| # | Nhóm | Bước | Kỳ vọng | Kết quả |
|---|------|------|---------|---------|
| 1 | Auth | B: đăng ký email mới | Vào game, có màn tạo nhân vật | |
| 2 | Auth | B: đăng nhập sai mật khẩu | "Sai email hoặc mật khẩu" | |
| 3 | Auth | B: sai mật khẩu 6 lần trong 1 phút | Lần 6: "Thao tác quá nhanh" (429) | |
| 4 | Auth | B: tạo nhân vật trùng tên với A | "Tên nhân vật đã có người dùng" | |
| 5 | Instance | A rồi B vào game | Cùng phòng, mỗi bên thấy capsule cam của người kia + tên | |
| 6 | Movement | B đi bằng joystick | A thấy B di chuyển mượt (trễ ~0,2 s), không giật | |
| 7 | Movement | A, B đi liên tục 1 phút | `Corrections` ≈ 0 ở cả hai | |
| 8 | Movement | Đi vào tường biên | Dừng ở biên ±20 m, không xuyên | |
| 9 | Combat | A đến gần Goblin Grunt | Grunt lao tới đánh, HP A giảm, số đỏ bay lên | |
| 10 | Combat | A bấm Đánh | Vòng vàng dưới Grunt, số trắng bay lên mỗi giây | |
| 11 | Combat | A bấm Charge 2 lần liên tiếp | Lần 1: 58 dmg; lần 2: nút xám, đếm ngược 8 s | |
| 12 | Combat | B nhìn A đánh quái | B thấy HP bar quái giảm, số damage, quái chết xám | |
| 13 | Loot | A giết quái một mình | Chỉ A có thông báo loot + gold, B không (personal loot) | |
| 14 | Loot | A và B cùng đánh 1 con | Cả hai đều có loot riêng | |
| 15 | Loot | Mở Túi đồ | Đồ vừa nhặt có trong danh sách, gold đúng | |
| 16 | Persist | A dừng Play, Play lại, đăng nhập | Túi đồ + gold còn nguyên | |
| 17 | Persist | A rời phòng khi HP < max, vào lại | HP giữ nguyên giá trị lúc rời | |
| 18 | Death | Đứng yên cho quái đánh đến chết | Xám, "Bạn đã gục", 5 s sau hồi sinh ở điểm xuất phát đầy HP | |
| 19 | Reconnect | B tắt Wi-Fi 10–20 s rồi bật lại | B tự kết nối lại, về đúng phòng; A thấy B vẫn ở đó | |
| 20 | Session | Đăng nhập tài khoản A trên thiết bị B | A bị đẩy ra ("Bị ngắt kết nối"), B vào nhân vật của A | |

---

## 2. Test tự động (server)

`cd server && .\mvnw.cmd test` — **82 test, 0 fail** (2026-10-15).

| Nhóm | Bao phủ |
|------|---------|
| Auth | Login 5/phút/IP+email → 429; tài khoản khác không bị khóa |
| WebSocket | JWT handshake, envelope, seq trùng, ERROR, KICK, 50 msg/s → RATE_LIMITED, flood → đóng kết nối |
| Session | Reconnect trong 60 s giữ session, hết hạn → rời phòng, idle 30 s |
| Instance | 2 người cùng phòng, spawn/despawn, speed hack bị sửa, packet dồn do jitter không bị sửa, reconnect gửi lại snapshot |
| Combat | Công thức damage TDD §8.2, auto-attack, Charge (cooldown / MP / tầm / rate limit), mob FSM (aggro, chase, attack, leash + hồi máu), chết / hồi sinh |
| Loot | Chỉ người gây damage được thưởng, roll theo trọng số, túi 40 ô (stack, đầy → từ chối) |
| Vitals | HP/MP lưu khi rời phòng, nạp lại khi vào |

Đã chạy thử với PostgreSQL thật (không có trong test tự động vì profile test không có DB):
loot vào `inventory_items`, `currency_audit_log` +10 gold, `item_audit_log` CREATE, insert trùng
`mob_kill_rewards` → 0 dòng (idempotent), `GET /inventory` nhân vật người khác → 404, HP lưu/nạp lại.

---

## 3. Load test & latency

Bot Java (mỗi bot: đăng ký, vào phòng, đi tới quái, auto-attack + Charge, PING mỗi 5 s, ACK loot).
Server `local-native` trên PC dev (Windows, PostgreSQL local), client bot cùng máy.

| Bots | Thời gian | Phòng | Mất kết nối | Lỗi | Quái bị giết | LOOT_DROP | Heap | RTT avg / p95 / max |
|------|-----------|-------|-------------|-----|--------------|-----------|------|---------------------|
| 10 | 60 s | 3 | 0 | 0 | 90 | 64 | ~76 MB | — (đo sai, xem ghi chú) |
| 10 | 30 s | 3 | 0 | 0 | 60 | 41 | ~74 MB | 0,8 / 1 / 1 ms |
| 40 | 40 s | 10 | 0 | 0 | 240 | 167 | ~90 MB | 0,9 / 1 / 2 ms |

- DoD "Server 10 concurrent WS không crash": **đạt** (thêm 40 CCU cũng ổn, log server 0 ERROR).
- RTT trên là localhost = thời gian xử lý của server; latency thật qua Wi-Fi / Internet cần đo
  bằng ô debug `RTT` trên điện thoại (điền vào bảng dưới).
- Lần chạy đầu đo RTT sai (~64 ms) vì bot chỉ đọc inbox mỗi 55 ms; đã sửa để ghi thời điểm khi nhận gói.

| Môi trường | RTT đo trên client | Ghi chú |
|------------|--------------------|---------|
| Hub | N/A | Phase 0 không có hub |
| Instance — Editor, localhost | | |
| Instance — điện thoại, Wi-Fi LAN | | |
| Instance — cloud dev server | | |

---

## 4. Bug list

| ID | Mức | Mô tả | Trạng thái |
|----|-----|-------|------------|
| B1 | P0 | `RateLimiter` tràn số (`now - Long.MIN_VALUE`) → mọi MOVE_INTENT bị chặn sau 1 s | Đã sửa (tuần 4) |
| B2 | P1 | Cho phép sai số 0,1 m mỗi packet → speed hack +40% | Đã sửa (tuần 3) |
| B3 | P1 | Packet dồn do jitter mạng bị coi là đi quá nhanh → giật lùi | Đã sửa (tuần 3) |
| B4 | P1 | `application-local-native.yml` (mật khẩu DB) bị commit do sai đường dẫn `.gitignore` | `.gitignore` đã sửa; cần `git rm --cached` |
| B5 | P2 | Server chưa gửi lại LOOT_DROP khi client không ACK (đồ vẫn đã lưu DB) | Mở — Phase 1 |
| B6 | P2 | Charge chưa có dash/knockback (chỉ damage) | Mở — cần server-driven movement |
| | | *(thêm bug tìm được khi test 2 thiết bị)* | |
