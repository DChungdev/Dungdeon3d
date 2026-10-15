# Dungeon Delvers — Unity Client

## Status

Unity **2022.3.46f1**, template 3D (URP). Packages: Input System 1.7.0, Newtonsoft Json 3.2.2, TextMeshPro 3.0.6.

## Mở project

Unity Hub → **Add** → chọn thư mục `ProjectGame/client/`. Xem [unity-client-guide.md](../docs/unity-client-guide.md).

## Chạy thử (Phase 0)

1. Chạy server: `cd server` → `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-native"`
2. Unity menu **Dungeon Delvers → Create Boot Scene** (tạo `Assets/_Project/Scenes/Boot.unity`).
3. Bấm **Play** → Đăng ký / Đăng nhập → (tạo Warrior nếu chưa có) → tự vào phòng Goblin Cave.
4. Di chuyển: **WASD / phím mũi tên**, hoặc kéo chuột / chạm ở nửa trái màn hình (joystick).
5. Combat: **Space** / nút **Đánh** = khóa con quái gần nhất và đánh thường (tầm 2,5 m);
   **Q** / nút **Charge** = skill Charge (tầm 8 m, hồi 8 s, 15 MP). Quái chết hồi sinh sau 20 s.
6. Loot: giết quái → thông báo góc phải (+đồ, +10 gold). Nút **Túi đồ** xem inventory (đọc từ server);
   **Rời phòng** tự mở túi đồ đã tải lại.

Hai người chơi: Editor + điện thoại (APK) cùng mạng Wi-Fi. Trên điện thoại nhập server
`http://<IP LAN của PC>:8080` (xem bằng `ipconfig`), và mở port 8080 trong Windows Firewall.

### Cấu trúc script (`Assets/_Project/Scripts`)

| Thư mục | Nội dung |
|---------|----------|
| `Core/` | `GameBootstrap` — entry point, luồng login → WS → vào phòng, heartbeat, reconnect |
| `Network/` | `RestApiService`, `WebSocketService` (nhận ở background → queue → main thread), DTO |
| `Gameplay/` | `InstanceController`, `LocalPlayerController` (prediction + reconciliation), `RemoteEntityView` (interpolation 200 ms), `EntityView` (HP, chết), `CombatController` (khóa mục tiêu, skill), HP bar, damage popup, greybox room, nameplate, camera |
| `UI/` | Login / debug overlay / combat HUD (IMGUI, Phase 0), `VirtualJoystick` |
| `Editor/` | Menu tạo Boot scene |

## Docs

- [Unity Client Guide](../docs/unity-client-guide.md)
- [Network Protocol](../docs/network-protocol.md)
- [Game Config Spec](../docs/game-config-spec.md)
- [Phase 0 Checklist](../docs/phase-0-checklist.md)
