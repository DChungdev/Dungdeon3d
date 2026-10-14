# HTTP API Examples — Phase 0

Base URL: `http://localhost:8080`

---

## Health

```bash
curl -s http://localhost:8080/health | jq
```

---

## Register

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"delver@example.com","password":"password123"}' | jq
```

---

## Login

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"delver@example.com","password":"password123"}' \
  | jq -r '.data.accessToken')

echo $TOKEN
```

---

## Create character

```bash
curl -s -X POST http://localhost:8080/api/v1/characters \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"DelverOne","classType":"WARRIOR"}' | jq
```

---

## List characters

```bash
curl -s http://localhost:8080/api/v1/characters \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Get inventory

```bash
CHAR_ID="<character-uuid-from-above>"

curl -s "http://localhost:8080/api/v1/characters/$CHAR_ID/inventory" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Game config manifest

```bash
curl -s http://localhost:8080/api/v1/config/game | jq
```

---

## Refresh token

```bash
REFRESH=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"delver@example.com","password":"password123"}' \
  | jq -r '.data.refreshToken')

curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}" | jq
```

---

## WebSocket (after login)

```
ws://localhost:8080/ws/game?token=<accessToken>
```

First message from server: `CONNECTED`

Client ping:

```json
{"type":"PING","seq":1,"payload":{}}
```

Join instance (Phase 0):

```json
{"type":"JOIN_INSTANCE","seq":2,"payload":{"dungeonId":"dungeon_goblin_cave","difficulty":"NORMAL"}}
```
