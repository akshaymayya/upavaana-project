# UPAVANA Plant Doctor — Developer Handover

**Last updated:** 2026-09-24  
**Git `main` tip (verified):** `0627a8d` — pushed to `origin/main`  
**Audience:** New developer with no project history. BMAD terms are explained on first use.

---

## 1. What this project is

UPAVANA Plant Doctor is a mobile app (React Native / Expo) plus a Spring Boot API. A user takes or picks a photo of a plant; the backend runs **vision AI** on the image, looks up matching diseases from a **MySQL knowledge base** (keyword-style retrieval, not vector embeddings), then runs **text AI** to produce a structured diagnosis: plant name, issue name, matched symptoms, recommended action, confidence note, and healthy vs not healthy.

The product goal is a trustworthy demo for houseplant owners, grounded in curated CSV research—not generic chatbot guesses.

---

## 2. Current working state (verified against code + tests)

### Backend (`backend/`)

| Area | Status | Evidence |
|------|--------|----------|
| **Server & DB** | Implemented | Spring Boot 4, Java 21, Flyway migrations, `GET /api/health` returns DB connectivity + row counts |
| **Seed KB** | Implemented | `POST /api/admin/seed` reads CSV; `plant-disease-seed-template.csv` ships with **5 sample plant/disease rows** (not an empty template) |
| **Diagnose** | Implemented | `POST /api/diagnose` multipart field `image` → `DiagnosisService.diagnosePlant()` |
| **Unit / integration tests** | **Passing on 2026-09-24** | `cd backend && .\mvnw.cmd test` (exit 0). Includes `DiagnosisHealthConsistencyTest`, `NvidiaClientServiceVisionPromptTest`, `NvidiaClientServiceSynthesisPromptTest`, plus `DiagnosisServiceTest`, `DiagnosisPropertiesTest`, `EnvFileLoaderTest`, `PlantDoctorApiApplicationTests` |

**Important:** Tests **mock** `NvidiaClientService`. They prove routing, retrieval, and persistence—not live calls to NVIDIA, Groq, or OpenAI.

### Mobile (`mobile/`)

| Area | Status | Evidence |
|------|--------|----------|
| **Core flow** | Implemented | Home (camera/gallery) → Loading (upload) → Results or Error |
| **Session history** | Implemented | In-memory list on Home (lost on app restart); commit `04471ee` |
| **API URL** | Dynamic | `LoadingScreen` builds `http://{metro-host}:8080/api/diagnose` from Expo host (not hardcoded localhost for devices) |
| **Client timeout** | 180 seconds | `AbortController` in `LoadingScreen.tsx` |
| **Formatted solution text** | Implemented | `FormattedGuidanceText` + `formatGuidance.ts` (**bold**, line breaks) on Results |
| **Automated tests** | **None** | No mobile Jest/Detox suite; `formatGuidance.selftest.ts` + `npx tsc --noEmit` |

### Database / seed data

- Schema: Flyway `V1__init_schema.sql`, `V2__add_description_column.sql`.
- **Whether your local DB is seeded is environment-specific.** The repo includes sample CSV rows; you must run seed (or `SEED_ON_STARTUP=true`) and confirm with `/api/health` (`tables.plants`, `tables.diseases`).

### AI providers — **live code path only** (read `DiagnosisService` + `NvidiaClientService`, not PRD alone)

Documentation in the PRD and parts of `ARCHITECTURE-SPINE.md` still describe **OpenAI `gpt-4o` as the primary synthesis** path. **That is the stakeholder target (D-7/D-8), not necessarily what runs on your machine.**

**What the Java code does today (`main` @ `0627a8d`):**

