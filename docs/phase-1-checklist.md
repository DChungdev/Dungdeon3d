# Phase 1 — Vertical Slice Checklist

**Thời gian:** 10 tuần (tuần 7–16 tích lũy)  
**Mục tiêu:** Hub → party 2 người → Goblin Cave hoàn chỉnh → Goblin King → loot theo rarity → về Hub.

---

## Definition of Done (Phase 1)

- [ ] Flow đầy đủ: Login → Tạo/Chọn nhân vật → Hub → Party → Goblin Cave → Result → Hub
- [ ] 2 class chơi được: Warrior + Mage, mỗi class 4 skill (GDD §3.2)
- [ ] Party 2 người clear Goblin Cave trong **10–15 phút**
- [ ] Goblin King có phase, boss drop guaranteed Rare+ (15% Epic)
- [ ] Equip đồ → stat thay đổi, server tính damage theo gear
- [ ] FPS ≥ 30 trên Android tầm trung (Hub và trận boss)
- [ ] Playtest 5–10 người, điểm "fun" trung bình ≥ 3.5/5
- [ ] Trailer 60 s

---

## Hiện trạng đầu Phase 1 (từ Phase 0)

| Phần | Trạng thái |
|------|------------|
| Server | Auth, WS gateway, session/reconnect, 1 room cố định (`JOIN_INSTANCE`), auto-attack + Charge, 3 mob, loot cá nhân, túi 40 ô, lưu HP/MP — 82 test |
| Config | `CombatCatalog` / `DungeonCatalog` **hardcode Java**; `shared/config/*.json` đã có đủ 8 skill, mob Goblin, loot table nhưng server chưa đọc |
| Client | 1 scene `Boot`, UI bằng IMGUI (`OnGUI`), greybox capsule |
| DB | Đã có bảng `equipment_slots`, `dungeon_runs`, `dungeon_run_participants` (V001) — chưa dùng |

### Việc còn treo từ Phase 0 (làm trong tuần 7)

- [ ] B5 — gửi lại `LOOT_DROP` khi client không ACK
- [ ] B6 — Charge dash/knockback → cần server-driven movement (làm cùng tuần 12)
- [ ] Android debug APK + chạy 20 case trong [phase-0-test-report.md](phase-0-test-report.md)
- [ ] Deploy cloud dev 1 instance (Dockerfile đã có — chốt nền tảng)
- [ ] Ghi latency Wi-Fi / cloud

---

## Tuần 7 — Nền tảng: data-driven config & client scene flow

### Server

- [ ] `GameConfigLoader`: đọc `shared/config/*.json` lúc startup → `SkillCatalog`, `MobCatalog`, `DungeonCatalog`, `ItemCatalog` (thay hardcode)
- [ ] Validate config khi load (id trùng, tham chiếu loot table/mob không tồn tại → fail startup)
- [ ] Test: load toàn bộ `shared/config` không lỗi
- [ ] Fix B5 (resend `LOOT_DROP` theo ACK timeout)

### Client

- [ ] Tách scene: `Boot` → `Login` → `CharacterSelect` → `Hub` → `Dungeon_GoblinCave`
- [ ] `SceneFlow` service + loading screen
- [ ] Chuyển UI từ IMGUI sang **uGUI** (Canvas + TextMeshPro), giữ debug overlay IMGUI
- [ ] Chọn asset pack low-poly (nhân vật, goblin, hang động, UI) — ghi license

**Deliverable tuần 7:** Server chạy hoàn toàn từ config JSON; client chuyển scene được.

---

## Tuần 8 — Character create / select (2 class)

### Server

- [ ] Base stats Mage trong config (HP thấp, MP cao, ATK phép)
- [ ] `GET /api/v1/characters` trả class, level, gear summary
- [ ] `DELETE /api/v1/characters/{id}` *(tùy chọn — MVP 1 nhân vật/tài khoản)*

### Client

- [ ] Màn tạo nhân vật: chọn class (preview model + mô tả 4 skill), nhập tên, validate 3–16 ký tự
- [ ] Màn chọn nhân vật → Enter World
- [ ] Prefab Warrior + Mage (model, animator: idle/run/attack/cast/hit/downed)

**Deliverable tuần 8:** Tạo Mage hoặc Warrior, vào game đúng class.

---

## Tuần 9 — Hub Skyhaven

### Server

- [ ] `HubManager` + `HubShard` (max 30 người, không combat, broadcast 10 Hz)
- [ ] `JOIN_HUB` → `HUB_STATE`; `MOVE_INTENT` trong hub (tolerance rộng hơn instance)
- [ ] `ENTITY_SPAWN` / `ENTITY_DESPAWN` khi vào/rời hub
- [ ] Sau login mặc định vào Hub; reconnect về đúng zone (`zoneType` HUB/INSTANCE)
- [ ] MVP 1 shard (thiết kế sẵn cho 2 shard — TDD §10.1)

### Client

