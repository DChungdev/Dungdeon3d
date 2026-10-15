# Phase 0 — Prototype Checklist

**Thời gian:** 6 tuần  
**Mục tiêu:** Chứng minh core loop kỹ thuật end-to-end.

---

## Definition of Done (Phase 0)

- [ ] 2 Android client cùng instance, thấy nhau di chuyển
- [x] Damage server-authoritative — client hack HP không có hiệu lực
- [x] Kill mob → loot vào inventory → logout/login loot còn
- [ ] Disconnect 30s → reconnect không mất state
- [x] Server 10 concurrent WS không crash *(load test 10 và 40 bot, 0 lỗi)*
- [ ] Demo video 3–5 phút recorded

---

## Tuần 1 — Infrastructure & Auth

### Server

- [x] Scaffold Spring Boot 4.0 + Java 21 (`server/`)
- [ ] Module structure: `app`, `auth`, `game-core`, `persistence`
- [x] PostgreSQL chạy (native Windows **hoặc** Docker — xem [setup-guide-windows-native.md](setup-guide-windows-native.md))
- [x] Profile `local-native` nếu không có Redis
- [x] Flyway V001 migration chạy thành công
- [ ] `application-local.yml` config
- [x] `GET /health` → 200

### Auth API

- [x] `POST /api/v1/auth/register`
- [x] `POST /api/v1/auth/login` → JWT access + refresh
- [x] `POST /api/v1/auth/refresh`
- [x] Password bcrypt hash
- [x] JWT filter cho protected endpoints

### Character API (minimal)

- [x] `POST /api/v1/characters` — tạo Warrior
- [x] `GET /api/v1/characters` — list by account

### DevOps

- [ ] `.gitignore`, README, docker-compose verified
- [ ] Postman/Bruno collection hoặc curl scripts test auth

**Deliverable tuần 1:** Register → login → create character qua REST.

---

## Tuần 2 — WebSocket & Session

### WebSocket Gateway

- [x] `ws://localhost:8080/ws/game?token=...`
- [x] JWT validate on connect
- [x] Message envelope parse (type, seq, payload)
- [x] `CONNECTED` response với serverTime
- [x] `PING` / `PONG` heartbeat 5s
- [ ] Session lưu Redis `session:{id}` *(hiện in-memory, đủ cho 1 node — chuyển Redis khi scale)*

### Session Manager

- [x] Map connection ↔ account ↔ character
- [x] Disconnect cleanup (grace 60s)
- [x] `ERROR` message format

### Client (Unity) — skeleton

- [x] Unity 2022.3 URP project `client/`
- [x] Boot scene + HTTP login flow
- [x] WS connect sau login
- [x] Log messages to console

**Deliverable tuần 2:** Client login REST + WS connect + heartbeat.

---

## Tuần 3 — Movement & Instance

### Instance Manager

- [x] `JOIN_INSTANCE` (hardcoded room MVP)
- [x] Create room in-memory
- [x] Player spawn position
- [x] `INSTANCE_ENTER` snapshot

### Movement Sync

- [ ] Client gửi `MOVE_INTENT` 20Hz *(chờ test 2 client)*
- [x] Server validate speed cap
- [x] Server broadcast `ENTITY_UPDATE` 10Hz
- [ ] Client prediction + reconciliation (basic) *(chờ test 2 client)*

### Unity

- [x] Greybox scene 1 room
- [x] Virtual joystick
- [ ] Remote player spawn/despawn *(chờ test 2 client)*
- [x] Nameplate

**Deliverable tuần 3:** 2 client thấy nhau di chuyển trong 1 room.

---

## Tuần 4 — Combat v0

### Combat Server

- [x] `CombatEntity` model (HP, ATK, DEF)
- [x] Combat tick loop 10Hz per instance
- [x] Auto-attack target lock
- [x] Damage formula (TDD §8.2)
- [x] `SKILL_CAST` — 1 skill Warrior (Charge) *(damage; dash/knockback hoãn)*
- [x] `DAMAGE_EVENT`, `ENTITY_DEATH` broadcast
- [x] `SKILL_RESULT` success/fail

