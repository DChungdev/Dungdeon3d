# Game Design Document (GDD) v1

## Dungeon Delvers Online

| Thuộc tính | Giá trị |
|------------|---------|
| Version | 1.0 |
| Ngày | 2026-06-09 |
| Genre | Co-op Action MMORPG (session-based) |
| Platform | Android (primary), iOS (defer) |
| Target audience | 18–35, mobile, thích co-op dungeon crawler |
| Session length | 10–20 phút / run |
| Monetization | Cosmetic + Battle Pass (no P2W) |

---

## 1. Vision

### 1.1 Elevator pitch

**Dungeon Delvers Online** là MMORPG mobile co-op: người chơi tụ tập tại hub town, lập party 4 người, và chinh phục dungeon để farm loot, craft gear, và leo bảng xếp hạng tuần.

### 1.2 USP (Unique Selling Points)

1. **Co-op 4 người real-time** — combat action, không auto-battle
2. **Dungeon run ngắn** — phù hợp mobile, chơi mọi lúc
3. **Fair monetization** — chỉ cosmetic + Battle Pass
4. **Seasonal affix** — nội dung mới mỗi tuần, dev cost thấp

### 1.3 Core loop

```
Login → Hub (social) → Party → Dungeon Run → Loot/XP → Craft/Upgrade → Hub
         ↑                                                              |
         └──────────────── Daily quest / Leaderboard / Battle Pass ─────┘
```

### 1.4 Design pillars

| Pillar | Mô tả |
|--------|--------|
| **Co-op first** | Mọi dungeon thiết kế cho 2–4 người |
| **Skill expression** | 4 skill + positioning, không chỉ tap stat |
| **Respect time** | 1 run 10–15 phút; daily 15–20 phút đủ progress |
| **Fair play** | Không bán sức mạnh |

---

## 2. World & Setting

### 2.1 Lore (tối giản)

Thế giới **Aetherra** — các Delver (thợ săn hầm ngục) khám phá dungeon nổi dưới các thành phố nổi. Hub **Skyhaven** là điểm hẹn trung lập.

Lore tối giản MVP — mở rộng qua seasonal story.

### 2.2 Hub — Skyhaven

- Không combat
- NPC: Dungeon portal, Vendor, Crafter, Guild registrar, Leaderboard board
- Max 30 người/shard
- Chat global, emote

### 2.3 Dungeons MVP

| ID | Tên | Room | Boss | Unlock |
|----|-----|------|------|--------|
| `dungeon_goblin_cave` | Goblin Cave | 3 | Goblin King | Level 1 |
| `dungeon_undead_crypt` | Undead Crypt | 4 | Lich | Level 10 |
| `dungeon_dragon_nest` | Dragon Nest | 5 | Young Dragon | Level 20, Hard |
| `dungeon_procedural` | Rift Dungeon | 4–7 | Random boss | Level 15 |

---

## 3. Player & Classes

### 3.1 Character

- 1 character / account (MVP) → 3 slot post-launch
- Tên 3–16 ký tự, unique server-wide
- Level cap MVP: **25**

### 3.2 Classes MVP

#### Warrior (Tank/DPS hybrid)

| Skill | Mô tả | CD |
|-------|--------|-----|
| Charge | Lao tới target, damage + knockback | 8s |
| Shield Bash | Stun 1.5s | 12s |
| Whirlwind | AOE xung quanh | 10s |
| Battle Cry | +20% ATK party 8s | 20s |

Role: frontline, absorb aggro, CC.

#### Mage (Ranged DPS)

| Skill | Mô tả | CD |
|-------|--------|-----|
| Firebolt | Single target burst | 3s |
| Frost Nova | AOE slow 40% 3s | 12s |
| Arcane Missile | Channel 3 hit | 8s |
| Mana Shield | Absorb damage 5s | 18s |

Role: burst damage, AOE clear.

### 3.3 Class defer

- Archer, Priest — Season 2+

---

## 4. Combat System

### 4.1 Overview