- [ ] Scene Hub greybox → art pass: quảng trường, NPC portal dungeon
- [ ] Thấy người chơi khác di chuyển trong Hub
- [ ] Tương tác NPC portal → mở panel dungeon

**Deliverable tuần 9:** 2 client gặp nhau trong Hub.

---

## Tuần 10 — Party

### Server

- [ ] `PartyManager` in-memory (max 4, leader)
- [ ] `PARTY_CREATE`, `PARTY_INVITE` (theo tên), `PARTY_INVITE_NOTIFY`, `PARTY_ACCEPT` / `PARTY_DECLINE`
- [ ] `PARTY_UPDATE` (danh sách thành viên, HP, online)
- [ ] `PARTY_LEAVE` / `PARTY_KICK`; leader rời → chuyển leader
- [ ] Thành viên disconnect: giữ chỗ trong grace 60 s
- [ ] Invite hết hạn 30 s; chặn invite người đã có party / offline
- [ ] Test: invite / accept / leave / kick / leader rời / disconnect

### Client

- [ ] Panel party (avatar, tên, HP bar), popup lời mời
- [ ] Mời bằng tên hoặc chạm vào người chơi trong Hub

**Deliverable tuần 10:** A mời B, B chấp nhận, cả hai thấy party.

---

## Tuần 11 — Vào dungeon & vòng đời instance

### Server

- [ ] `DUNGEON_QUEUE` (leader only) → tạo `InstanceRoom` riêng cho party (chưa có matchmaking ngẫu nhiên)
- [ ] Kiểm tra: tất cả thành viên online, ở Hub, đủ level
- [ ] `QUEUE_MATCHED` → `INSTANCE_ENTER` → `INSTANCE_START` (đếm ngược 3 s)
- [ ] Room state: `CREATING → ACTIVE → COMPLETED | FAILED → DESTROYED`
- [ ] Idle timeout 5 phút; destroy 60 s sau complete
- [ ] Rời instance → về Hub (đúng vị trí portal)
- [ ] `JOIN_INSTANCE` trực tiếp chỉ còn ở profile dev
- [ ] Ghi `dungeon_runs` + `dungeon_run_participants` lúc bắt đầu / kết thúc

### Client

- [ ] Panel chọn dungeon (Goblin Cave, Normal) → nút Start (chỉ leader)
- [ ] Loading → scene dungeon → đếm ngược

**Deliverable tuần 11:** Party 2 người từ Hub vào cùng một room riêng, rời về Hub.

---

## Tuần 12 — Combat mở rộng: 8 skill, buff, downed/revive

### Server

- [ ] Skill pipeline data-driven (TDD §8.4): loại target / self / AOE circle / cone
- [ ] Buff system (TDD §8.6): stat modifier, stun, slow, shield, hết hạn theo tick → `BUFF_APPLY` / `BUFF_REMOVE`
- [ ] Server-driven movement: dash, knockback (đóng B6)
- [ ] Warrior: Charge (dash + knockback), Shield Bash (stun 1.5 s), Whirlwind (AOE), Battle Cry (+20% ATK party 8 s)
- [ ] Mage: Firebolt, Frost Nova (slow 40% 3 s), Arcane Missile (channel 3 hit), Mana Shield (absorb 5 s)
- [ ] Mage auto-attack tầm xa
- [ ] Downed: HP = 0 → `PLAYER_DOWNED`, bleedout 30 s; revive cast 3 s → 30% HP (`REVIVE_START` / `REVIVE_CANCEL` / `REVIVE_COMPLETE`)
- [ ] Cả party downed → `INSTANCE_FAILED` sau 5 s
- [ ] Test cho từng skill + buff + revive

### Client

- [ ] 4 nút skill theo class (icon, cooldown, MP), auto-aim mục tiêu gần nhất
- [ ] VFX cơ bản mỗi skill, icon buff trên HP bar
- [ ] UI downed + nút revive đồng đội

**Deliverable tuần 12:** Warrior + Mage dùng đủ 4 skill, cứu được đồng đội.

---

## Tuần 13 — Goblin Cave & Goblin King

### Server

- [ ] 3 room theo `dungeons.json`; cửa giữa các room mở khi clear (objective)
- [ ] `OBJECTIVE_UPDATE` (số quái còn lại, phase boss)
- [ ] Goblin Archer (tầm xa), Goblin Shaman (hồi máu đồng minh)
- [ ] Goblin King: 2–3 phase theo ngưỡng HP (vd: 70% gọi đệ, 35% enrage/AOE dậm đất có telegraph)
- [ ] Scale HP/ATK mob theo số người trong party (chốt hệ số khi cân bằng)
- [ ] `INSTANCE_COMPLETE`: thời gian, rank, tóm tắt loot; kiểm tra clear time ≥ tối thiểu
- [ ] Gold + (XP ghi nhận, chưa lên level — Phase 2)

### Client

