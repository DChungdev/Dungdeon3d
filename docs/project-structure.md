# Project Structure

## Dungeon Delvers Online — Monorepo Layout

---

## Root

```
ProjectGame/
├── README.md
├── docker-compose.yml
├── .gitignore
├── .editorconfig
├── client/                         # Unity 2022.3 LTS
├── server/                         # Spring Boot 3.2+
├── shared/
│   └── config/                     # Dev JSON configs (mirror DB seed)
├── docs/
│   ├── README.md                   # Doc index
│   ├── GDD-v1.md
│   ├── TDD-v1.md
│   ├── roadmap.md
│   ├── economy-design.md
│   ├── phase-0-checklist.md
│   ├── setup-guide.md
│   ├── coding-conventions.md
│   ├── network-protocol.md
│   ├── game-config-spec.md
│   ├── unity-client-guide.md
│   ├── api/openapi.yaml
│   └── decisions/                  # ADRs
└── .github/
    └── workflows/                  # CI (Phase 1+)
```

---

## Server (`server/`)

```
server/
├── pom.xml                         # Parent POM (modular monolith)
├── mvnw
├── mvnw.cmd
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/com/dungeondelvers/
│   │   │   ├── DungeonDelversApplication.java
│   │   │   ├── app/
│   │   │   │   ├── config/         # Security, Redis, WS, Jackson
│   │   │   │   └── properties/     # @ConfigurationProperties
│   │   │   ├── auth/
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   └── jwt/
│   │   │   ├── api/
│   │   │   │   ├── rest/           # REST controllers
│   │   │   │   └── ws/             # WebSocket handler, dispatcher
│   │   │   ├── game/
│   │   │   │   ├── hub/
│   │   │   │   ├── instance/
│   │   │   │   ├── combat/
│   │   │   │   ├── matchmaking/
│   │   │   │   └── session/
│   │   │   ├── economy/
│   │   │   │   ├── loot/
│   │   │   │   ├── inventory/
│   │   │   │   ├── craft/
│   │   │   │   └── shop/
│   │   │   ├── social/
│   │   │   │   ├── party/
│   │   │   │   ├── friend/
│   │   │   │   ├── guild/
│   │   │   │   └── chat/
│   │   │   ├── quest/
│   │   │   ├── config/             # GameConfigLoader
│   │   │   ├── admin/
│   │   │   └── common/
│   │   │       ├── dto/
│   │   │       ├── exception/
│   │   │       └── util/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml.example
│   │       └── db/migration/
│   │           ├── V001__init_schema.sql
│   │           └── V002__seed_config.sql
│   └── test/
│       └── java/com/dungeondelvers/
│           ├── auth/
│           ├── combat/
│           └── integration/
```

### Module responsibility

| Package | Responsibility |
|---------|----------------|
| `auth` | Register, login, JWT, guest |
| `api.rest` | HTTP endpoints per OpenAPI |
| `api.ws` | WS connect, route, serialize |
| `game.session` | SessionManager, Redis presence |
| `game.hub` | Hub shard, movement broadcast |
| `game.instance` | Room lifecycle, enter/exit |
| `game.combat` | Tick loop, damage, skills, AI |
| `game.matchmaking` | Party queue |
| `economy.*` | Loot, inventory, craft, shop |
| `social.*` | Party, friend, guild, chat |
| `config` | Load item/skill/mob from DB |
| `admin` | Ban, grant, broadcast |

---

## Client (`client/`)

```
client/
├── Assets/
│   └── _Project/
│       ├── Scenes/
│       │   ├── Boot.unity
│       │   ├── Login.unity
│       │   ├── CharacterSelect.unity
│       │   ├── Hub.unity
│       │   └── Dungeon.unity
│       ├── Scripts/
│       │   ├── Core/
│       │   │   ├── GameManager.cs
│       │   │   ├── GameStateMachine.cs
│       │   │   └── ServiceLocator.cs
│       │   ├── Network/
│       │   │   ├── RestApiService.cs
│       │   │   ├── WebSocketService.cs
│       │   │   ├── MessageDispatcher.cs
│       │   │   └── Messages/          # DTO mirror protocol
│       │   ├── Gameplay/
│       │   │   ├── Player/
│       │   │   ├── Combat/
│       │   │   ├── Mob/
│       │   │   └── Dungeon/
│       │   ├── UI/
│       │   │   ├── Screens/
│       │   │   └── HUD/
│       │   ├── Data/
│       │   │   └── ScriptableObjects/
│       │   └── Utils/
│       ├── Prefabs/
│       │   ├── Player/
│       │   ├── Mobs/
│       │   ├── UI/
│       │   └── VFX/
│       ├── Art/
│       ├── Audio/
│       └── Settings/
│           └── GameConfig/          # Loaded from server
├── Packages/
├── ProjectSettings/
└── user.keystore.example            # NOT real keystore
```

---

## Shared config (`shared/config/`)

JSON files for local dev reference — production loads from DB.

```
shared/config/
├── manifest.json       # version + file list
├── items.json
├── skills.json
├── mobs.json
├── dungeons.json
├── loot_tables.json
└── quests.json
```

---

## Docs (`docs/`)

See [README.md](README.md) for full index.

---

## Deployment artifacts (Phase 2+)

```
server/Dockerfile          → ECR/GCR image
.github/workflows/         → CI/CD
infra/                     → Terraform (optional Phase 3)
```

---

## Naming: environments

| Env | Branch | URL |
|-----|--------|-----|
| local | feature/* | localhost:8080 |
| staging | develop | staging.api.* |
| prod | main | api.* |

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial structure |