| Step | Provider | How it is selected |
|------|----------|-------------------|
| **1. Vision (always)** | **NVIDIA NIM** | `NvidiaClientService.analyzeImage()` — model from `nvidia.vision-model` (default `meta/llama-3.2-11b-vision-instruct`). Requires `NVIDIA_API_KEY`. **No Gemini, no OpenAI vision, no env switch for vision.** |
| **2. KB retrieval** | Local MySQL | `DiagnosisService.findCandidateDiseases()` — plant-name match (cap 5) **plus** symptom-pattern scoring (min score 4, cap 3) |
| **3. Synthesis** | **Env-selected** | `ACTIVE_SYNTHESIS_PROVIDER` via `DiagnosisProperties.resolvedProvider()` (also loaded from `.env` through `EnvFileLoader` at startup): |
| | `groq` | `synthesizeDiagnosisWithGroq` — needs `GROQ_API_KEY` |
| | `openai` | `synthesizeDiagnosisWithOpenAi` — **hardcoded model `gpt-4o`**, needs `OPENAI_API_KEY` |
| | `deepseek` (default if unset/unknown) | `synthesizeDiagnosisWithDeepSeek` → **NVIDIA-hosted DeepSeek** on `integrate.api.nvidia.com` using **`NVIDIA_API_KEY`** and `DEEPSEEK_NIM_MODEL` — **not** `api.deepseek.com` or OpenRouter |
| **4. Synthesis fallback** | **NVIDIA NIM text** | `synthesizeDiagnosis()` — model `nvidia.text-model` (default `openai/gpt-oss-20b`) when primary fails or key missing |
| **5. Post-synthesis** | **`DiagnosisHealthConsistency`** | `enforce()` adjusts healthy vs not when result text conflicts with visible damage cues (merged `f5f6aee`) |

**Repo defaults:** `application.yml` and `backend/.env.example` set `ACTIVE_SYNTHESIS_PROVIDER=deepseek`.

**Confidence tiers (High / Medium / Low):** Requested in synthesis **prompts** inside `NvidiaClientService` (priority rules for holes/chew vs wilt after merge). There is **no** full `DiagnosisPipeline` on `main`. Mobile shows `confidence_note` and a fixed disclaimer string on Results.

---

## 3. What is broken, incomplete, or risky

### Product / process (from PRD & architecture — still open)

| Item | Source | Notes |
|------|--------|--------|
| **OQ-1 / D-6** | PRD §9, Risk Register | **Critical:** Stakeholder has **not** named 3–5 must-pass plant+disease demo pairs. Blocks demo acceptance criteria (DAC-1) and prioritized seeding. |
| **OQ-5** | PRD | Post-MVP priority (chat vs KB vs polish) — open |
| **OQ-6** | PRD | Who owns CSV updates during sprint — open |
| **OQ-8** | PRD | Whether research is explicitly India/local — open (copy only) |
| **DAC-6 / NVIDIA fallback E2E** | PRD §10, AD-6 | **Required before demo** in docs — **not verified** in this handover (no automated E2E test in repo) |
| **Gate 3 formal sign-off** | Architecture memlog | Verbal approval 2026-08-22 noted; OQ-1 still open |

### Code / ops gaps (from `deferred-work.md` + code review)

- **No guard** if vision returns null/empty before synthesis (deferred).
- **RAG prompt size** uncapped — many plant-name hits can inflate synthesis prompts (deferred).
- **DeepSeek synthesis path** uses plain JSON instructions; OpenAI path uses `json_schema` — behavior differs.
- **NVIDIA text fallback** uses a different prompt stack than Groq/OpenAI primaries; `isCompleteDiagnosis` checks differ by path.
- **Stacked timeouts** (vision + synthesis + retries) can approach or exceed mobile **180s** under load (deferred risk).
- **`POST /api/admin/seed` has no authentication** — fine for LAN demo only (AD-10).
- **Follow-up chat** (AGENTS.md phase 5) — **not started**.
- **No production deployment / CI** — local demo only (architecture Deferred table).

### Documentation drift (trust code over these for runtime)