- [ ] Scene Goblin Cave 3 phòng (art pass), cửa mở/đóng
- [ ] Thanh HP boss, cảnh báo telegraph vùng đỏ
- [ ] Màn Result (thời gian, rank, loot) → nút về Hub

**Deliverable tuần 13:** 2 người clear trọn Goblin Cave.

---

## Tuần 14 — Rarity loot & trang bị

### Server

- [ ] Loot rarity Common 70% / Rare 25% / Epic 5%; boss guaranteed Rare+, 15% Epic (GDD §6.1)
- [ ] Equip / unequip (`equipment_slots`): weapon, helm, chest — đúng class
- [ ] Stat nhân vật = base + gear; áp vào `DamageCalculator`
- [ ] Bind on equip; Epic ghi `item_audit_log`
- [ ] REST hoặc WS: `POST /api/v1/characters/{id}/equip`
- [ ] Test: equip sai class / sai slot bị từ chối, stat thay đổi đúng

### Client

- [ ] Inventory uGUI: lưới 40 ô, màu theo rarity, tooltip so sánh stat
- [ ] Tab trang bị, model đổi vũ khí khi equip
- [ ] Loot popup hiển thị rarity (hiệu ứng riêng cho Epic)

**Deliverable tuần 14:** Nhặt đồ Rare/Epic → equip → đánh mạnh hơn.

---

## Tuần 15 — Polish: UI flow, SFX, hiệu năng

- [ ] SFX: đánh thường, 8 skill, quái bị đánh/chết, boss, UI click, loot Epic
- [ ] Nhạc nền Hub + dungeon + boss
- [ ] Camera shake, hit-stop nhẹ, damage number theo crit
- [ ] Tutorial ngắn lần đầu (joystick, skill, revive)
- [ ] Cân bằng: thời gian clear 2p 10–15 phút, TTK mob thường 3–8 s
- [ ] Profiling Android: FPS ≥ 30, draw call, GC alloc; LOD / texture compression
- [ ] Load test lại: 10 party × 2 bot qua Hub → dungeon

**Deliverable tuần 15:** Build release-candidate nội bộ.

---

## Tuần 16 — Playtest & trailer

- [ ] Internal test build (APK) + server cloud dev
- [ ] Analytics tối thiểu: thời gian clear, số lần downed, skill dùng nhiều nhất, crash
- [ ] Playtest 5–10 người + form khảo sát (fun, độ khó, độ rõ UI, lỗi)
- [ ] Fix bug P0/P1 từ playtest
- [ ] Trailer 60 s
- [ ] Phase 1 retrospective + cập nhật roadmap Phase 2

**Deliverable tuần 16:** Phase 1 sign-off → bắt đầu Phase 2.

---

## Phase 1 — KHÔNG làm

- Chat, emote, friends, guild
- Matchmaking ngẫu nhiên (chỉ party tự lập)
- Leveling 1–25, XP curve
- Undead Crypt, Dragon Nest, procedural dungeon
- Hard mode, leaderboard
- Crafting, enhancement, NPC shop
- Monetization, Battle Pass
- Redis / nhiều node (in-memory 1 node như Phase 0)

---

## Test Cases (smoke)

| # | Test | Expected |
|---|------|----------|
| 1 | Login lần đầu | Vào màn tạo nhân vật |
| 2 | Tạo Mage | Vào Hub với 4 skill Mage |
| 3 | 2 người trong Hub | Thấy nhau di chuyển |
| 4 | A mời B, B chấp nhận | Cả hai thấy party 2 người |
| 5 | B (không phải leader) bấm Start | Server từ chối |
| 6 | Leader Start | Cả party vào cùng room riêng, đếm ngược 3 s |
| 7 | Shield Bash lên Grunt | Grunt bị stun 1.5 s |
| 8 | A bị downed, B revive 3 s | A dậy với 30% HP |
| 9 | Cả party downed | `INSTANCE_FAILED`, về Hub |
| 10 | Giết Goblin King | Mỗi người ≥ 1 đồ Rare+, màn Result |
| 11 | Equip vũ khí Rare | ATK tăng, damage tăng |
| 12 | Equip Staff cho Warrior | Server từ chối |
| 13 | Disconnect trong dungeon 20 s | Reconnect về đúng room, party còn nguyên |
| 14 | Leader rời party trong Hub | Thành viên còn lại thành leader |

---

## Rủi ro Phase 1

| Rủi ro | Mitigation |
|--------|------------|
| Art/animation tốn thời gian (solo dev) | Dùng asset pack low-poly; chốt pack ở tuần 7 |
| Combat 8 skill + buff phức tạp | Skill pipeline data-driven trước, thêm skill sau |
| Boss 2 người quá dễ/khó | Scale theo party size, tune bằng config JSON không cần build lại |
| Chuyển IMGUI → uGUI tốn công | Làm dần theo từng màn, giữ debug overlay IMGUI |
| Scope creep | Danh sách "KHÔNG làm" |
| FPS Android thấp | Profiling sớm từ tuần 9 (Hub), không để tới tuần 15 |
