# Repository structure — UPAVANA Plant Doctor

**Purpose:** Single map of the monorepo: what each top-level area is, where docs live, how backend/mobile/env fit together.  
**Audience:** Developers and reviewers onboarding to the project.  
**Last updated:** 2026-09-23

---

## Monorepo at a glance

This git repository contains **four related products**, not one app:

| Area | Path | Runtime | Role |
|------|------|---------|------|
| **Plant Doctor API** | `backend/` | Java 21, Spring Boot, port **8080** | Diagnosis, MySQL, AI calls |
| **Plant Doctor mobile** | `mobile/` | Expo ~54 / React Native | Camera → upload → results |
| **Marketing site** | `src/`, `public/`, root `package.json` | Next.js, port **3000** | Landing / brand (not diagnose flow) |
| **BMAD planning & agents** | `_bmad/`, `_bmad-output/`, `.agents/` | N/A (docs + Cursor skills) | PRD, architecture, specs, agent workflows |

**Trust order for runtime behavior:** Java code + `application.yml` → root **`AGENTS.md`** → **`SYSTEM-OVERVIEW.md`** → **`HANDOVER.md`** → `_bmad-output/` planning docs (may drift).

---

## Top-level tree (committed / product code)

```text
new/                          # git root (project_name in BMAD: "new")
├── AGENTS.md                 # Agent rules, stack, build phases
├── SYSTEM-OVERVIEW.md        # Short stack + flow (shareable)
├── DATABASE.md               # MySQL schema + SQL
├── HANDOVER.md               # Deep developer handover (local; may be untracked)
├── REPO-STRUCTURE.md         # This file
├── docs/                     # Doc index → docs/README.md
│
├── backend/                  # Spring Boot API ★
├── mobile/                   # Expo app ★
│
├── src/                      # Next.js App Router (marketing)
├── public/                   # Static assets for Next.js
├── package.json              # Next.js dependencies (root)
│
├── _bmad-output/             # Generated planning + implementation specs
├── _bmad/                    # BMAD installer config (local tooling)
├── .agents/                  # Cursor agent skills (often untracked)
└── .cursor/                  # Cursor project config (often untracked)
```

---

## Backend (`backend/`)

### Layout

```text
backend/
├── pom.xml
├── mvnw / mvnw.cmd
├── .env.example              # Template for secrets (copy → .env)
├── .env                      # Local secrets (gitignored)
├── plant-disease-seed-template.csv   # Primary seed CSV (5 sample rows)
├── plant-disease-sample-data.csv       # Extra sample (optional)
├── uploads/                  # Saved diagnosis images (gitignored)
│
└── src/
    ├── main/
    │   ├── java/com/plantdoctor/
    │   │   ├── PlantDoctorApiApplication.java
    │   │   ├── config/       # Env, properties, .env loader
    │   │   ├── controller/   # REST: diagnose, health, seed
    │   │   ├── entity/       # JPA: Plant, Disease, Query
    │   │   ├── repository/   # Spring Data JPA
    │   │   ├── seed/         # CSV → MySQL
    │   │   └── service/      # DiagnosisService, NvidiaClientService, …
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/ # Flyway V1, V2
    └── test/java/com/plantdoctor/
        ├── service/          # Diagnosis, NVIDIA prompt tests, …
        └── config/           # EnvFileLoader, DiagnosisProperties
```

### Package responsibilities

| Package | Responsibility |
|---------|----------------|
| `config` | `EnvFileLoader`, `DiagnosisProperties`, NVIDIA/Groq/OpenAI/Seed property beans |
| `controller` | `POST /api/diagnose`, `GET /api/health`, `POST /api/admin/seed` |
| `entity` / `repository` | MySQL mapping for plants, diseases, query log |
| `seed` | Parse CSV, insert plants/diseases, startup runner when enabled |
| `service` | Vision → RAG candidates → synthesis → `DiagnosisHealthConsistency` → save |

