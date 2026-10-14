# Dungeon Delvers — Server

Spring Boot 3.2+ · Java 21 · PostgreSQL · Redis · WebSocket

## Status

**Scaffolded** — Spring Boot 4.0 · Java 21 · Phase 0 Tuần 1 (auth + characters + health).

## Quick start (sau scaffold)

**Không Docker (Windows):**

```powershell
# 1. PostgreSQL — xem docs/setup-guide-windows-native.md
psql -U postgres -f ..\scripts\windows\init-postgres.sql

# 2. Config (không cần Redis Phase 0)
copy src\main\resources\application-local-native.yml.example src\main\resources\application-local-native.yml

# 3. Run
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-native"
```

**Có Docker:**

```bash
docker compose up -d
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Migrations

Flyway migrations trong `src/main/resources/db/migration/`:

| Version | File | Description |
|---------|------|-------------|
| V001 | `V001__init_schema.sql` | Full schema |
| V002 | `V002__seed_config.sql` | MVP seed data |

## Scaffold command (tuần 1)

Tạo project với [Spring Initializr](https://start.spring.io/):

- Project: Maven
- Java 21
- Dependencies: Web, WebSocket, Security, JPA, PostgreSQL, Flyway, Redis, Validation, Actuator

Sau đó copy migrations và config từ repo này vào.

## Docs

- [TDD v1](../docs/TDD-v1.md)
- [OpenAPI](../docs/api/openapi.yaml)
- [Network Protocol](../docs/network-protocol.md)
- [Phase 0 Checklist](../docs/phase-0-checklist.md)