| Document | Drift |
|----------|--------|
| `prd.md` / addendum | Still emphasize **OpenAI gpt-4o** as active MVP synthesis |
| `ARCHITECTURE-SPINE.md` AD-6 | Says `DEEPSEEK_API_KEY` required for `deepseek` — **code uses `NVIDIA_API_KEY` + NIM model** |
| `ARCHITECTURE-SPINE.md` AD-11 | Says Spring does not load `.env` — **`EnvFileLoader` loads `.env` into system properties** at startup |
| `ARCHITECTURE-SPINE.md` Deferred | “Wire OpenAI on diagnosePlant” — **obsolete**; routing exists; default is `deepseek` |
| `backend/README.md` | Still says Spring does not load `.env` |
| Root `README.md` | **Next.js marketing site**, not the Plant Doctor app |

### Work **not** on `main` (do not assume it shipped)

| Branch / tag | Contents |
|--------------|----------|
| `backup/pre-rollback-2026-09-02` (`05867e5`, tag on remote) | Large WIP snapshot: Gemini vision routing, `DiagnosisPipeline`, vision evidence normalization, many regression tests, timeout work — **not** merged |

**Merged to `main` (2026-09-23):** `cursor/diagnosis-priority-formatted-solution` → merge `f5f6aee` (commit `ebecd2a`): synthesis prioritization, `DiagnosisHealthConsistency`, vision/synthesis prompt tests, mobile formatted guidance.

If someone’s local `.env` still has `ACTIVE_VISION_PROVIDER=gemini` or similar, **`main` ignores those keys.**

---

## 4. Project history — key decisions (from PRD decision log + git)

Chronological **commits on `main`** (abbreviated):

| Date | Commit | What changed |
|------|--------|--------------|
| 2026-08-22 | `f76d551` | Spring Boot backend skeleton + marketing Next.js site |
| 2026-08-22 | `0c87bfe` | Working diagnosis pipeline, mobile app, BMAD planning docs |
| 2026-08-22 | `45877ec` | **OpenAI gpt-4o** wired as live synthesis in `DiagnosisService` (DeepSeek methods kept) |
| 2026-08-29 | `04471ee` | Mobile in-memory diagnosis history |
| 2026-08-29 | `2b8c5bc` | **Groq** synthesis path; `.env` loading for `ACTIVE_SYNTHESIS_PROVIDER`; faster NIM DeepSeek timeouts |
| 2026-09-23 | `f9b455a`–`c659296` | Hygiene: remove unused official DeepSeek env bindings; doc alignment with NVIDIA/Groq routing |
| 2026-09-23 | `f5f6aee` | Merge diagnosis priority + formatted solution (`ebecd2a`) |
| 2026-09-23 | `1b84383` | Mobile selftest import fix (`tsc --noEmit` clean) |
| 2026-09-23 | `0627a8d` | Onboarding docs: `HANDOVER.md`, `SYSTEM-OVERVIEW.md`, `DATABASE.md`, `REPO-STRUCTURE.md`, `docs/README.md` |

**Decision themes (PRD §9 — include reversals):**

| Decision | Outcome | Why |
|----------|---------|-----|
| **D-1** NVIDIA vision only | Active | Cost/stack; no OpenAI vision |
| **D-4** Dual RAG (plant name + symptom patterns) | Active in code | Reduce missed KB hits when vision misnames plant; replaces earlier plant-only rule |
| **D-2 / D-5 / OQ-2** DeepSeek API as MVP primary | **Superseded** | Slow/quality issues in testing |
| **D-7** OpenAI primary + NVIDIA text fallback | **Target stack** | Better synthesis quality; billing gate |
| **D-8** Lock diagnosis synthesis to **gpt-4o** | **Target** when `ACTIVE_SYNTHESIS_PROVIDER=openai` | Demo quality over cost |
| **Runtime default DeepSeek (2026-08-27, PRD §9.3.3)** | **Config default `deepseek`** | OpenAI billing blocked; uses **NVIDIA NIM DeepSeek**, not DeepSeek API |
| **Groq path (2026-08-29)** | **Optional primary** | Added as another synthesis provider via env |
| **Official DeepSeek / OpenRouter env vars** | **Removed from bindings (2026-09-23)** | Never used for HTTP on `main` |

