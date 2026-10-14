# Product Roadmap — Dungeon Delvers Online

| Version | 1.0 |
|---------|-----|
| Timeline | 14–16 tháng (buffer 18) |
| Team | 1 solo dev + AI |

Xem backlog đầy đủ trong conversation / issue tracker. Tài liệu này tóm tắt phase.

---

## Timeline

| Phase | Tuần | Tích lũy | Mục tiêu |
|-------|------|----------|----------|
| **Phase 0** Prototype | 6 | 6 | Core loop kỹ thuật |
| **Phase 1** Vertical Slice | 10 | 16 | 1 dungeon polish end-to-end |
| **Phase 2** Alpha | 20 | 36 | Feature-complete MVP 100 CCU |
| **Phase 3** Beta | 10 | 46 | Soft launch + monetization |
| **Phase 4** Launch | 4 | 50 | Global release |

---

## Phase 0 — Prototype (6 tuần)

**Mục tiêu:** Login → instance → combat → loot → persist.

**Tính năng:** Auth, 1 class, WS sync, 3 mob, 1 skill, inventory DB.

**Deliverable:** APK debug 2-device demo, architecture doc.

**Done khi:** 2 client sync, server-authoritative damage, loot persist, 10 CCU stable.

→ Chi tiết: [phase-0-checklist.md](phase-0-checklist.md)

---

## Phase 1 — Vertical Slice (10 tuần)

**Mục tiêu:** Hub → party 2p → 1 dungeon polish → boss → rarity loot.

**Tính năng:** Hub, char create, 2 class, party invite, Goblin Cave, UI flow, SFX.

**Deliverable:** Internal test build, playtest 5–10 người, trailer 60s.

**Done khi:** 2p clear 10–15 phút, FPS ≥30, playtest ≥3.5/5 fun.

---

## Phase 2 — Alpha (20 tuần)

**Mục tiêu:** MVP 100 CCU, retention test.

**Tính năng:** Level 1–25, 3 dungeon + procedural, crafting, guild, chat, daily quest, leaderboard, admin panel, load test.

**Deliverable:** Closed alpha 50–100 testers, economy spreadsheet, load test report.

**Done khi:** 100 CCU load test, D1≥35%, D7≥15%, no economy exploit P0.

---

## Phase 3 — Beta (10 tuần)

**Mục tiêu:** Soft launch, monetization live.

**Tính năng:** UI polish, Battle Pass S1, IAP, affix, localization EN/VI, push notification.

**Deliverable:** Play Store soft launch, KPI dashboard.

**Done khi:** D7≥18%, IAP 98% success, 100 CCU prod 72h stable.

---

## Phase 4 — Launch (4 tuần)

**Mục tiêu:** Global release + Season 1.

**Tính năng:** Launch event, referral, auto-scale, patch 1.0.x, Discord community.

**Done khi:** 100+ CCU launch day, uptime 99.5%, crash <1%.

---

## Epic Index (23 epics)

| # | Epic | Phase chính |
|---|------|-------------|
| 1 | Project Foundation & DevOps | P0–P2 |
| 2 | Authentication & Account | P0–P2 |
| 3 | Character System | P0–P2 |
| 4 | Client Core (Unity) | P0–P3 |
| 5 | Networking & Realtime | P0–P3 |
| 6 | Hub World | P1–P2 |
| 7 | Combat System | P0–P2 |
| 8 | Dungeon & Instance | P1–P2 |
| 9 | Party & Matchmaking | P1–P2 |
| 10 | Progression & Leveling | P2 |
| 11 | Inventory & Equipment | P0–P2 |
| 12 | Loot & Drop System | P0–P2 |
| 13 | Crafting | P2 |
| 14 | Economy & NPC Shop | P2–P3 |
| 15 | Social Systems | P2–P3 |
| 16 | Quest System | P2 |
| 17 | UI/UX | P1–P3 |
| 18 | Leaderboard & Seasons | P2–P4 |
| 19 | Monetization | P2–P4 |
| 20 | Analytics & Admin | P2–P4 |
| 21 | Security & Anti-Cheat | P0–P3 |
| 22 | QA, Performance & Polish | P2–P3 |
| 23 | Launch & Live Ops | P3–P4 |

Backlog chi tiết Epic → Feature → Task: xem session planning hoặc import vào issue tracker.

---

## MVP Scope Lock

**Có:** Hub, party 4, 3 fixed + 1 procedural dungeon, 2 class, level 25, guild, chat, daily, leaderboard, Battle Pass.

**Không (defer):** iOS, PvP, trade, housing, bot fill, 3rd class, voice chat, stamina.

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial roadmap |
