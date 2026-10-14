# Setup Guide — Development Environment

## Dungeon Delvers Online

---

## 1. Yêu cầu hệ thống

| Tool | Version | Ghi chú |
|------|---------|---------|
| OS | Windows 10/11, macOS, Linux | |
| Java JDK | 21 LTS | `java -version` |
| Maven | 3.9+ | Hoặc dùng wrapper trong `server/` |
| Docker Desktop | Latest | **Tùy chọn** — PostgreSQL + Redis |
| PostgreSQL | 16 | **Bắt buộc** — native hoặc Docker |
| Git | 2.40+ | |
| Unity Hub | Latest | Khi làm client |
| Unity Editor | 2022.3 LTS | Module Android Build Support |
| Android Studio | Latest | SDK API 33+ |
| IDE | IntelliJ IDEA / VS Code | Backend |
| Postman hoặc Bruno | — | Test API |

---

## 2. Clone & cấu trúc

```bash
git clone <repo-url> ProjectGame
cd ProjectGame
```

```
ProjectGame/
├── client/          # Unity (tạo Phase 0 tuần 2)
├── server/          # Spring Boot
├── shared/config/   # JSON config dev
├── docs/
└── docker-compose.yml
```

---

## 3. Infrastructure local

> **Không có Docker?** Dùng hướng dẫn chi tiết: **[setup-guide-windows-native.md](setup-guide-windows-native.md)**  
> Phase 0 chỉ cần PostgreSQL + profile `local-native` (không Redis).

### 3.1a Windows native (không Docker) — khuyến nghị nếu không cài Docker

1. Cài PostgreSQL 16 từ [postgresql.org](https://www.postgresql.org/download/windows/)
2. Chạy `scripts/windows/init-postgres.sql`
3. Copy `application-local-native.yml.example` → `application-local-native.yml`
4. Verify: `.\scripts\windows\verify-setup.ps1`

### 3.1b Docker (tùy chọn)

```bash
docker compose up -d
```

Verify:

```bash
docker compose ps
docker exec -it ddo-postgres psql -U ddo -d dungeon_delvers -c "\dt"
docker exec -it ddo-redis redis-cli ping
```

### 3.2 Connection strings

| Service | URL |
|---------|-----|
| PostgreSQL | `jdbc:postgresql://localhost:5432/dungeon_delvers` |
| Redis | `redis://localhost:6379` |

Credentials: user `ddo`, password `ddo`

---

## 4. Server setup

### 4.1 Config local

**Không Redis (Phase 0):**

```powershell
copy server\src\main\resources\application-local-native.yml.example server\src\main\resources\application-local-native.yml
# Profile: local-native
```

**Có Redis (Memurai / Docker):**

```powershell
copy server\src\main\resources\application-local.yml.example server\src\main\resources\application-local.yml
# Profile: local
```

Environment variables (PowerShell):

```powershell
$env:SPRING_PROFILES_ACTIVE = "local-native"
$env:DDO_DB_URL = "jdbc:postgresql://localhost:5432/dungeon_delvers"
$env:DDO_DB_USER = "ddo"
$env:DDO_DB_PASSWORD = "ddo"
$env:DDO_JWT_SECRET = "dev-secret-change-in-production-min-32-chars"
```

### 4.2 Scaffold server (tuần 1)

Khi tạo project Spring Boot:

```bash
cd server
# Sau khi có pom.xml:
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway tự chạy `V001` + `V002` on startup.

### 4.3 Verify server

```bash
curl http://localhost:8080/health

curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### 4.4 WebSocket test

Dùng `wscat` hoặc Postman WS:

```
ws://localhost:8080/ws/game?token=<access_token>
```

Gửi:

```json
{"type":"PING","seq":1,"payload":{}}
```

---

## 5. Unity client setup

Xem chi tiết: [unity-client-guide.md](unity-client-guide.md)

Tóm tắt:

1. Unity Hub → Install **2022.3 LTS** + Android Build Support
2. New project URP template → save vào `client/`
3. Player Settings: Company name, package `com.yourstudio.dungeondelvers`
4. Minimum API Level: 24 (Android 7.0)
5. Scripting Backend: IL2CPP (release), Mono (dev faster)

---

## 6. IDE configuration

### IntelliJ IDEA

- Open `server/` as Maven project
- SDK: Java 21
- Enable annotation processing (Lombok nếu dùng)
- Run config: Spring Boot, profile `local`

### VS Code

Extensions: Extension Pack for Java, Spring Boot Extension Pack

---

## 7. Environment matrix

| Variable | Dev | Staging | Prod |
|----------|-----|---------|------|
| `SPRING_PROFILES_ACTIVE` | local | staging | prod |
| `DDO_JWT_SECRET` | dev secret | secrets manager | secrets manager |
| `DDO_DB_URL` | localhost | RDS endpoint | RDS endpoint |
| `DDO_REDIS_HOST` | localhost | ElastiCache | ElastiCache |
| `DDO_CORS_ORIGINS` | `*` | staging domain | prod domain |

**Không commit** `application-local.yml` với secret thật.

---

## 8. CI/CD (Phase 1+)

GitHub Actions workflow (tạo `.github/workflows/server-ci.yml`):

1. Checkout
2. Setup Java 21
3. `mvn test`
4. Build Docker image (staging+)

---

## 9. Troubleshooting

| Vấn đề | Giải pháp |
|--------|-----------|
| Flyway migration fail | `docker compose down -v` reset DB (dev only) |
| Port 5432 in use | Đổi port trong docker-compose.yml |
| Redis connection refused | `docker compose up -d redis` |
| Unity không connect WS | Check cleartext permission Android (dev HTTP) |
| JWT expired | Refresh token hoặc re-login |

### Reset database (dev only)

```bash
docker compose down -v
docker compose up -d
# Restart server — Flyway re-run migrations
```

---

## 10. Checklist first day

- [ ] PostgreSQL running (native hoặc Docker)
- [ ] Redis optional — `local-native` profile nếu không có
- [ ] Java 21 installed
- [ ] Server health 200
- [ ] Register + login works
- [ ] Flyway tables created (30+ tables)
- [ ] Seed data: `SELECT count(*) FROM skill_definitions` → 8
- [ ] Docs read: phase-0-checklist.md

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial setup guide |