**Dead ends / abandoned (do not reintroduce without discussion):**

- OpenAI vision, Gemini vision (only on backup branch, not `main`).
- `DEEPSEEK_API_KEY` / OpenRouter URLs for synthesis on current `main`.
- Plant-name-only retrieval (superseded by D-4, still documented in old memlog entries).

---

## 5. How to run locally

### Prerequisites

- **JDK 21**
- **MySQL 8** running locally
- **Node.js** for Expo (`mobile/`)
- API keys for the providers you actually select (see below)

### Environment variables

Copy `backend/.env.example` → `backend/.env` (or repo-root `.env`). `PlantDoctorApiApplication` loads the first file found via `EnvFileLoader` (does not override OS env vars already set).

| Variable | Required? | Used for |
|----------|-----------|----------|
| `DB_USERNAME`, `DB_PASSWORD` | **Yes** (typical local) | MySQL |
| `DB_URL` | Optional | Default JDBC URL in `application.yml` |
| `NVIDIA_API_KEY` | **Yes** for any diagnosis | Vision + NIM DeepSeek (`deepseek`) + NVIDIA text fallback |
| `ACTIVE_SYNTHESIS_PROVIDER` | Optional | `deepseek` (default), `groq`, or `openai` — **restart JVM after change** |
| `GROQ_API_KEY` | If provider=`groq` | Groq synthesis |
| `OPENAI_API_KEY` | If provider=`openai` | OpenAI gpt-4o synthesis |
| `DEEPSEEK_NIM_MODEL` | Optional | Override NIM DeepSeek model id |
| `NVIDIA_VISION_MODEL`, `NVIDIA_TEXT_MODEL`, `GROQ_MODEL` | Optional | Model overrides |

**Not used by `main` code (safe to omit; may exist in old local `.env`):** `DEEPSEEK_API_KEY`, `DEEPSEEK_BASE_URL`, `DEEPSEEK_MODEL`, `ACTIVE_VISION_PROVIDER`, `GEMINI_*`.

### Backend

```powershell
cd backend
# Set DB_* and API keys (or use backend/.env)
.\mvnw.cmd spring-boot:run
```

Run from `backend/` so `./plant-disease-seed-template.csv` resolves.

Seed (recommended once):

```powershell
curl.exe -X POST http://localhost:8080/api/admin/seed
curl.exe http://localhost:8080/api/health
```

### Mobile

```powershell
cd mobile
npm install
npx expo start
```

Phone/emulator and laptop must be on the **same network**; backend on port **8080**.

### Verify tests (backend only)

```powershell
cd backend
.\mvnw.cmd test
```

---

## 6. Where things live

### Application code

```text
new/
  backend/                    # Spring Boot API (Plant Doctor)
    src/main/java/com/plantdoctor/
      controller/             # /api/health, /api/diagnose, /api/admin/seed
      service/                # DiagnosisService, NvidiaClientService, DiagnosisHealthConsistency, DiagnosisResult
      config/                 # DiagnosisProperties, Nvidia/Groq/OpenAi props, EnvFileLoader
      seed/                   # CSV import
      entity/, repository/
    src/main/resources/
      application.yml
      db/migration/
    plant-disease-seed-template.csv
  mobile/                     # Expo app (UPAVANA UI)
    src/screens/              # Home, Loading, Results, Error
    src/components/           # FormattedGuidanceText
    src/utils/                # formatGuidance
    src/context/              # Session diagnosis history
  src/                        # Separate Next.js marketing landing (not the mobile app)
  AGENTS.md                   # Phase order & ground rules
  SYSTEM-OVERVIEW.md          # Shareable stack + flow
  DATABASE.md                 # MySQL schema + SQL
  REPO-STRUCTURE.md           # Monorepo + env map
  docs/README.md              # Documentation index
  _bmad-output/
    project-context.md        # Agent rules (prefer over stale PRD for stack)
```