- Real-time action, server-authoritative
- Auto-attack khi có target lock
- 4 skill button + dodge (defer dodge MVP — dùng movement)
- Target lock toggle

### 4.2 Stats

| Stat | Mô tả |
|------|--------|
| HP | Health |
| MP | Skill resource |
| ATK | Damage output |
| DEF | Damage reduction |
| CRIT | Crit chance / crit damage |
| Move Speed | Movement |

### 4.3 Damage & TTK

- Mob thường: 3–8 giây kill (party)
- Elite: 15–20 giây
- Boss: 2–4 phút (party 4, appropriate gear)

### 4.4 Death & Revive

- HP = 0 → **Downed** (không chết ngay)
- Bleedout 30 giây
- Ally revive: cast 3s → 30% HP
- Full party downed → fail run

### 4.5 Difficulty

| Mode | Modifier | Reward |
|------|----------|--------|
| Normal | 1.0× | Standard loot |
| Hard | 1.5× mob HP/ATK | +Rare rate, leaderboard eligible |

---

## 5. Progression System

### 5.1 Leveling (1–25)

| Level range | Nguồn XP chính | Thời gian ước tính |
|-------------|----------------|-------------------|
| 1–10 | Tutorial + Goblin Cave | 2–3 giờ |
| 10–20 | Crypt + daily | 8–12 giờ |
| 20–25 | Dragon Nest + Hard | 6–10 giờ |

XP curve: `xpToNext = 100 * level^1.5` (làm tròn)

### 5.2 Gear progression

| Tier | Level range | Rarity |
|------|-------------|--------|
| Tier 1 | 1–10 | Common, Rare |
| Tier 2 | 10–20 | Rare, Epic |
| Tier 3 | 20–25 | Epic |

6 equip slots: Weapon, Helm, Chest, Gloves, Boots, Accessory.

### 5.3 Enhancement

- +1 to +3 bằng gold
- 100% success MVP (no fail, no break)
- Mỗi +1: +5% base stat item

### 5.4 Crafting

- Material drop từ dungeon
- 15 recipe MVP: weapon, armor, consumable
- Craft tại hub station

### 5.5 Power sources (tổng hợp)

```
Total Power = Base level stats + Equipment stats + Enhancement + Buffs (trong run)
```

Không có paid stat.

---

## 6. Items & Loot

### 6.1 Rarity

| Rarity | Color | Drop rate (mob thường) |
|--------|-------|------------------------|
| Common | White | 70% |
| Rare | Blue | 25% |
| Epic | Purple | 5% |

Boss: guaranteed Rare+, 15% Epic.

### 6.2 Personal loot

Mỗi player nhận loot riêng — không tranh đồ, giảm toxicity.

### 6.3 Inventory

- 40 slot
- Stack material max 999
- Equip bind on equip

---

## 7. Social Systems

### 7.1 Party

- Max 4 members
- Leader start dungeon
- Invite by name hoặc quick match queue

### 7.2 Friends

- Add/remove, online status
- See in-dungeon status

### 7.3 Guild

- Max 30 members MVP
- Guild tag hiển thị nameplate
- Guild chat
- Guild buff +5% XP (Season 1, không P2W)

### 7.4 Chat

| Channel | Scope |
|---------|--------|
| Hub Global | Shard hub |
| Party | Instance + hub party |
| Guild | Guild members |

Rate limit: 5 msg / 10s.

---

## 8. Quest System

### 8.1 Tutorial (8 bước)

1. Di chuyển
2. Tương tác NPC
3. Auto-attack mob
4. Dùng skill
5. Clear room
6. Mở chest
7. Equip item
8. Complete first dungeon

Reward: weapon Rare class-appropriate.

### 8.2 Daily quest (×3)

Ví dụ pool:
- Clear 1 dungeon Normal
- Kill 30 mobs
- Craft 1 item
- Clear dungeon Hard
- Complete dungeon without wipe

Reward: gold + material + small XP.

Reset: 05:00 server time.

---

## 9. Leaderboard & Seasons

