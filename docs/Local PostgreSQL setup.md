# Local PostgreSQL setup (no Docker)

Blast Radius **requires PostgreSQL** from Phase 1 onward when you run `spring-boot:run`. Tests use in-memory H2 and do **not** need Postgres.

Your machine already has **PostgreSQL 17** installed and the service is **Running**.

## 1. Create the database

### Option A — pgAdmin (GUI)

1. Open **pgAdmin 4** from Start menu
2. Connect to **PostgreSQL 17** (use the password you set at install time)
3. Right-click **Databases** → **Create** → **Database**
4. Name: `blastradius` → Save

### Option B — psql (terminal)

```powershell
psql -U postgres -h localhost
```

Then in psql:

```sql
CREATE DATABASE blastradius;
\q
```

(Use your actual postgres superuser password when prompted.)

## 2. Point the app at your database

Edit `src/main/resources/application.properties` **or** set environment variables (safer — no password in git):

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "YOUR_POSTGRES_PASSWORD"
```

Default in repo (change if yours differs):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/blastradius
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## 3. Run and verify

```powershell
cd C:\Users\kaust\projects\blastradius
.\mvnw.cmd spring-boot:run
```

Success looks like:

- Console: `Started BlastradiusApplication`
- Flyway: `Successfully applied 1 migration`
- Browser: http://localhost:8080/health → `Blast Radius backend is alive`

## 4. IntelliJ Database tool

- Host: `localhost`, Port: `5432`, Database: `blastradius`, User: `postgres`
- You should see 5 tables: `project`, `dependency`, `usage_site`, `advisory`, `finding`

---

## Docker later (Phase 7 deploy)

**Yes — skipping Docker during development is fine.**

| Now (dev) | Later (ship) |
|-----------|--------------|
| Local PostgreSQL 17 | `docker-compose.yml` with Postgres + app image |
| `application.properties` → localhost | Railway / Vercel env vars |

Containerizing at the end is a normal pattern: develop against local Postgres, package everything in Docker/Railway when you deploy **v0.1.0**.