### API surface

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/diagnose` | Multipart `image` → `DiagnosisResult` JSON |
| `GET` | `/api/health` | DB ping + table row counts |
| `POST` | `/api/admin/seed` | Load CSV into DB (no auth — demo only) |

### Config files

| File | Purpose |
|------|---------|
| `application.yml` | DB URL, Flyway, seed flags, synthesis default, NVIDIA/Groq/OpenAI bindings |
| `application-local.yml` | Optional overrides (gitignored pattern in `backend/.gitignore`) |
| `.env` | API keys and overrides; loaded at startup (see **Environment** below) |

Details: [DATABASE.md](./DATABASE.md), [backend/README.md](./backend/README.md).

---

## Mobile (`mobile/`)

### Layout

```text
mobile/
├── App.tsx                   # Root component
├── app.json                  # Expo config (name, icons, Android/iOS)
├── package.json              # Expo ~54, React Navigation, etc.
├── index.ts                  # Entry
├── AGENTS.md                 # Expo version pointer for agents
│
└── src/
    ├── navigation/AppNavigator.tsx   # Stack: Home → Loading → Results / Error
    ├── screens/
    │   ├── HomeScreen.tsx            # Camera, gallery, session list
    │   ├── LoadingScreen.tsx         # Upload + POST diagnose
    │   ├── ResultsScreen.tsx         # Diagnosis UI + formatted guidance
    │   └── ErrorScreen.tsx
    ├── components/FormattedGuidanceText.tsx
    ├── context/DiagnosisSessionContext.tsx
    ├── types/diagnosis.ts
    ├── theme/colors.ts
    └── utils/formatGuidance.ts       # Bold/newlines for solution text
```

### Mobile configuration (no `.env` today)

- **API URL** is built at runtime in `LoadingScreen.tsx` from Expo’s dev host (`Constants.expoConfig.hostUri` / `debuggerHost`) → `http://{host}:8080/api/diagnose`.
- **No** `EXPO_PUBLIC_*` env file in repo; physical devices must reach the machine running the API on the LAN.
- **Client timeout:** 180s (`AbortController` on upload).

### Tests

- No Jest/detox suite in repo.
- `formatGuidance.selftest.ts` — manual/CLI-style checks for text formatting (`npx tsc --noEmit` includes it).

---

## Marketing site (repo root Next.js)

```text
src/app/          # Next.js App Router pages
src/components/   # Landing sections
public/images/    # Brand assets
```

Not used by the Plant Doctor diagnose path. Root [README.md](./README.md) is still the default Next.js boilerplate.

---

## Documentation map

### Canonical (root — use for onboarding)

| Document | Use when |
|----------|----------|
| [SYSTEM-OVERVIEW.md](./SYSTEM-OVERVIEW.md) | Explaining stack and flow to someone new |
| [DATABASE.md](./DATABASE.md) | MySQL tables, CREATE SQL, seeding |
| [REPO-STRUCTURE.md](./REPO-STRUCTURE.md) | Where everything lives in the repo |
| [AGENTS.md](./AGENTS.md) | AI agent build rules and phases |
| [HANDOVER.md](./HANDOVER.md) | Full audit: tests, drift, risks, git history |

### Per-app

| Document | Location |
|----------|----------|
| Backend runbook | `backend/README.md` |
| Mobile agent hint | `mobile/AGENTS.md` |

### BMAD / planning (`_bmad-output/`)

| Path | Contents |
|------|----------|
| `project-context.md` | AI rules for agents working in this repo |
| `planning-artifacts/prds/.../prd.md` | Product requirements |
| `planning-artifacts/architecture/.../ARCHITECTURE-SPINE.md` | Architecture decisions (verify against code) |
| `planning-artifacts/ux/.../DESIGN.md` | UX spec |
| `planning-artifacts/epics.md` | Epics |
| `implementation-artifacts/spec-*.md` | Done/in-progress feature specs |
| `implementation-artifacts/deferred-work.md` | Known deferred items |

### Legacy / duplicate names at root

| File | Note |
|------|------|
| `project_documentation.md` | Older narrative doc; prefer HANDOVER + SYSTEM-OVERVIEW |
| `bmad_documentation.md` | BMAD-oriented notes; prefer `_bmad-output/` |

### Empty placeholder

- `docs/` — indexed by [docs/README.md](./docs/README.md) (pointers only; BMAD `project_knowledge` points here).

---

## Environment variables

### Where secrets live

| Location | Committed? | Used by |
|----------|------------|---------|
| `backend/.env` | **No** (gitignored) | Spring Boot JVM |
| `backend/.env.example` | **Yes** | Template |
| OS environment | N/A | Wins over `.env` for same key |
| Root `.env*` | Ignored by root `.gitignore` | **Not** loaded by backend unless you copy keys to `backend/.env` |

### How backend loads `.env`

On startup, `PlantDoctorApiApplication` calls `EnvFileLoader.loadFirstExisting`:

1. `{user.dir}/.env`
2. `{user.dir}/backend/.env`

**Run directory matters:** from repo root, only `./.env` or `./backend/.env` apply. From `backend/`, use `backend/.env`.

Keys are applied as **system properties** if not already set in the OS environment. Spring then resolves `${VAR}` in `application.yml`.

### Variable reference

