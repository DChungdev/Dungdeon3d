# Unity Client Guide

## Dungeon Delvers Online

---

## 1. Project creation

1. Unity Hub → **2022.3 LTS** → New project → **3D (URP)**
2. Project path: `ProjectGame/client/`
3. Product Name: `Dungeon Delvers Online`

### Required modules (Unity Hub → Installs → Gear)

- Android Build Support
- Android SDK & NDK Tools
- OpenJDK

---

## 2. Project settings

### Player Settings → Android

| Setting | Value |
|---------|-------|
| Package Name | `com.yourstudio.dungeondelvers` |
| Minimum API Level | 24 |
| Target API Level | 33+ |
| Scripting Backend | Mono (dev), IL2CPP (release) |
| Internet Access | Required |
| Write Permission | External (SDCard) — Optional |

### Quality / URP

- URP Asset: Balanced for mobile
- Target frame rate: 60 (cap 30 on low-end via settings)

### Input

- **Input System Package** (new) — install via Package Manager
- Action maps: `Gameplay`, `UI`

---

## 3. Recommended packages

| Package | Purpose | Phase |
|---------|---------|-------|
| Input System | Touch joystick | P0 |
| TextMeshPro | UI text | P0 |
| Addressables | Asset bundles | P1 |
| Newtonsoft Json | WS/REST JSON | P0 |
| Unity Web Request | REST (built-in) | P0 |

Install via **Window → Package Manager**.

### manifest.json additions

```json
{
  "com.unity.inputsystem": "1.7.0",
  "com.unity.textmeshpro": "3.0.6",
  "com.unity.addressables": "1.21.0",
  "com.unity.nuget.newtonsoft-json": "3.2.1"
}
```

---

## 4. Scene flow

```
Boot → Login → CharacterSelect → Hub → Dungeon → (Result overlay) → Hub
```

| Scene | Responsibility |
|-------|----------------|
| Boot | Init services, check config version, load user token |
| Login | REST auth |
| CharacterSelect | List/create character |
| Hub | WS hub zone, social UI |
| Dungeon | WS instance, combat |

`Boot` is index 0 in Build Settings.

---

## 5. Core services (singleton)

Initialize in `Boot`:

```csharp
// Pseudocode — implement Phase 0
ServiceLocator.Register<IRestApiService>(new RestApiService(config));
ServiceLocator.Register<IWebSocketService>(new WebSocketService(config));
ServiceLocator.Register<IGameStateMachine>(new GameStateMachine());
ServiceLocator.Register<IConfigManager>(new ConfigManager());
```

### RestApiService

- Base URL from `ScriptableObject` `ServerConfig`
- Bearer token header
- Wrapper parse `{ success, data, error, serverTime }`

### WebSocketService

- Connect after login with token query param
- Send queue when disconnected
- `OnMessage` → `MessageDispatcher`

### MessageDispatcher

```csharp
// Register handlers per type
Register("ENTITY_UPDATE", HandleEntityUpdate);
Register("DAMAGE_EVENT", HandleDamageEvent);
```

---

## 6. Network config

Create `Assets/_Project/Settings/ServerConfig.asset`:

| Field | Dev value |
|-------|-----------|
| RestBaseUrl | `http://10.0.2.2:8080` (Android emulator) |
| WsBaseUrl | `ws://10.0.2.2:8080/ws/game` |
| UseTls | false |

Physical device: use machine LAN IP e.g. `http://192.168.1.100:8080`

### Android cleartext (dev only)

`Assets/Plugins/Android/AndroidManifest.xml`:

```xml
<application android:usesCleartextTraffic="true" ...>
```

Remove in production — WSS only.

---

## 7. Movement & prediction (Phase 0)

1. Local player: `CharacterController` or `Rigidbody` move on input
2. `FixedUpdate`: send `MOVE_INTENT` with position, velocity, rotation
3. Remote players: interpolate toward server `ENTITY_UPDATE`
4. Correction: if error > 1.5m, lerp snap 200ms

---

## 8. Combat presentation

- Server sends `DAMAGE_EVENT` → spawn damage number (cosmetic only)
- Server sends `SKILL_RESULT` → play VFX if success
- **Never** apply damage locally without server event

---

## 9. Folder conventions

See [project-structure.md](project-structure.md).

- Scripts: one class per file, match filename
- Prefabs: `pf_` prefix optional
- Scenes: PascalCase

---

## 10. Build workflow

### Dev APK

```
File → Build Settings → Android → Development Build → Build
```

### Versioning

`bundleVersion`: `0.1.0` (Phase 0), `0.2.0` (Phase 1), ...

Align with server `configVersion` for compatibility check.

---

## 11. Debugging

| Tool | Use |
|------|-----|
| Unity Profiler | FPS, memory |
| Frame Debugger | Draw calls |
| Android Logcat | Device logs |
| ParrelSync (optional) | Multi-editor test |

### Network debug overlay (Phase 0)

- RTT display
- Last WS message type
- Server tick number

---

## 12. Phase 0 client scope

**Build:**
- Boot + Login scene
- Greybox dungeon 1 room
- Joystick + 1 skill button
- Debug inventory text

**Do not build yet:**
- Hub scene polish
- Addressables
- Full UI theme
- Mage class

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial Unity guide |
