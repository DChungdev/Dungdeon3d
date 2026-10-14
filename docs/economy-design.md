# Economy Design — Dungeon Delvers Online

| Version | 1.0 |
|---------|-----|
| Ngày | 2026-06-09 |
| Liên kết | [GDD-v1.md](GDD-v1.md) · [TDD-v1.md](TDD-v1.md) |

---

## 1. Nguyên tắc

1. **Gold là soft currency** — earn in-game, không mua trực tiếp MVP
2. **Gem là hard currency** — IAP only, chỉ cosmetic + Battle Pass
3. **Sink ≥ Faucet** ở endgame để tránh inflation
4. **Personal loot** — không trade, kiểm soát economy dễ hơn

---

## 2. Currencies

### 2.1 Gold

| Thuộc tính | Giá trị |
|------------|---------|
| Cap | 9,999,999 |
| Tradeable | No |
| Purchasable | No (MVP) |

### 2.2 Premium Gems

| Thuộc tính | Giá trị |
|------------|---------|
| Cap | 99,999 |
| Tradeable | No |
| Purchasable | Yes (IAP) |

---

## 3. Gold Faucets (nguồn)

| Nguồn | Gold / lần | Lần/ngày | Gold/ngày max |
|-------|------------|----------|---------------|
| Mob thường | 2–5 | ~100 (4 run) | ~400 |
| Mob elite | 10–15 | ~20 | ~200 |
| Boss Normal | 30–50 | 4 | ~160 |
| Boss Hard | 60–80 | 2 | ~140 |
| Daily quest ×3 | 50 each | 1 | 150 |
| Sell junk Common | 5–10/item | — | ~100 |
| **Tổng casual** | | | **~800–1000** |
| **Tổng hardcore** | | | **~1500** |

---

## 4. Gold Sinks (tiêu thụ)

| Sink | Chi phí | Tần suất |
|------|---------|----------|
| Vendor buy consumable | 20–100 | Optional |
| Craft weapon T2 | 200–400 | 1–2/tuần |
| Enhance +1 | 50 | Per item |
| Enhance +2 | 150 | Per item |
| Enhance +3 | 300 | Per item |
| **Enhance full 6 slot +3** | **~3000** | One-time milestone |
| Guild create | 1000 | One-time |
| Repair (defer) | — | — |

### 4.1 Net balance theo level

| Level | Net gold/ngày | Mục đích |
|-------|---------------|----------|
| 1–10 | +500–800 | Tích lũy, craft T1 |
| 10–20 | +200–400 | Enhance sink bắt đầu |
| 20–25 | +0–200 | Sink ≈ faucet |

---

## 5. Drop Tables

### 5.1 Mob thường (Normal)

| Drop | Rate |
|------|------|
| Common equipment | 15% |
| Common material | 40% |
| Gold only | 45% |

### 5.2 Mob elite

| Drop | Rate |
|------|------|
| Rare equipment | 20% |
| Rare material | 35% |
| Common | 45% |

### 5.3 Boss

| Drop | Normal | Hard |
|------|--------|------|
| Guaranteed Rare+ | 100% | 100% |
| Epic | 5% | 15% |
| Material bundle | 100% | 100% (×1.5 qty) |

### 5.4 Personal loot roll

Mỗi player roll độc lập — party 4 ≠ 4× loot chung, nhưng 4 roll riêng.

---

## 6. Item Pricing (Vendor)

| Rarity | Buy | Sell (50%) |
|--------|-----|------------|
| Common | 80 | 40 |
| Rare | 300 | 150 |
| Epic | 1000 | 500 |
| Material | 20–50 | 10–25 |

---

## 7. Crafting Economy

| Recipe tier | Input value | Output value | Net |
|-------------|-------------|--------------|-----|
| T1 weapon | ~150 material | ~200 vendor | +50 (time sink) |
| T2 weapon | ~400 material | ~500 vendor | +100 |
| Consumable | ~30 | — | Utility |

Craft là **time sink** hơn là gold printer.

---

## 8. Gem Economy

### 8.1 Gem sources

| Nguồn | Gems |
|-------|------|
| IAP 100 pack | 100 |
| IAP 500 pack | 550 (+10%) |
| Battle Pass free track | 20 total |
| Battle Pass premium | 100 total |
| Achievement | 5–10 one-time |

### 8.2 Gem sinks (cosmetic only)

| Item | Gem cost |
|------|----------|
| Character skin | 300–800 |
| Weapon skin | 150–400 |
| Emote pack (5) | 100 |
| Battle Pass premium | 500 (or $4.99 IAP) |

### 8.3 Không có gem → gold conversion MVP

---

## 9. Battle Pass Economy

| Track | Value estimate | Price |
|-------|----------------|-------|
| Free | ~500 gold equiv + 1 skin | $0 |
| Premium | ~3000 gem equiv cosmetics | $4.99 |

Premium không chứa stat item.

---

## 10. Anti-inflation & Anti-bot

| Measure | Mô tả |
|---------|--------|
| Daily gold soft cap | 3000 (warn, không hard block MVP) |
| Audit log | Mọi gold change > 500 logged |
| Anomaly flag | >2000 gold/hour → review |
| No trade | Không player economy |
| No gold IAP | Không bán gold |

---

## 11. Spreadsheet formulas (implement reference)

```
# XP to next level
xp_next = floor(100 * pow(level, 1.5))

# Damage (combat)
raw = skill_base + atk * scaling
mitigated = raw * (100 / (100 + def))
final = max(1, floor(mitigated * (crit ? crit_damage : 1)))

# Enhance cost
enhance_cost(level) = 50 * pow(3, level - 1)  # 50, 150, 300

# Item vendor sell
sell_price = floor(buy_price * 0.5)
```

---

## 12. Tuning workflow

1. Thay đổi trong `shared/config/loot_tables.json` hoặc DB seed
2. Chạy spreadsheet simulator (manual MVP)
3. Closed test 1 tuần → đo gold/hour thực tế
4. Patch qua admin hot reload `loot_tables`

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Initial economy design |
