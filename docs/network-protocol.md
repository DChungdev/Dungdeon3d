# Network Protocol Specification v1

## Dungeon Delvers Online — WebSocket Protocol

| Version | 1.0 |
|---------|-----|
| Transport | WebSocket Secure (WSS) |
| Payload | JSON (UTF-8) |
| Max message size | 64 KB |

REST API: [api/openapi.yaml](api/openapi.yaml)

---

## 1. Connection

### 1.1 Endpoint

```
wss://{host}/ws/game?token={accessToken}
```

Phase 0 local: `ws://localhost:8080/ws/game?token=...`

### 1.2 Handshake flow

1. Client obtains JWT via `POST /api/v1/auth/login`
2. Client opens WSS with `token` query param
3. Server validates JWT → sends `CONNECTED`
4. Client sends `JOIN_HUB` or `JOIN_INSTANCE` (Phase 0)

### 1.3 Heartbeat

- Client sends `PING` every **5 seconds**
- Server replies `PONG`
- Miss **3** consecutive PONGs → client reconnect
- Server disconnects if no message in **30 seconds**

### 1.4 Reconnect

1. Client reconnect WSS with fresh/refreshed token
2. Send `RECONNECT` with `lastSeq`, `zoneId`, `zoneType`
3. Server resend zone snapshot if room still exists (60s grace)

---

## 2. Message Envelope

### 2.1 Client → Server

