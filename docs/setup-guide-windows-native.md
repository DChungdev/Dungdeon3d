# Setup Guide — Windows Native (Không Docker)

Dành cho máy **không có / không cài được Docker Desktop**.

Phase 0 chỉ **bắt buộc PostgreSQL**. Redis có thể bỏ qua bằng profile `local-native` (session in-memory).

---

## Tổng quan 3 hướng

| Hướng | PostgreSQL | Redis | Độ khó | Khuyến nghị |
|-------|------------|-------|--------|-------------|
| **A — Tối thiểu** | Cài native | Không (in-memory) | Thấp | ★ Bắt đầu Phase 0 |
| **B — Đầy đủ local** | Cài native | Memurai | Trung bình | Trước Phase 1 |
| **C — Cloud dev** | Neon (free) | Upstash (free) | Thấp | Không muốn cài DB |

---

## Hướng A — PostgreSQL native + không Redis (Phase 0)

### Bước 1: Cài Java 21

1. Tải [Eclipse Temurin JDK 21](https://adoptium.net/) (Windows x64 MSI)
2. Cài đặt, tick **Set JAVA_HOME**
3. Kiểm tra PowerShell:

```powershell
java -version
```

Kết quả mong đợi: `openjdk version "21.x.x"`

---

### Bước 2: Cài PostgreSQL 16

1. Tải [PostgreSQL Windows installer](https://www.postgresql.org/download/windows/) (EDB installer, version 16)
2. Cài đặt:
   - Port: `5432` (mặc định)
   - Superuser password: **ghi nhớ** (ví dụ `postgres`)
   - Tick **pgAdmin 4** (optional, tiện xem DB)
   - Stack Builder: **bỏ qua**
3. Thêm `C:\Program Files\PostgreSQL\16\bin` vào PATH (nếu lệnh `psql` không chạy)

---

### Bước 3: Tạo database và user

Mở **SQL Shell (psql)** từ Start Menu, hoặc PowerShell:

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres
```

Chạy SQL (có thể copy từ `scripts/windows/init-postgres.sql`):

```sql
CREATE USER ddo WITH PASSWORD 'ddo';
CREATE DATABASE dungeon_delvers OWNER ddo;
GRANT ALL PRIVILEGES ON DATABASE dungeon_delvers TO ddo;
\c dungeon_delvers
GRANT ALL ON SCHEMA public TO ddo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ddo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ddo;
\q
```

**Hoặc chạy script tự động** (nhập password superuser `postgres` khi được hỏi):

```powershell
cd D:\ProjectGame
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -f scripts\windows\init-postgres.sql
```

---

### Bước 4: Cấu hình server (profile `local-native`)

```powershell
cd D:\ProjectGame\server\src\main\resources
copy application-local-native.yml.example application-local-native.yml
```

Chỉnh password nếu bạn không dùng `ddo/ddo`:

```yaml
spring:
  datasource:
    username: ddo
    password: ddo   # đổi nếu cần
```

Profile `local-native`:
- **Không cần Redis**
- Session / presence lưu **in-memory** (đủ Phase 0, 1 server process)
- Khi scale multi-node (Phase 2+) → thêm Redis

---

### Bước 5: Chạy server (sau khi scaffold Spring Boot)

```powershell
cd D:\ProjectGame\server
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-native"
```

Flyway tự tạo bảng từ `V001` + `V002`.

---

### Bước 6: Kiểm tra

```powershell
# Script kiểm tra môi trường
.\scripts\windows\verify-setup.ps1

# Sau server chạy
curl http://localhost:8080/health
```

Kiểm tra DB (sau server start lần đầu):

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U ddo -d dungeon_delvers -c "SELECT count(*) FROM skill_definitions;"
```

Kết quả mong đợi: `8`

---

## Hướng B — Thêm Redis local (Memurai)

Redis **không có bản chính thức Windows**. Dùng **Memurai** (tương thích Redis):

1. Tải [Memurai Developer](https://www.memurai.com/get-memurai) (miễn phí dev)
2. Cài → chạy như Windows Service (port `6379`)
3. Kiểm tra:

```powershell
# Nếu có redis-cli từ Memurai
memurai-cli ping
# → PONG
```

4. Dùng profile `local` thay vì `local-native`:

```powershell
copy application-local.yml.example application-local.yml
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

---

## Hướng C — Cloud free tier (không cài PostgreSQL/Redis)

Phù hợp nếu máy yếu hoặc không muốn cài gì ngoài Java.

### PostgreSQL — [Neon](https://neon.tech)

1. Tạo project free → copy connection string
2. Trong `application-local-native.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://ep-xxx.region.aws.neon.tech/dungeon_delvers?sslmode=require
    username: neon_user
    password: neon_password
```

### Redis — [Upstash](https://upstash.com)

1. Tạo Redis database free → copy endpoint + password
2. Profile `local` với:

```yaml
spring:
  data:
    redis:
      host: xxx.upstash.io
      port: 6379
      password: your-upstash-password
      ssl:
        enabled: true
```

**Lưu ý:** Cần internet ổn định khi dev.

---

## So sánh profile Spring

| Profile | PostgreSQL | Redis | Khi nào dùng |
|---------|------------|-------|--------------|
| `local-native` | Local / Neon | **Không** — in-memory | Phase 0, không Docker |
| `local` | Local / Neon | Local Memurai / Upstash | Phase 1+ |
| `staging` / `prod` | Cloud RDS | ElastiCache | Deploy |

---

## Reset database (không Docker)

### Xóa và tạo lại DB

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "DROP DATABASE IF EXISTS dungeon_delvers;"
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -f scripts\windows\init-postgres.sql
```

Restart server → Flyway chạy lại migrations.

### Chỉ xóa data, giữ schema

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U ddo -d dungeon_delvers -c "TRUNCATE accounts, characters CASCADE;"
```

---

## Troubleshooting Windows

| Vấn đề | Giải pháp |
|--------|-----------|
| `psql` không nhận lệnh | Thêm `PostgreSQL\16\bin` vào PATH |
| `Connection refused` port 5432 | Services → **postgresql-x64-16** → Start |
| `password authentication failed` | Kiểm tra user/password trong yml |
| `Flyway validate failed` | Drop DB và tạo lại (xem trên) |
| Port 5432 bị chiếm | `netstat -ano \| findstr 5432` — đổi port PostgreSQL hoặc tắt app conflict |
| Không có Redis, server báo lỗi | Dùng profile **`local-native`**, không phải `local` |
| Firewall chặn Unity → PC | Dùng IP LAN máy dev trong `ServerConfig` |

---

## Checklist (không Docker)

- [ ] Java 21: `java -version`
- [ ] PostgreSQL 16 chạy (Services)
- [ ] DB `dungeon_delvers` + user `ddo` đã tạo
- [ ] `application-local-native.yml` đã copy
- [ ] `verify-setup.ps1` pass
- [ ] Server start profile `local-native`
- [ ] `skill_definitions` count = 8

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-09 | Windows native guide without Docker |
