# Coding Conventions

## Dungeon Delvers Online

---

## 1. General principles

- **Read before write** — match existing patterns in the module
- **Server is truth** — never trust client for gameplay outcomes
- **Config over code** — skill stats, drop rates in DB/JSON, not hardcoded
- **Small PRs** — one feature per branch, reviewable in 30 minutes
- **No premature abstraction** — extract when duplicated 3+ times

---

## 2. Git workflow

### Branches

| Branch | Purpose |
|--------|---------|
| `main` | Production-ready |
| `develop` | Integration |
| `feature/*` | New features |
| `fix/*` | Bug fixes |
| `docs/*` | Documentation only |

### Commit messages

```
<type>(<scope>): <subject>

<body optional>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`

Examples:

```
feat(auth): add guest login endpoint
fix(combat): reject skill cast when on cooldown
docs(tdd): update WebSocket message types
```

### Rules

- Never commit secrets, keystores, `.env`
- Never force push `main`
- Squash optional for feature branches

---

## 3. Java / Spring Boot (server)

### Package structure

```
com.dungeondelvers
├── app                 # Spring Boot main, config
├── auth
├── game
│   ├── hub
│   ├── instance
│   ├── combat
│   └── social
├── economy
├── persistence
│   ├── entity
│   └── repository
├── api
│   ├── rest
│   └── ws
└── common
    ├── exception
    ├── dto
    └── util
```

### Naming

| Element | Convention | Example |
|---------|------------|---------|
| Class | PascalCase | `CombatTickService` |
| Interface | PascalCase, no `I` prefix | `LootService` |
| Method | camelCase | `rollLootForMob` |
| Constant | UPPER_SNAKE | `MAX_PARTY_SIZE` |
| DB table | snake_case | `inventory_items` |
| REST path | kebab-case | `/api/v1/battle-pass` |
| WS type | UPPER_SNAKE | `SKILL_CAST` |

### Layer rules

```
Controller/Handler → Service → Repository
                      ↓
                   Domain model
```

- Controllers: no business logic
- Services: transactional boundaries (`@Transactional`)
- Repositories: data access only

### DTOs

- REST request/response: `*Request`, `*Response`, `*Dto`
- WS payload: `*Payload` in `api.ws.payload`
- Never expose JPA entities directly in API

### Error handling

- Custom exceptions: `GameException` + `ErrorCode` enum
- Map to HTTP status in `@ControllerAdvice`
- WS errors: `ERROR` message with code (not stack trace)

### Logging

```java
log.info("Dungeon started instanceId={} dungeonId={}", instanceId, dungeonId);
log.warn("Suspicious speed characterId={} delta={}", characterId, delta);
log.error("Failed to persist loot characterId={}", characterId, e);
```

- Structured key=value in message
- Never log passwords, tokens, full receipts

### Tests

- Unit: JUnit 5 + Mockito
- Integration: `@SpringBootTest` + Testcontainers (PostgreSQL, Redis)
- Name: `methodName_condition_expectedResult`

---

## 4. C# / Unity (client)

### Folder & namespace

```
Namespace: DungeonDelvers.{Module}
Example:   DungeonDelvers.Network
```

### Naming

| Element | Convention | Example |
|---------|------------|---------|
| Class | PascalCase | `NetworkService` |
| Interface | `I` prefix | `INetworkService` |
| Method | PascalCase | `SendSkillCast` |
| Private field | `_camelCase` | `_wsClient` |
| SerializeField | `_camelCase` | `[SerializeField] private Button _loginBtn` |
| ScriptableObject | PascalCase + suffix | `SkillDefinitionSO` |

### Unity rules

- No gameplay logic in `Update` that should be server-side
- Use `FixedUpdate` for movement intent send
- Cache `GetComponent` in `Awake`
- Object pool for VFX/projectiles
- Avoid `FindObjectOfType` in Update

### Async

- REST: `async Task` + `UnityWebRequest` or `HttpClient`
- WS: dedicated thread/background receive → `ConcurrentQueue` → drain in `Update`

### SerializeField over public

```csharp
[SerializeField] private float _moveSpeed = 5f;
public float MoveSpeed => _moveSpeed;
```

---

## 5. SQL / Flyway

- Files: `V{version}__{description}.sql` — double underscore
- Never edit applied migrations — add new version
- Use `snake_case` for all identifiers
- Always add indexes for FK and query columns
- Comments for non-obvious constraints

---

## 6. JSON / Config

- Keys: `camelCase` in JSON files
- IDs: `snake_case` string (`warrior_charge`, `mob_goblin_grunt`)
- Version field required in config bundles

---

## 7. API contract

- REST changes → update `docs/api/openapi.yaml` first
- WS changes → update `docs/network-protocol.md` first
- Breaking change → bump version, note in changelog

---

## 8. Code review checklist

- [ ] Server validates all client input
- [ ] No magic numbers (use config/constants)
- [ ] Transaction boundary correct for gold/item changes
- [ ] Idempotent for purchase/loot grant
- [ ] Logs without secrets
- [ ] Docs updated if API/protocol changed
- [ ] No scope creep beyond task

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial conventions |