### BMAD planning artifacts (read for “why,” not always “what runs today”)

| Path | Contents |
|------|----------|
| `_bmad-output/planning-artifacts/prds/prd-plant-doctor-2026-08-01/prd.md` | Full PRD, decision log, risks, FRs |
| `_bmad-output/planning-artifacts/prds/.../addendum.md` | Pipeline summary (partially stale vs code) |
| `_bmad-output/planning-artifacts/architecture/.../ARCHITECTURE-SPINE.md` | AD-1–AD-14, deferred items |
| `_bmad-output/planning-artifacts/ux/.../DESIGN.md` | Visual / UX principles |
| `_bmad-output/implementation-artifacts/` | Short specs for past stories (`spec-wire-openai-synthesis.md`, `spec-synthesis-reliability.md`, etc.) |
| `_bmad-output/implementation-artifacts/deferred-work.md` | Known technical debt list |

**BMAD** = internal planning method (PRD, architecture spine, stories). You do not need BMAD tooling to run the app.

**RAG** here = retrieving disease rows from MySQL using vision text (plant name + symptom keywords), not embedding search.

---

## 7. Immediate next steps / priorities

Suggested starting order for a new developer:

1. **Answer OQ-1 with stakeholder** — name 3–5 must-pass plant+disease pairs; seed CSV; capture test photos (PRD blocker).
2. **Pick synthesis provider for demo** — align env (`ACTIVE_SYNTHESIS_PROVIDER`) with keys you have; document the choice for the team. PRD target remains OpenAI `gpt-4o` when billing allows.
3. **Run manual E2E** — real photo through mobile → backend with live APIs; explicitly test **primary failure → NVIDIA text fallback** (DAC-6). No script exists in repo.
4. **Reconcile docs** — PRD/addendum/spine/backend README still disagree with code on synthesis default and `.env` loading; use root `SYSTEM-OVERVIEW.md`, `REPO-STRUCTURE.md`, and `DATABASE.md` for runtime truth.
5. **Decide fate of `backup/pre-rollback-2026-09-02`** — cherry-pick or discard Gemini / `DiagnosisPipeline` work (separate from merged formatted-solution branch).
6. **AGENTS.md phase 5** — follow-up chat not built.
7. **Hardening from deferred-work** — vision null guard, prompt size caps, auth on `/api/admin/seed` before any public deploy.

---

## 8. What this handover could **not** verify (read before relying on it)

Please confirm manually:

| Topic | Gap |
|-------|-----|
| **Your machine’s active synthesis provider** | Depends on **your** `backend/.env` / OS env, not repo default alone. This doc does not read your secrets. |
| **End-to-end diagnosis with real images** | Not run as part of this update; `mvnw test` (mocked AI) passed on **2026-09-24** after merge `f5f6aee`. |
| **NVIDIA fallback E2E** | PRD requires it before demo; **no proof** it was executed recently. |
| **Diagnosis accuracy** | No claim that healthy/diseased labels are correct on all real photos; `DiagnosisHealthConsistency` on `main` is a light post-check, not full pipeline from backup branch. |
| **Local DB row counts** | Depends on whether seed was run on your MySQL instance. |
| **Groq / OpenAI / NIM availability** | External services; quotas, model deprecations, and latency change over time. |
| **Stakeholder gates** | OQ-1 and formal demo checklist (PRD Appendix B) — status taken from PRD text, not a live stakeholder sign-off in repo. |

When in doubt: trace `DiagnosisService.diagnosePlant()` and `NvidiaClientService`, then compare to `application.yml` and your local `.env`.

---

*Generated for handover per project audit instructions. Update this file when stack, providers, or demo blockers change.*
