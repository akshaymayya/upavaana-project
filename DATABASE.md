# Plant Doctor — MySQL database

**Purpose:** Schema, relationships, and SQL to create or inspect the database.  
**Source of truth in code:** Flyway migrations in `backend/src/main/resources/db/migration/`.  
**Last updated:** 2026-09-23

---

## Overview

The app uses one MySQL database (default name **`plant_doctor`**) with three application tables:

| Table | Purpose |
|--------|---------|
| **`plants`** | Plant species in the knowledge base (canonical name + optional aliases). |
| **`diseases`** | One row per plant + disease; holds symptoms, causes, and recommended solution text used during diagnosis retrieval. |
| **`queries`** | Audit log of each diagnosis: saved image path and full result JSON. |

Spring Boot uses **`ddl-auto: validate`** — it does **not** auto-create tables. **Flyway** runs migrations on startup and creates/updates schema.

Default connection (from `backend/src/main/resources/application.yml`):

- URL: `jdbc:mysql://localhost:3306/plant_doctor?createDatabaseIfNotExist=true&...`
- User: `DB_USERNAME` (default `root`)
- Password: `DB_PASSWORD` (default empty)

---

## Entity relationship (logical)

```mermaid
erDiagram
  plants ||--o{ diseases : "plant_id"
  queries }o--|| : "no FK to plants/diseases"

  plants {
    int id PK
    varchar name
    varchar common_names
  }

  diseases {
    int id PK
    int plant_id FK
    varchar disease_name
    text description
    text symptoms
    text causes
    text solution
  }

  queries {
    int id PK
    varchar image_url
    text result_json
    timestamp created_at
  }
```

- **`diseases.plant_id`** → **`plants.id`** (many diseases per plant).
- **`queries`** is independent: it stores the API outcome, not normalized diagnosis fields.

---

## Create database and schema (manual SQL)

Use this if you want to set up MySQL by hand. In normal development, starting the Spring Boot app with Flyway enabled applies the same structure automatically.

### 1. Create database and user (optional)

```sql
CREATE DATABASE IF NOT EXISTS plant_doctor
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Optional: dedicated user (adjust password)
CREATE USER IF NOT EXISTS 'plant_doctor'@'localhost' IDENTIFIED BY 'your_password_here';
GRANT ALL PRIVILEGES ON plant_doctor.* TO 'plant_doctor'@'localhost';
FLUSH PRIVILEGES;

USE plant_doctor;
```

### 2. Application tables (equivalent to V1 + V2 migrations)

```sql
USE plant_doctor;

CREATE TABLE plants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    common_names VARCHAR(255)
);

CREATE TABLE diseases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plant_id INT,
    disease_name VARCHAR(255) NOT NULL,
    description TEXT,
    symptoms TEXT,
    causes TEXT,
    solution TEXT,
    FOREIGN KEY (plant_id) REFERENCES plants(id)
);

CREATE TABLE queries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(500),
    result_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Note: Migration **V1** created `diseases` without `description`; **V2** added it with `ALTER TABLE diseases ADD COLUMN description TEXT AFTER disease_name;`. The `CREATE TABLE` above is the **final** shape after both migrations.

### 3. Flyway history table (created automatically)

When the backend starts, Flyway also creates **`flyway_schema_history`** and records applied migrations. You do not need to create it manually.

---

## Column reference

### `plants`

| Column | Type | Notes |
|--------|------|--------|
| `id` | INT, PK, auto-increment | Internal ID |
| `name` | VARCHAR(255), NOT NULL | Primary display name (e.g. `Money Plant`) |
| `common_names` | VARCHAR(255), nullable | Comma-separated aliases matched during retrieval (e.g. `pothos, devil's ivy`) |

### `diseases`

| Column | Type | Notes |
|--------|------|--------|
| `id` | INT, PK, auto-increment | Internal ID |
| `plant_id` | INT, FK → `plants.id` | Which plant this disease belongs to |
| `disease_name` | VARCHAR(255), NOT NULL | e.g. `Root Rot`, `Mealybugs` |
| `description` | TEXT | Short summary of the condition |
| `symptoms` | TEXT | Comma-separated phrases; used for keyword scoring vs vision text |
| `causes` | TEXT | Cause narrative (passed to AI context when candidate matches) |
| `solution` | TEXT | Recommended treatment (ground truth for synthesis) |

### `queries`

