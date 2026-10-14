# Dungeon Delvers Online

MMORPG co-op session-based 3D mobile — Unity client + Spring Boot backend.

| Thuộc tính | Giá trị |
|------------|---------|
| Engine | Unity 2022.3 LTS (URP) |
| Backend | Java 21 · Spring Boot 3.2+ |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Realtime | WebSocket (WSS) |
| Target | ~100 CCU MVP · Android first |

## Cấu trúc repo

```
ProjectGame/
├── client/                 # Unity project (tạo khi Phase 0)
├── server/                 # Spring Boot modular monolith
├── shared/
│   └── config/             # Game config JSON (source of truth dev)
├── docs/                   # Tài liệu thiết kế
├── docker-compose.yml      # PostgreSQL + Redis local
└── README.md
```

## Bắt đầu nhanh

### Yêu cầu

- Java 21 JDK
- PostgreSQL 16 (cài native Windows — **không cần Docker**)
- Unity 2022.3 LTS (khi làm client)
- Git

Redis **không bắt buộc** Phase 0 — dùng profile `local-native` (session in-memory).

### 1. Infrastructure local

**Không có Docker?** → [docs/setup-guide-windows-native.md](docs/setup-guide-windows-native.md)

```powershell
# Cài PostgreSQL 16, rồi tạo DB:
psql -U postgres -f scripts\windows\init-postgres.sql

# Kiểm tra môi trường:
.\scripts\windows\verify-setup.ps1
```

PostgreSQL: `localhost:5432` · DB: `dungeon_delvers` · User/Pass: `ddo/ddo`

*(Tùy chọn)* Có Docker: `docker compose up -d` — xem [docker-compose.yml](docker-compose.yml)

### 2. Server (sau khi scaffold Phase 0)

```powershell
cd server
copy src\main\resources\application-local-native.yml.example src\main\resources\application-local-native.yml
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-native"
```

API: `http://localhost:8080`  
Health: `http://localhost:8080/health`  
WebSocket: `ws://localhost:8080/ws/game`

### 3. Unity client

Xem [docs/setup-guide.md](docs/setup-guide.md) và [docs/unity-client-guide.md](docs/unity-client-guide.md).

## Tài liệu

| Tài liệu | Mô tả |
|----------|--------|
| [docs/README.md](docs/README.md) | **Index tài liệu** — đọc từ đây |
| [docs/GDD-v1.md](docs/GDD-v1.md) | Game Design Document |
| [docs/TDD-v1.md](docs/TDD-v1.md) | Technical Design Document |
| [docs/roadmap.md](docs/roadmap.md) | Product Roadmap (Phase 0–4) |
| [docs/phase-0-checklist.md](docs/phase-0-checklist.md) | Checklist triển khai Phase 0 |
| [docs/api/openapi.yaml](docs/api/openapi.yaml) | REST API specification |
| [docs/network-protocol.md](docs/network-protocol.md) | WebSocket protocol |
| [docs/game-config-spec.md](docs/game-config-spec.md) | Format config game data |
| [docs/setup-guide.md](docs/setup-guide.md) | Môi trường dev |
| [docs/coding-conventions.md](docs/coding-conventions.md) | Quy ước code |
| [docs/project-structure.md](docs/project-structure.md) | Cấu trúc chi tiết |
| [docs/economy-design.md](docs/economy-design.md) | Economy & monetization |

## Phase hiện tại

**Phase 0 — Prototype** (6 tuần)

Mục tiêu: Login → instance → combat → loot → persist DB.

Xem checklist: [docs/phase-0-checklist.md](docs/phase-0-checklist.md)

## License

Proprietary — All rights reserved.