### Mob

- [x] 3 mob spawn cố định
- [x] FSM: IDLE → CHASE → ATTACK → RETURN
- [x] Mob death despawn

### Unity

- [ ] Attack button + 1 skill button *(đã viết script, chờ test trong Unity)*
- [ ] Target lock nearest *(đã viết script, chờ test trong Unity)*
- [ ] HP bar mob + player *(đã viết script, chờ test trong Unity)*
- [ ] Damage number popup (cosmetic) *(đã viết script, chờ test trong Unity)*

**Deliverable tuần 4:** Kill 3 mob bằng auto-attack + 1 skill.

---

## Tuần 5 — Loot & Persistence

### Inventory

- [x] `inventory_items` CRUD server-side *(tạo, gộp chồng, đọc; xóa/bán chưa có — chưa cần ở P0)*
- [x] Loot roll on mob death (1 item Common) *(1 lần roll theo `loot_table_entries`, có thể ra RARE)*
- [x] `GET /api/v1/characters/{id}/inventory`
- [x] Inventory full → reject với error

### Persistence

- [x] Save character HP/MP on instance exit
- [x] Currency audit log (gold +10 per mob MVP)
- [x] Idempotent loot (mob kill once)

### Unity

- [ ] Loot notification UI *(đã viết script, chờ test trong Unity)*
- [ ] Simple inventory list UI (debug) *(đã viết script, chờ test trong Unity)*
- [ ] Instance exit → reload inventory REST *(đã viết script, chờ test trong Unity)*

**Deliverable tuần 5:** Loot persist qua logout/login.

---

## Tuần 6 — Integration & Demo

### Hardening

- [ ] Reconnect flow (disconnect 10s → reconnect) *(server + client đã làm; chờ test case #19)*
- [x] Rate limit login 5/min
- [x] Input validation all WS messages *(+ giới hạn 50 msg/s/kết nối, flood → đóng)*
- [ ] Error handling client (toast) *(đã viết script, chờ test trong Unity)*
- [x] 2-device test checklist 20 cases *(viết xong: [phase-0-test-report.md](phase-0-test-report.md); chờ chạy tay)*

### Build

- [ ] Android debug APK
- [ ] Server deploy cloud dev (1 instance) *(Dockerfile + profile `dev` sẵn sàng; chưa chọn nền tảng)*
- [x] Client point to dev server config *(ô Server trên màn đăng nhập, nhớ trong PlayerPrefs)*

### Documentation

- [ ] Ghi latency đo được (hub N/A, instance) *(localhost đã đo; còn Wi-Fi / cloud)*
- [x] Bug list P0/P1
- [ ] Phase 0 retrospective doc
- [ ] Demo video 3–5 phút

**Deliverable tuần 6:** Phase 0 sign-off → bắt đầu Phase 1.

---

## Phase 0 — KHÔNG làm

- Hub world
- Party system
- Multiple dungeons
- Class Mage
- UI polish
- Monetization
- Guild / chat
- Procedural dungeon

---

## Test Cases (smoke)

| # | Test | Expected |
|---|------|----------|
| 1 | Register new account | 201, token returned |
| 2 | Login wrong password | 401 |
| 3 | WS without token | Connection rejected |
| 4 | 2 players same instance | See each other |
| 5 | Kill mob player A | Only A gets loot (personal) |
| 6 | Spam skill on cooldown | Server reject |
| 7 | Disconnect + reconnect 20s | Rejoin instance |
| 8 | Kill mob twice same corpse | No double loot |

---

## Rủi ro Phase 0

| Rủi ro | Mitigation |
|--------|------------|
| WS sync khó | Chỉ 1 room, 4 player max test |
| Unity learning curve | Greybox only, defer art |
| Spring WS config | Dùng proven template |
| Scope creep | Checklist "KHÔNG làm" |
