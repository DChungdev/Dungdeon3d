# ADR-002: Raw WebSocket + JSON (not STOMP, not binary)

| Status | Accepted |
|--------|----------|
| Date | 2026-06-09 |

## Context

Realtime gameplay cần low latency trên mobile. Cần protocol dễ debug cho solo dev.

## Decision

- Transport: **raw WebSocket** (Spring WebSocket handler)
- Payload: **JSON UTF-8** MVP
- Message envelope: `{ type, seq, payload }`

## Alternatives considered

| Option | Rejected because |
|--------|------------------|
| STOMP | Extra framing overhead, harder custom game messages |
| MessagePack | Harder debug, defer to Phase 3 if bandwidth issue |
| UDP/ENet | Too complex for solo, mobile NAT issues |
| Photon/FishNet | Vendor lock-in, server-authoritative requirement |

## Consequences

- Bandwidth ~2× vs binary — acceptable for 4-player instance
- Easy inspect with browser DevTools / logs
- Migration path: add `Content-Type: application/msgpack` later

## Revisit when

- Bandwidth per player > 60 KB/s sustained
- Battery/network complaints from beta testers