```json
{
  "type": "SKILL_CAST",
  "seq": 1042,
  "ts": 1717939200123,
  "payload": {}
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | string | yes | Message type enum |
| seq | int | yes | Monotonic per connection, starts at 1 |
| ts | long | no | Client epoch ms (debug only) |
| payload | object | yes | Type-specific body |

### 2.2 Server → Client

```json
{
  "type": "DAMAGE_EVENT",
  "seq": 8821,
  "serverTs": 1717939200456,
  "ackRequired": false,
  "payload": {}
}
```

| Field | Type | Description |
|-------|------|-------------|
| seq | int | Server sequence |
| serverTs | long | Server epoch ms |
| ackRequired | bool | Client must send `ACK` |
| payload | object | Type-specific body |

### 2.3 ACK (reliable events)

Client → Server:

```json
{
  "type": "ACK",
  "seq": 1043,
  "payload": {
    "ackSeq": 8821
  }
}
```

Used for: `INSTANCE_COMPLETE`, rare loot drop, purchase confirm.

---

## 3. Error Format

```json
{
  "type": "ERROR",
  "seq": 9001,
  "serverTs": 1717939200999,
  "payload": {
    "code": "SKILL_ON_COOLDOWN",
    "message": "Skill not ready",
    "refSeq": 1042
  }
}
```

### Error codes

| Code | Description |
|------|-------------|
| `INVALID_MESSAGE` | Schema validation failed |
| `UNAUTHORIZED` | Token invalid/expired |
| `NOT_IN_ZONE` | Action wrong zone |
| `SKILL_ON_COOLDOWN` | CD active |
| `INSUFFICIENT_MP` | Not enough MP |
| `TARGET_INVALID` | Target dead/out of range |
| `RATE_LIMITED` | Too many messages |
| `PARTY_NOT_LEADER` | Only leader can queue |
| `INSTANCE_NOT_FOUND` | Room destroyed |
| `INVENTORY_FULL` | Loot could not be placed (bag has 40 slots) |
| `MAINTENANCE` | Server maintenance |

---

## 4. Shared Types

### 4.1 Vec3

```json
{ "x": 1.5, "y": 0.0, "z": -3.2 }
```

`y` ignored for gameplay logic (XZ plane).

### 4.2 EntitySnapshot

```json
{
  "entityId": "550e8400-e29b-41d4-a716-446655440000",
  "entityType": "PLAYER",
  "name": "DelverOne",
  "level": 5,
  "classType": "WARRIOR",
  "position": { "x": 0, "y": 0, "z": 0 },
  "rotation": 90.0,
  "hp": 800,
  "maxHp": 1000,
  "state": "IDLE"
}
```

Players also carry `mp` / `maxMp`; mobs carry `defId` (e.g. `mob_goblin_grunt`) instead of
`classType`. Absent fields are omitted.

`entityType`: `PLAYER` | `MOB` | `BOSS` | `PROJECTILE` | `INTERACTABLE`

`state`: `IDLE` | `MOVING` | `ATTACKING` | `CASTING` | `DOWNED` | `DEAD`

### 4.3 EntityType enum

See §4.2.

---

## 5. Connection Messages

### CONNECTED (S→C)

```json
{
  "type": "CONNECTED",
  "payload": {
    "sessionId": "uuid",
    "serverTime": 1717939200123,
    "configVersion": "1.0.0",
    "characterId": "uuid"
  }
}
```

### PING (C→S) / PONG (S→C)

```json
{ "type": "PING", "seq": 100, "payload": {} }
{ "type": "PONG", "seq": 101, "payload": { "serverTime": 1717939200123 } }
```

### KICK (S→C)

```json
{
  "type": "KICK",
  "payload": {
    "reason": "BANNED",
    "message": "Account suspended"
  }
}
```

### RECONNECT (C→S)

```json
{
  "type": "RECONNECT",
  "seq": 1,
  "payload": {
    "lastSeq": 5000,
    "zoneType": "INSTANCE",
    "zoneId": "instance-uuid"
  }
}
```

---

## 6. Hub Messages

### JOIN_HUB (C→S)

```json
{ "type": "JOIN_HUB", "seq": 10, "payload": {} }
```

### HUB_STATE (S→C) — full snapshot

```json
{
  "type": "HUB_STATE",
  "payload": {
    "shardId": "hub-shard-1",
    "entities": [],
    "npcs": [
      { "npcId": "vendor", "position": { "x": 5, "y": 0, "z": 0 } }
    ]
  }
}
```

### MOVE_INTENT (C→S)

```json
{
  "type": "MOVE_INTENT",
  "seq": 11,
  "payload": {
    "position": { "x": 1.0, "y": 0, "z": 2.0 },
    "velocity": { "x": 0, "y": 0, "z": 1.0 },
    "rotation": 45.0,
    "inputSeq": 42
  }
}
```

Rate: **20 Hz** max. Server drops excess.

### ENTITY_UPDATE (S→C) — delta batch

```json
{
  "type": "ENTITY_UPDATE",
  "payload": {
    "tick": 12345,
    "entities": [
      {
        "entityId": "uuid",
        "position": { "x": 1.1, "y": 0, "z": 2.1 },
        "rotation": 45.0,
        "state": "MOVING"
      }
    ]
  }
}
```

Rate: **10 Hz**. Only entities that changed since the last tick are included.

Every entity in the batch carries `hp`; players also `mp`.

Player entities also carry `lastInputSeq`: the last `MOVE_INTENT.inputSeq` the server
applied. The owning client drops inputs ≤ `lastInputSeq` from its prediction buffer and
replays the rest on top of the server position (reconciliation).

Server movement validation (Phase 0): elapsed time earns a distance budget at
`moveSpeed × 1.1` (capped at 0.5 s worth), each move spends it, with a one-off 0.1 m
tolerance. A move beyond the budget is cut short: the player is moved as far as allowed
towards the target. Packets bunched by network jitter are therefore not corrected.
Positions are clamped to the room bounds (Phase 0 greybox: x, z ∈ [-20, 20]).

### ENTITY_SPAWN / ENTITY_DESPAWN (S→C)

```json
{
  "type": "ENTITY_SPAWN",
  "payload": { "entity": { } }
}
```

### CHAT_SEND (C→S)

```json
{
  "type": "CHAT_SEND",
  "payload": {
    "channel": "HUB",
    "message": "Hello!"
  }
}
```

`channel`: `HUB` | `PARTY` | `GUILD`

### CHAT_MSG (S→C)

```json
{
  "type": "CHAT_MSG",
  "payload": {
    "channel": "HUB",
    "senderId": "uuid",
    "senderName": "DelverOne",
    "message": "Hello!",
    "timestamp": 1717939200123
  }
}
```

### EMOTE (C→S / S→C)

```json
{
  "type": "EMOTE",
  "payload": {
    "emoteId": "wave",
    "entityId": "uuid"
  }
}
```

---

## 7. Party Messages

### PARTY_CREATE (C→S)

```json
{ "type": "PARTY_CREATE", "payload": {} }
```

### PARTY_INVITE (C→S)

```json
{
  "type": "PARTY_INVITE",
  "payload": { "targetCharacterName": "DelverTwo" }
}
```

### PARTY_INVITE_NOTIFY (S→C)

```json
{
  "type": "PARTY_INVITE_NOTIFY",
  "payload": {
    "partyId": "uuid",
    "inviterName": "DelverOne",
    "inviterId": "uuid"
  }
}
```

### PARTY_ACCEPT / PARTY_DECLINE (C→S)

```json
{
  "type": "PARTY_ACCEPT",
  "payload": { "partyId": "uuid" }
}
```

### PARTY_UPDATE (S→C)

```json
{
  "type": "PARTY_UPDATE",
  "payload": {
    "partyId": "uuid",
    "leaderId": "uuid",
    "members": [
      {
        "characterId": "uuid",
        "name": "DelverOne",
        "level": 5,
        "classType": "WARRIOR",
        "online": true
      }
    ]
  }
}
```

### PARTY_LEAVE / PARTY_KICK (C→S)

```json
{ "type": "PARTY_LEAVE", "payload": {} }
{ "type": "PARTY_KICK", "payload": { "targetCharacterId": "uuid" } }
```

### DUNGEON_QUEUE (C→S) — leader only

```json
{
  "type": "DUNGEON_QUEUE",
  "payload": {
    "dungeonId": "dungeon_goblin_cave",
    "difficulty": "NORMAL"
  }
}
```

`difficulty`: `NORMAL` | `HARD`

### QUEUE_UPDATE (S→C)

```json
{
  "type": "QUEUE_UPDATE",
  "payload": {
    "status": "SEARCHING",
    "position": 1,
    "estimatedWaitSec": 10
  }
}
```

`status`: `SEARCHING` | `MATCHED` | `CANCELLED` | `TIMEOUT`

### QUEUE_MATCHED (S→C)

```json
{
  "type": "QUEUE_MATCHED",
  "payload": {
    "instanceId": "uuid",
    "dungeonId": "dungeon_goblin_cave",
    "difficulty": "NORMAL"
  }
}
```

---

## 8. Instance Messages

### JOIN_INSTANCE (C→S) — Phase 0 direct join

```json
{
  "type": "JOIN_INSTANCE",
  "payload": {
    "dungeonId": "dungeon_goblin_cave",
    "difficulty": "NORMAL"
  }
}
```

Phase 0: bypass party queue for testing.

### INSTANCE_ENTER (S→C)

```json
{
  "type": "INSTANCE_ENTER",
  "payload": {
    "instanceId": "uuid",
    "dungeonId": "dungeon_goblin_cave",
    "difficulty": "NORMAL",
    "seed": 12345678,
    "roomTemplateIds": ["room_a", "room_b", "room_boss"],
    "entities": [],
    "localPlayerId": "uuid"
  }
}
```

### INSTANCE_START (S→C)

```json
{
  "type": "INSTANCE_START",
  "payload": {
    "countdownSec": 3,
    "startTime": 1717939205000
  }
}
```

### SKILL_CAST (C→S)

```json
{
  "type": "SKILL_CAST",
  "payload": {
    "skillId": "warrior_charge",
    "targetEntityId": "uuid",
    "aimPosition": { "x": 5, "y": 0, "z": 3 }
  }
}
```

`targetEntityId` nullable for self/AOE skills.

### AUTO_ATTACK_TARGET (C→S)

```json
{
  "type": "AUTO_ATTACK_TARGET",
  "payload": { "targetEntityId": "uuid" }
}
```

Send `null` target to clear.

### SKILL_RESULT (S→C)

```json
{
  "type": "SKILL_RESULT",
  "payload": {
    "skillId": "warrior_charge",
    "success": false,
    "reason": "SKILL_ON_COOLDOWN",
    "cooldownEndTick": 12500,
    "cooldownRemainingMs": 6200,
    "mpRemaining": 150
  }
}
```

Sent to the caster only. `reason` (failures only): `SKILL_ON_COOLDOWN` | `INSUFFICIENT_MP` |
`TARGET_INVALID` | `UNKNOWN_SKILL`. `cooldownRemainingMs` lets the client show the cooldown
without knowing the tick rate. Phase 0: `warrior_charge` deals damage only; the dash /
knockback is deferred.

### DAMAGE_EVENT (S→C)

Broadcast to the room. `skillId` is `null` for auto-attacks.

```json
{
  "type": "DAMAGE_EVENT",
  "payload": {
    "sourceEntityId": "uuid",
    "targetEntityId": "uuid",
    "amount": 120,
    "crit": false,
    "skillId": "warrior_charge",
    "targetHpRemaining": 680
  }
}
```

### BUFF_APPLY (S→C)

```json
{
  "type": "BUFF_APPLY",
  "payload": {
    "targetEntityId": "uuid",
    "buffId": "battle_cry",
    "durationSec": 8.0,
    "stacks": 1
  }
}
```

### BUFF_REMOVE (S→C)

```json
{
  "type": "BUFF_REMOVE",
  "payload": {
    "targetEntityId": "uuid",
    "buffId": "battle_cry"
  }
}
```

### ENTITY_DEATH (S→C)

```json
{
  "type": "ENTITY_DEATH",
  "payload": {
    "entityId": "uuid",
    "killerEntityId": "uuid"
  }
}
```

### PLAYER_DOWNED (S→C)

```json
{
  "type": "PLAYER_DOWNED",
  "payload": {
    "entityId": "uuid",
    "bleedoutEndTime": 1717939230000
  }
}
```

### REVIVE_START / REVIVE_CANCEL (C→S)

```json
{
  "type": "REVIVE_START",
  "payload": { "targetEntityId": "uuid" }
}
```

### REVIVE_COMPLETE (S→C)

```json
{
  "type": "REVIVE_COMPLETE",
  "payload": {
    "targetEntityId": "uuid",
    "hp": 300
  }
}
```

### OBJECTIVE_UPDATE (S→C)

```json
{
  "type": "OBJECTIVE_UPDATE",
  "payload": {
    "objectiveType": "KILL_ALL",
    "current": 5,
    "total": 8,
    "bossPhase": 2
  }
}
```

### LOOT_DROP (S→C) — personal

```json
{
  "type": "LOOT_DROP",
  "ackRequired": true,
  "payload": {
    "mobEntityId": "uuid",
    "items": [
      {
        "inventoryItemId": "uuid",
        "itemDefId": "mat_goblin_ear",
        "name": "Goblin Ear",
        "rarity": "COMMON",
        "quantity": 2
      }
    ],
    "gold": 10,
    "goldTotal": 120
  }
}
```

Sent only to players who damaged the mob (personal loot, ADR-003); each rolls separately.
The reward is already persisted when this is sent. `quantity` is the amount added by this
drop (it may have been stacked onto an existing item, so `inventoryItemId` can repeat).
A kill pays out at most once per character (`mob_kill_rewards` primary key).
If the bag is full the item is dropped (gold is still granted), `items` is empty and an
`ERROR` with code `INVENTORY_FULL` follows.

Phase 0: the server accepts `ACK` but does not resend unacknowledged events yet; the client
reloads the inventory over REST after leaving the instance.

### INSTANCE_COMPLETE (S→C)

```json
{
  "type": "INSTANCE_COMPLETE",
  "ackRequired": true,
  "payload": {
    "success": true,
    "clearTimeMs": 720000,
    "rank": "A",
    "xpAwarded": 500,
    "goldAwarded": 80,
    "lootSummary": []
  }
}
```

`rank`: `S` | `A` | `B` | `C` based on time vs par.

### INSTANCE_FAILED (S→C)

```json
{
  "type": "INSTANCE_FAILED",
  "payload": {
    "reason": "WIPE",
    "returnToHubInSec": 5
  }
}
```

### LEAVE_INSTANCE (C→S)

```json
{ "type": "LEAVE_INSTANCE", "payload": {} }
```

---

## 9. Rate Limits

| Action | Limit |
|--------|-------|
| MOVE_INTENT | 20/sec |
| SKILL_CAST | 5/sec |
| CHAT_SEND | 1 per 2 sec |
| PARTY_INVITE | 5/min |
| Global messages | 64 KB max |

---

## 10. Phase Implementation Matrix

| Message | P0 | P1 | P2 |
|---------|----|----|-----|
| CONNECTED, PING/PONG | ✓ | ✓ | ✓ |
| JOIN_INSTANCE | ✓ | ✓ | ✓ |
| MOVE_INTENT, ENTITY_UPDATE | ✓ | ✓ | ✓ |
| SKILL_CAST, DAMAGE_EVENT | ✓ | ✓ | ✓ |
| LOOT_DROP | ✓ | ✓ | ✓ |
| JOIN_HUB, HUB_STATE | | ✓ | ✓ |
| PARTY_* | | ✓ | ✓ |
| DUNGEON_QUEUE | | | ✓ |
| CHAT_* | | | ✓ |
| REVIVE_* | | | ✓ |
| INSTANCE_COMPLETE | | ✓ | ✓ |

---

## 11. Versioning

- Protocol version in `CONNECTED.configVersion`
- Breaking change → bump major, maintain backward compat 1 release
- Client ignore unknown message types (log warning)

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial protocol spec |
