# ADR-003: Personal Loot, No Player Trade (MVP)

| Status | Accepted |
|--------|----------|
| Date | 2026-06-09 |

## Context

Party loot distribution ảnh hưởng social dynamics, economy control, và implementation complexity.

## Decision

- **Personal loot**: mỗi player roll bảng riêng khi mob chết
- **No trade / auction house** trong MVP
- **Bind on equip** cho Rare+

## Rationale

- Giảm toxicity (need/greed drama)
- Economy kiểm soát được — không có RMT path
- Server implementation đơn giản — grant trực tiếp inventory
- Phù hợp mobile casual session

## Consequences

- Less "MMO social" depth từ trading
- Dupe exploit surface nhỏ hơn
- Party không cần loot roll UI

## Revisit when

- Season 3+ nếu community request trade
- Cần implement mail system + trade window + audit