| Column | Type | Notes |
|--------|------|--------|
| `id` | INT, PK, auto-increment | Request ID |
| `image_url` | VARCHAR(500) | Relative path under backend `uploads/` (e.g. `uploads/<uuid>.jpg`) |
| `result_json` | TEXT | Full `DiagnosisResult` JSON returned to the client |
| `created_at` | TIMESTAMP | Default `CURRENT_TIMESTAMP` on insert |

---

## How data gets loaded (seeding)

Knowledge base rows are **not** hand-maintained in SQL for day-to-day work. They come from a CSV:

**File:** `backend/plant-disease-seed-template.csv`

**Columns:** `plant_name`, `disease_name`, `description`, `symptoms`, `causes`, `solution`

**Ways to seed:**

1. **HTTP (typical dev):** `POST http://localhost:8080/api/admin/seed`  
   Body is empty; path comes from `app.seed.csv-path` (default `./plant-disease-seed-template.csv`).

2. **On startup:** set `SEED_ON_STARTUP=true` (or `app.seed.enabled=true` in config).

Seeding **upserts** plants by name and **skips** duplicate `(plant_id, disease_name)` pairs (case-insensitive).

### Example manual INSERT (single row)

```sql
INSERT INTO plants (name, common_names)
VALUES ('Money Plant', 'pothos, devil''s ivy');

INSERT INTO diseases (plant_id, disease_name, description, symptoms, causes, solution)
VALUES (
  LAST_INSERT_ID(),
  'Root Rot',
  'A fungal condition affecting the root system, common in overwatered houseplants.',
  'Yellowing leaves, mushy blackened roots, wilting despite moist soil.',
  'Caused by overwatering and poor drainage.',
  'Reduce watering frequency, repot in well-draining soil, trim away affected roots.'
);
```

---

## How the API uses the database

1. **Diagnosis** loads all diseases with plants: JPA `findAllWithPlant()` (no complex SQL in repo).
2. **Retrieval** is done in Java on the vision text:
   - **Plant name:** if vision text contains plant `name` or any `common_names` entry → up to **5** candidates.
   - **Symptom patterns:** score symptoms/disease name keywords → min score **4**, up to **3** more candidates.
3. After synthesis, a row is inserted into **`queries`**.

There are **no** stored procedures, views, or full-text indexes in the current schema.

---

## Useful inspection queries

```sql
-- Row counts (same idea as GET /api/health)
SELECT
  (SELECT COUNT(*) FROM plants)   AS plants,
  (SELECT COUNT(*) FROM diseases) AS diseases,
  (SELECT COUNT(*) FROM queries)  AS queries;

-- All knowledge base rows with plant name
SELECT
  p.name AS plant_name,
  d.disease_name,
  LEFT(d.symptoms, 80) AS symptoms_preview
FROM diseases d
JOIN plants p ON p.id = d.plant_id
ORDER BY p.name, d.disease_name;

-- Recent diagnoses
SELECT
  id,
  image_url,
  created_at,
  LEFT(result_json, 200) AS result_preview
FROM queries
ORDER BY created_at DESC
LIMIT 10;

-- Diseases for one plant
SELECT d.*
FROM diseases d
JOIN plants p ON p.id = d.plant_id
WHERE p.name = 'Snake Plant';
```

---

## Teardown / reset (dev only)

```sql
USE plant_doctor;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE queries;
TRUNCATE TABLE diseases;
TRUNCATE TABLE plants;
SET FOREIGN_KEY_CHECKS = 1;
```

Then run seed again (`POST /api/admin/seed` or restart with seed enabled).

To drop everything including Flyway history:

```sql
DROP DATABASE IF EXISTS plant_doctor;
```

Restart the backend to recreate the database (if `createDatabaseIfNotExist=true`) and re-run Flyway migrations.

---

## Related files

| File | Role |
|------|------|
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | Initial tables |
| `backend/src/main/resources/db/migration/V2__add_description_column.sql` | Adds `diseases.description` |
| `backend/src/main/java/com/plantdoctor/entity/*.java` | JPA entity mapping |
| `backend/plant-disease-seed-template.csv` | Sample seed data |
| [SYSTEM-OVERVIEW.md](./SYSTEM-OVERVIEW.md) | End-to-end app flow |
| [REPO-STRUCTURE.md](./REPO-STRUCTURE.md) | Full repo and env layout |
| [docs/README.md](./docs/README.md) | Documentation index |
