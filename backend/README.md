# Plant Doctor API (Spring Boot)

Backend for the Plant Doctor app.

## What lives here

| Piece | Purpose |
|--------|---------|
| `PlantDoctorApiApplication.java` | Starts the Spring Boot server |
| `application.yml` | DB settings, Flyway, optional startup seed flag |
| `db/migration/V1__init_schema.sql` | Core tables |
| `db/migration/V2__add_description_column.sql` | Adds `diseases.description` |
| `plant-disease-seed-template.csv` | **You fill this** — seed data (header only until you add rows) |
| `PlantDiseaseSeedService` | Reads CSV → `plants` + `diseases` (no hardcoded data) |
| `POST /api/admin/seed` | Run seed manually (recommended) |
| `GET /api/health` | DB check + row counts (use after seeding) |

## Prerequisites (one-time setup)

1. **JDK 21** — [Adoptium Temurin 21](https://adoptium.net/) or Oracle JDK 21. After install, `java -version` should work in a new terminal.
2. **MySQL 8** — Server running locally (MySQL Workbench, Docker, or installer). Create a user/password you will use below.
3. **No global Maven required** — use `mvnw.cmd` (Windows) or `./mvnw` (Mac/Linux) in this folder.

## Configure database

Set environment variables (PowerShell example):

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
```

See `.env.example` for all variable names. Spring Boot does not load a `.env` file automatically; export vars in your shell or use your IDE run configuration.

The JDBC URL includes `createDatabaseIfNotExist=true`, so MySQL will create the `plant_doctor` database on first connect if it is missing.

## Run the server

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Flyway runs pending migrations on startup (`V1`, then `V2`, …).

---

## Phase 2 — CSV seeding

### CSV format

Edit `plant-disease-seed-template.csv` in this folder (`backend/`). **Exact header** (already present):

```text
plant_name,disease_name,description,symptoms,causes,solution
```

Column mapping from your research docs:

| CSV column | Research section |
|------------|------------------|
| `description` | “What are they and how they will be?” |
| `symptoms` | “How they will affect the plant?” |
| `causes` | “Where will they live and survive?” |
| `solution` | “How to control them?” |

Use quotes in the CSV when a field contains commas or line breaks (standard CSV). Re-running seed **skips** rows that already exist for the same plant + disease name (case-insensitive).

### Option A — Manual seed (recommended)

With the server running **from the `backend` directory** (so the default CSV path resolves):

```powershell
curl.exe -X POST http://localhost:8080/api/admin/seed
```

Custom file path:

```powershell
curl.exe -X POST "http://localhost:8080/api/admin/seed?csvPath=./my-filled-seed.csv"
```

Example response:

```json
{
  "csvPath": "C:\\...\\backend\\plant-disease-seed-template.csv",
  "rowsRead": 42,
  "plantsCreated": 5,
  "diseasesInserted": 42,
  "diseasesSkipped": 0,
  "errors": []
}
```

### Option B — Seed on startup

Set before `spring-boot:run`:

```powershell
$env:SEED_ON_STARTUP = "true"
$env:SEED_CSV_PATH = "./plant-disease-seed-template.csv"
.\mvnw.cmd spring-boot:run
```

Or in `application.yml` via `app.seed.enabled` (default is `false`).

### Confirm seeding worked

```powershell
curl.exe http://localhost:8080/api/health
```

`tables.plants` and `tables.diseases` should match your CSV (one plant row per distinct `plant_name`, one disease row per row in the file).

### Optional: inspect in MySQL

```sql
USE plant_doctor;
SELECT p.name, d.disease_name, LEFT(d.description, 80) AS description_preview
FROM diseases d
JOIN plants p ON p.id = d.plant_id
LIMIT 10;
```

---

## Phase 1 — Health check

```powershell
curl.exe http://localhost:8080/api/health
```

**Expected (empty DB):**

```json
{
  "status": "ok",
  "database": "connected",
  "tables": {
    "plants": 0,
    "diseases": 0,
    "queries": 0
  }
}
```

**If MySQL is down or credentials are wrong:** HTTP 503 with `"database": "disconnected"`.
