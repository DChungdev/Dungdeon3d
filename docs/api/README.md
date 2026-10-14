# API Documentation

## Files

| File | Description |
|------|-------------|
| [openapi.yaml](openapi.yaml) | OpenAPI 3.0 REST contract |
| [http-examples.md](http-examples.md) | curl examples Phase 0 |

## WebSocket

Realtime protocol: [../network-protocol.md](../network-protocol.md)

## Phase markers in OpenAPI

- `[P0]` — Phase 0 Prototype (implement first)
- `[P1]` — Phase 1 Vertical Slice
- `[P2]` — Phase 2 Alpha
- `[P3]` — Phase 3 Beta

## View OpenAPI

- Swagger UI: import `openapi.yaml` at [editor.swagger.io](https://editor.swagger.io)
- IntelliJ: OpenAPI plugin
- VS Code: OpenAPI (Swagger) Editor extension

## Contract-first workflow

1. Update `openapi.yaml`
2. Implement server endpoint
3. Implement client `RestApiService` method
4. Test with curl / Bruno