| Variable | Required | Default / notes |
|----------|----------|-----------------|
| **Database** | | |
| `DB_URL` | No | `jdbc:mysql://localhost:3306/plant_doctor?createDatabaseIfNotExist=true&...` |
| `DB_USERNAME` | No | `root` |
| `DB_PASSWORD` | Often yes | empty |
| **Server** | | |
| `SERVER_PORT` | No | `8080` |
| **Seed** | | |
| `SEED_ON_STARTUP` | No | `false` → maps to `app.seed.enabled` |
| `SEED_CSV_PATH` | No | `./plant-disease-seed-template.csv` (relative to **cwd**) |
| **Diagnosis** | | |
| `ACTIVE_SYNTHESIS_PROVIDER` | No | `deepseek` \| `groq` \| `openai` (OS env wins over YAML) |
| **NVIDIA** | | |
| `NVIDIA_API_KEY` | **Yes** for live diagnose | Vision always; DeepSeek-on-NIM; text fallback |
| `NVIDIA_BASE_URL` | No | `https://integrate.api.nvidia.com/v1` |
| `NVIDIA_VISION_MODEL` | No | `meta/llama-3.2-11b-vision-instruct` |
| `NVIDIA_TEXT_MODEL` | No | `openai/gpt-oss-20b` |
| `DEEPSEEK_NIM_MODEL` | No | `deepseek-ai/deepseek-v4-pro-0813` |
| **Groq** (if provider=groq) | | |
| `GROQ_API_KEY` | Yes | |
| `GROQ_BASE_URL` | No | Groq OpenAI-compatible API |
| `GROQ_MODEL` | No | `openai/gpt-oss-120b` |
| **OpenAI** (if provider=openai) | | |
| `OPENAI_API_KEY` | Yes | Model **gpt-4o** hardcoded in Java |
| `OPENAI_BASE_URL` | No | `https://api.openai.com/v1` |

### Minimal `.env` for local demo

```env
NVIDIA_API_KEY=nvapi-...
ACTIVE_SYNTHESIS_PROVIDER=deepseek
DB_PASSWORD=your_mysql_password
```

Optional: `GROQ_API_KEY` / `OPENAI_API_KEY` when switching synthesis provider.

### Mobile env

No env file required. Ensure backend listens on `0.0.0.0:8080` (Spring default) and phone is on same network as the dev machine.

---

## Local dev workflow (typical)

```text
1. MySQL running
2. cd backend && copy .env.example .env && fill keys
3. .\mvnw.cmd spring-boot:run
4. POST /api/admin/seed  (or SEED_ON_STARTUP=true)
5. GET /api/health  → plants/diseases counts > 0
6. cd mobile && npx expo start
7. Scan QR → take photo → diagnosis
```

---

## Git & ignored artifacts

| Pattern | Meaning |
|---------|---------|
| `backend/.env`, `backend/uploads/` | Secrets and user uploads |
| `backend/target/` | Maven build |
| `node_modules/`, `.next/` | Root Next.js |
| `.env*` (root) | Except `!.env.example` at root only |
| `.agents/`, `_bmad/`, `.cursor/` | Often local-only (check your git status) |

---

## Structure review (code-review lens)

Findings from a **repository layout** pass (not a line-by-line diff). Severity for **organization**, not security.

| ID | Category | Finding | Suggestion |
|----|----------|---------|------------|
| S1 | **patch** | `backend/README.md` says Spring does not load `.env`; code **does** via `EnvFileLoader` | Align README with [HANDOVER.md](./HANDOVER.md) / this doc |
| S2 | **patch** | Root `README.md` describes only Next.js | Add 3-line pointer to `backend/`, `mobile/`, `SYSTEM-OVERVIEW.md` |
| S3 | **defer** | `docs/` empty while BMAD sets `project_knowledge: docs` | Use `docs/README.md` index (added) |
| S4 | **defer** | Duplicate docs: `project_documentation.md`, `bmad_documentation.md` | Archive or merge into HANDOVER when convenient |
| S5 | **defer** | Planning docs in `_bmad-output/` drift from `main` (OpenAI-primary narrative) | Label “planning” vs “runtime” in PRD spine |
| S6 | **decision** | Monorepo mixes Next marketing + Expo + Spring | Keep as-is for demo, or split repos later for CI/deploy |

No file moves were performed in this pass — only documentation structure.

---

## Related links

- [SYSTEM-OVERVIEW.md](./SYSTEM-OVERVIEW.md) — product flow  
- [DATABASE.md](./DATABASE.md) — MySQL  
- [docs/README.md](./docs/README.md) — documentation index  
