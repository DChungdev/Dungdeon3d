# ADR-001: Modular Monolith over Microservices

| Status | Accepted |
|--------|----------|
| Date | 2026-06-09 |
| Context | Solo developer, MVP 100 CCU |

## Context

Dungeon Delvers Online cần realtime gameplay (WebSocket) và REST API. Team chỉ 1 developer + AI.

## Decision

Deploy **một Spring Boot JAR** với package modules rõ ràng (`auth`, `game`, `economy`, `social`), không tách microservices cho tới khi vượt ~500 CCU.

## Rationale

- Giảm operational overhead (1 deploy, 1 log stream)
- Debug dễ hơn — single process cho combat tick
- Đủ scale vertical + 2 game nodes cho 100 CCU
- Tách service sau khi có metric bottleneck thực tế

## Consequences

**Positive:**
- Faster Phase 0 delivery
- Lower cloud cost MVP
- Simpler local dev

**Negative:**
- Combat tick và REST share JVM — cần monitor tick jitter
- Scale path requires refactor to split instance service later

## Revisit when

- CCU > 300 sustained
- Combat tick p99 > 50ms consistently
- Team > 2 backend developers