### 9.1 Weekly leaderboard

- Per dungeon Hard mode
- Rank by fastest clear time
- Party size recorded
- Reset Chủ nhật 00:00

### 9.2 Season

- Length: 4 tuần
- Weekly affix (2 at launch):
  - **Enraged**: mob +30% ATK
  - **Fortified**: mob +50% HP
- Season reward top 100: cosmetic title + frame

---

## 10. Economy

Chi tiết số tại [economy-design.md](economy-design.md).

### 10.1 Currencies

| Currency | Loại | Nguồn | Sink |
|----------|------|-------|------|
| Gold | Soft | Mob, quest, sell | Vendor, craft, enhance |
| Gem | Hard | IAP, Battle Pass | Cosmetic shop |

### 10.2 Gold flow (tóm tắt)

- Earn/run Normal: ~50–80 gold
- Earn/run Hard: ~100–150 gold
- Enhance +3: ~500 gold total per item
- Daily net positive nếu chơi 2–3 run

---

## 11. Monetization

### 11.1 Nguyên tắc

- **Không bán stat**
- **Không gacha loot box** MVP
- Cosmetic + convenience nhẹ

### 11.2 Revenue streams

| Stream | Mô tả | Giá gợi ý |
|--------|--------|-----------|
| Battle Pass Premium | 30 tier / season | $4.99 |
| Character skin | Visual only | $2.99–$9.99 |
| Weapon skin | Visual only | $1.99–$4.99 |
| Emote pack | 5 emote | $0.99 |
| Starter bundle | Skin + emote + small gems | $2.99 (1 lần) |
| Gem packs | 100/500/1200/2500 | $0.99–$19.99 |

### 11.3 Battle Pass

- Free track: gold, material, 1 skin tier 15
- Premium track: 2 skin, emote, weapon skin, title
- XP từ daily quest + weekly challenge

### 11.4 ARPDAU target (soft launch)

≥ $0.05 (cosmetic only, 100 DAU test)

---

## 12. UX Flow

```
Splash → Login/Register → Character Select/Create → Loading → Hub
  → Party Form → Queue → Loading → Dungeon → Result → Hub
```

### 12.1 FTUE (First Time User Experience)

- 15 phút đầu: tutorial quest → first dungeon clear → equip → hub explore
- Không hiện shop trước level 5
- Battle Pass intro level 8

### 12.2 Mobile UX

- Virtual joystick trái, skill phải
- Auto-aim nearest enemy (toggle)
- Camera auto-follow, pinch zoom (optional defer)

---

## 13. Content Scope MVP

| Content | Số lượng |
|---------|----------|
| Classes | 2 |
| Hub maps | 1 |
| Fixed dungeons | 3 |
| Procedural template | 1 |
| Mob types | 8 |
| Bosses | 4 |
| Items (equipment) | ~40 |
| Craft recipes | 15 |
| Daily quests | 8 pool |
| Achievements | 10 |
| Cosmetic skins | 10 (launch) |
| Emotes | 5 free + 5 premium |

---

## 14. Out of Scope MVP

- PvP
- Player trade / auction house
- Voice chat
- Housing
- Mount
- 3rd class
- iOS
- Bot party fill
- Cross-server
- Stamina / energy system
- Loot box gacha

---

## 15. KPI Targets

| Metric | Alpha | Beta | Launch |
|--------|-------|------|--------|
| D1 retention | 35% | 40% | 40% |
| D7 retention | 15% | 18% | 18% |
| Tutorial complete | 70% | 80% | 80% |
| Avg session | 18 min | 20 min | 20 min |
| Crash rate | <2% | <1% | <1% |
| CCU peak | 50 | 80 | 100 |

---

## 16. Risks (Design)

| Rủi ro | Mitigation |
|--------|------------|
| Matchmaking rỗng | Manual invite; Discord community |
| Content drought | Weekly affix; seasonal cosmetics |
| Class imbalance | Monthly balance patch |
| Pay perception | Zero stat IAP; transparent shop |

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial GDD |
