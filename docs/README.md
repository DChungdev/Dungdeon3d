# Documentation Index — Dungeon Delvers Online

Tài liệu được sắp xếp theo thứ tự đọc khuyến nghị khi bắt đầu triển khai.

## 1. Vision & Design (đọc trước)

| # | Tài liệu | Ai cần đọc | Mục đích |
|---|----------|------------|----------|
| 1 | [GDD-v1.md](GDD-v1.md) | Everyone | Gameplay, progression, economy, monetization |
| 2 | [TDD-v1.md](TDD-v1.md) | Backend, Client | Kiến trúc kỹ thuật, DB, networking |
| 3 | [roadmap.md](roadmap.md) | PM, Dev | Phase 0–4, backlog, timeline |
| 4 | [economy-design.md](economy-design.md) | Design, Backend | Gold flow, drop rate, shop |

## 2. Implementation (đọc khi code)

| # | Tài liệu | Ai cần đọc | Mục đích |
|---|----------|------------|----------|
| 5 | [phase-0-checklist.md](phase-0-checklist.md) | Dev | Task cụ thể tuần 1–6 |
| 5b | [phase-0-test-report.md](phase-0-test-report.md) | Dev / QA | 20 case test 2 thiết bị, load test, latency, bug list |
| 5c | [phase-1-checklist.md](phase-1-checklist.md) | Dev | Task cụ thể tuần 7–16 (Vertical Slice) |
| 6 | [project-structure.md](project-structure.md) | Dev | Folder layout client + server |
| 7 | [setup-guide.md](setup-guide.md) | Dev | IDE, env vars |
| 7b | [setup-guide-windows-native.md](setup-guide-windows-native.md) | Dev | **Không Docker** — PostgreSQL native |
| 8 | [coding-conventions.md](coding-conventions.md) | Dev | Naming, Git, review rules |
| 9 | [unity-client-guide.md](unity-client-guide.md) | Client | Unity packages, scenes, workflow |

## 3. API & Protocol (reference khi implement)

| # | Tài liệu | Ai cần đọc | Mục đích |
|---|----------|------------|----------|
| 10 | [api/openapi.yaml](api/openapi.yaml) | Client, Backend | REST contract |
| 11 | [network-protocol.md](network-protocol.md) | Client, Backend | WebSocket messages, payloads |
| 12 | [game-config-spec.md](game-config-spec.md) | Design, Backend | Item, skill, mob, loot format |

## 4. Database

| # | Tài liệu | Path |
|---|----------|------|
| 13 | Schema migration | `server/src/main/resources/db/migration/V001__init_schema.sql` |
| 14 | Seed data MVP | `server/src/main/resources/db/migration/V002__seed_config.sql` |

## 5. Architecture Decisions

| # | Tài liệu | Quyết định |
|---|----------|------------|
| 15 | [decisions/ADR-001-modular-monolith.md](decisions/ADR-001-modular-monolith.md) | Modular monolith thay microservices |
| 16 | [decisions/ADR-002-websocket-json.md](decisions/ADR-002-websocket-json.md) | Raw WebSocket + JSON MVP |
| 17 | [decisions/ADR-003-personal-loot.md](decisions/ADR-003-personal-loot.md) | Personal loot, không trade MVP |

## 6. Shared config (dev)

JSON mẫu tại `shared/config/` — sync vào DB lúc seed hoặc load runtime.

```
shared/config/
├── items.json
├── skills.json
├── mobs.json
├── dungeons.json
├── loot_tables.json
└── quests.json
```

## Trạng thái tài liệu

| Tài liệu | Version | Status |
|----------|---------|--------|
| GDD | 1.0 | Ready |
| TDD | 1.0 | Ready |
| Roadmap | 1.0 | Ready |
| OpenAPI | 1.0 | Ready — Phase 0 subset marked |
| Network Protocol | 1.0 | Ready |
| DB Migration | V001–V002 | Ready |

## Cập nhật tài liệu

- Breaking API change → bump OpenAPI version + update network-protocol.md
- Schema change → new Flyway migration (không sửa V001)
- Gameplay change → update GDD + economy-design.md
