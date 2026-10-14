# Game Config Specification

## Dungeon Delvers Online

Config được lưu trong PostgreSQL (`*_definitions` tables) và export JSON cho client sync.

---

## 1. Manifest

`shared/config/manifest.json`:

```json
{
  "version": "1.0.0",
  "hash": "sha256-of-all-files",
  "files": ["items.json", "skills.json", "mobs.json", "dungeons.json", "loot_tables.json", "quests.json"]
}
```

Client: `GET /api/v1/config/game` → compare version → download if stale.

---

## 2. Items (`items.json`)

```json
{
  "items": [
    {
      "id": "sword_iron",
      "name": "Iron Sword",
      "description": "Reliable iron blade.",
      "itemType": "WEAPON",
      "equipSlot": "WEAPON",
      "stackable": false,
      "maxStack": 1,
      "baseStats": {
        "atk": 15
      },
      "levelRequired": 5,
      "vendorPrice": 200
    }
  ]
}
```

### itemType enum

`WEAPON` | `ARMOR` | `ACCESSORY` | `MATERIAL` | `CONSUMABLE`

### equipSlot enum

`WEAPON` | `HELM` | `CHEST` | `GLOVES` | `BOOTS` | `ACCESSORY` | `null`

### baseStats keys

`atk`, `def`, `maxHp`, `maxMp`, `critRate`, `critDamage`, `moveSpeed`, `healHp`

---

## 3. Skills (`skills.json`)

```json
{
  "skills": [
    {
      "id": "warrior_charge",
      "classType": "WARRIOR",
      "name": "Charge",
      "description": "Rush to target dealing damage.",
      "cooldownSec": 8.0,
      "mpCost": 15,
      "baseDamage": 20,
      "atkScaling": 0.8,
      "targetType": "ENEMY",
      "rangeM": 8.0,
      "aoeRadiusM": 0,
      "effect": {
        "knockback": 2.0
      },
      "animationId": "anim_warrior_charge",
      "iconId": "icon_charge"
    }
  ]
}
```

### targetType enum

`SELF` | `ENEMY` | `ALLY` | `GROUND_AOE` | `POINT_BLANK_AOE`

### effect object (optional keys)

| Key | Type | Description |
|-----|------|-------------|
| `stunSec` | float | Stun duration |
| `slowPercent` | float | 0–1 |
| `durationSec` | float | Buff/debuff duration |
| `knockback` | float | Knockback distance |
| `buffId` | string | Reference buff |
| `atkPercent` | float | ATK buff |
| `absorbPercent` | float | Shield |
| `hits` | int | Multi-hit |
| `channelSec` | float | Channel time |

---

## 4. Mobs (`mobs.json`)

```json
{
  "mobs": [
    {
      "id": "mob_goblin_grunt",
      "name": "Goblin Grunt",
      "mobType": "NORMAL",
      "level": 1,
      "hp": 150,
      "atk": 8,
      "def": 3,
      "moveSpeed": 3.5,
      "aggroRadiusM": 8.0,
      "leashRadiusM": 20.0,
      "attackRangeM": 2.0,
      "aiProfile": "MELEE_BASIC",
      "lootTableId": "loot_goblin_normal",
      "prefabId": "pf_goblin_grunt"
    }
  ]
}
```

### mobType enum

`NORMAL` | `ELITE` | `BOSS`

### aiProfile enum

`MELEE_BASIC` | `MELEE_SLOW` | `RANGED_BASIC` | `BOSS_MELEE` | `BOSS_CASTER` | `BOSS_DRAGON`

---

## 5. Dungeons (`dungeons.json`)

```json
{
  "dungeons": [
    {
      "id": "dungeon_goblin_cave",
      "name": "Goblin Cave",
      "description": "A cave infested with goblins.",
      "minLevel": 1,
      "maxPartySize": 4,
      "roomCount": 3,
      "parTimeMs": 600000,
      "layoutType": "FIXED",
      "layout": {
        "rooms": ["goblin_room_1", "goblin_room_2", "goblin_boss"],
        "mobs": {
          "goblin_room_1": ["mob_goblin_grunt", "mob_goblin_grunt", "mob_goblin_archer"],
          "goblin_room_2": ["mob_goblin_grunt", "mob_goblin_shaman"],
          "goblin_boss": ["mob_boss_goblin_king"]
        }
      }
    }
  ]
}
```

### layoutType enum

`FIXED` | `PROCEDURAL`

### Procedural layout

```json
{
  "layoutType": "PROCEDURAL",
  "layout": {
    "roomTemplates": ["proc_room_empty", "proc_room_mob", "proc_room_elite", "proc_room_boss"],
    "minRooms": 4,
    "maxRooms": 7
  }
}
```

---

## 6. Loot tables (`loot_tables.json`)

```json
{
  "lootTables": [
    {
      "id": "loot_goblin_normal",
      "name": "Goblin Normal Drops",
      "entries": [
        {
          "itemDefId": "mat_goblin_ear",
          "weight": 400,
          "minQty": 1,
          "maxQty": 2,
          "minRarity": "COMMON"
        }
      ]
    }
  ]
}
```

### Roll algorithm (server)

```
totalWeight = sum(entry.weight)
roll = random(0, totalWeight)
iterate entries cumulative weight → pick item
qty = random(entry.minQty, entry.maxQty)
```

Boss: guaranteed至少 1 roll from table + bonus epic chance (see economy-design.md).

---

## 7. Quests (`quests.json`)

```json
{
  "quests": [
    {
      "id": "quest_daily_kill",
      "name": "Pest Control",
      "description": "Slay 30 monsters.",
      "questType": "KILL",
      "objective": { "count": 30 },
      "reward": { "gold": 50, "xp": 80 },
      "isDaily": true,
      "isTutorial": false
    }
  ]
}
```

### questType enum

`KILL` | `CLEAR_DUNGEON` | `COLLECT` | `CRAFT` | `TALK`

### objective examples

```json
{ "mobId": "mob_goblin_grunt", "count": 3 }
{ "dungeonId": "dungeon_goblin_cave" }
{ "itemDefId": "mat_bone", "count": 5 }
{ "count": 1 }
{ "step": 1 }
```

---

## 8. Character base stats (server-side)

Not in JSON files — defined in code constant, overridden by level + gear.

### WARRIOR base (level 1)

```json
{
  "maxHp": 1000,
  "maxMp": 100,
  "atk": 50,
  "def": 30,
  "critRate": 0.05,
  "critDamage": 1.5,
  "moveSpeed": 5.0,
  "attackSpeed": 1.0
}
```

### MAGE base (level 1)

```json
{
  "maxHp": 700,
  "maxMp": 200,
  "atk": 55,
  "def": 20,
  "critRate": 0.08,
  "critDamage": 1.5,
  "moveSpeed": 5.0,
  "attackSpeed": 0.9
}
```

### Level growth per level

```
+maxHp: 50 (WARRIOR) / 35 (MAGE)
+atk: 3
+def: 2
```

---

## 9. Config reload

1. Admin updates DB hoặc deploy new seed migration
2. Bump `config_meta.version` and `hash`
3. Server reload in-memory cache on startup or admin endpoint
4. Client checks on login → download new bundle

---

## 10. Validation rules

| Field | Rule |
|-------|------|
| id | `^[a-z][a-z0-9_]{2,49}$` |
| cooldownSec | 0.5 – 60 |
| weight | > 0 |
| hp | > 0 |
| levelRequired | 1 – 25 (MVP) |

Server rejects invalid config at load time — fail fast on deploy.

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial config spec |
