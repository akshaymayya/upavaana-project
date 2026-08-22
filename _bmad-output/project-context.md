---
project_name: 'Plant Doctor App'
user_name: 'Aksha'
date: '2026-08-01'
sections_completed:
  - technology_stack
  - language_rules
  - framework_rules
  - testing_rules
  - quality_rules
  - workflow_rules
  - anti_patterns
status: complete
rule_count: 42
optimized_for_llm: true
existing_patterns_found: 18
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

| Layer | Stack | Versions / Notes |
|-------|-------|------------------|
| **Backend** | Java, Spring Boot, JPA, Flyway, MySQL | Java **21**, Spring Boot **4.0.0**, Maven (`mvnw.cmd` / `./mvnw`) |
| **Mobile** | React Native, Expo, React Navigation | Expo **~54**, React **19.1.0**, RN **0.81.5**, TypeScript **~5.9.2** |
| **Database** | MySQL 8 | Schema via Flyway migrations only (`backend/src/main/resources/db/migration/`) |
| **AI (actual)** | NVIDIA NIM (vision + text fallback), DeepSeek API (synthesis) | **Not OpenAI** — see `NvidiaClientService`, `application.yml` |
| **CSV seeding** | OpenCSV | **5.12.0** |

**Repo layout:** `backend/` (Spring Boot API), `mobile/` (Expo app), `_bmad-output/` (BMAD artifacts), `docs/` (project knowledge, currently empty).

**Authoritative build brief:** root `AGENTS.md` — phase order and ground rules override ad-hoc plans.

---

## Critical Implementation Rules

### Language-Specific Rules

**Java (backend)**
- Base package: `com.plantdoctor` — subpackages: `controller`, `service`, `entity`, `repository`, `config`, `seed`.
- Use **constructor injection** (not field injection); JPA entities use protected no-arg constructors.
- API JSON for diagnosis uses **snake_case** fields via `DiagnosisResult` record (`plant_name`, `disease_name`, `is_healthy`, etc.) — do not switch to camelCase in responses.
- Schema changes require new Flyway migrations (`V{n}__description.sql`); `spring.jpa.hibernate.ddl-auto` is **`validate`** — never rely on auto-DDL.
- Config via `@ConfigurationProperties` beans (`NvidiaProperties`, `SeedProperties`) bound in `application.yml` with `${ENV_VAR:default}` placeholders.
- External API calls use `RestTemplate` with explicit connect/read timeouts — preserve timeout tiers (fast for NVIDIA vision/text, longer for DeepSeek).

**TypeScript (mobile)**
- `tsconfig.json` has **`strict: true`** — no implicit any without justification.
- Screen components live in `mobile/src/screens/` as default exports; navigation types in `AppNavigator.tsx`.
- `DiagnosisData` interface must stay aligned with backend `DiagnosisResult` snake_case fields.

### Framework-Specific Rules

**Spring Boot**
- Controllers under `@RequestMapping("/api")` — existing endpoints: `GET /api/health`, `POST /api/admin/seed`, `POST /api/diagnose`.
- `POST /api/diagnose` accepts **`multipart/form-data`** field name **`image`** (`MultipartFile`).
- Return `ResponseEntity` with `Map.of("error", ...)` for 400/500; success returns `DiagnosisResult` JSON directly.
- Seed data comes **only from CSV** (`PlantDiseaseSeedService`) — never hardcode plant/disease rows in Java.
- Run server from **`backend/`** directory so relative CSV paths (`./plant-disease-seed-template.csv`) resolve correctly.

**Expo / React Native**
- Read **`mobile/AGENTS.md`** before mobile changes — points to Expo **v57 docs** even though `package.json` pins Expo 54; verify APIs against installed version.
- API base URL is derived dynamically from `Constants.expoConfig?.hostUri` → `http://{metro-host-ip}:8080/api/diagnose` — do not hardcode `localhost` for physical devices.
- Image upload uses React Native `FormData` with `{ uri, name, type }` object — required shape for multipart POST.
- Mobile client timeout is **180 seconds** (`AbortController`) — backend total AI latency must stay under this (DeepSeek uses `maxAttempts = 1` for this reason).
- UI palette: greens on `#F5F8F5` background (`#1B4332`, `#2D6A4F`, `#52B788`) — match existing screens when adding UI.
- Prefer inline StyleSheet over new icon libraries (HomeScreen uses custom View-based icons intentionally).

**AI diagnosis pipeline (do not break)**
1. NVIDIA vision model analyzes image → text description.
2. `findCandidateDiseases()` matches DB records by **plant name only** (including `common_names` split on commas).
3. **Never re-enable symptom-only matching** — it caused cross-plant hallucinations (e.g., "yellowing" pulling unrelated Money Plant records).
4. If no plant match → return **empty candidate list**; AI synthesizes without DB context.
5. DeepSeek (`synthesizeDiagnosisWithDeepSeek`) is primary synthesizer when `DEEPSEEK_API_KEY` is set; falls back to NVIDIA text model on failure or missing key.
6. Synthesis prompts enforce strict JSON output — use `extractJson()` to strip markdown fences; `@JsonIgnoreProperties(ignoreUnknown = true)` on `DiagnosisResult`.

### Testing Rules

- Backend tests: JUnit 5 + `@ExtendWith(MockitoExtension.class)`; mock `NvidiaClientService`, repositories.
- Use `MockMultipartFile` for upload tests; verify `queryRepository.save()` is called after successful diagnosis.
- Run tests: `cd backend && .\mvnw.cmd test` (Windows) or `./mvnw test`.
- When changing `findCandidateDiseases` logic, add/update unit tests covering plant-name match vs. symptom-only non-match cases.
- No mobile test suite yet — manual device/emulator verification for API integration changes.

### Code Quality & Style Rules

- **Do not change tech stack, database, or architecture without asking first** (root `AGENTS.md`).
- **Never delete working code** unless explicitly requested.
- Secrets via environment variables only — `.env` is gitignored; reference `.env.example` for variable names.
- Spring Boot does **not** auto-load `.env` — export vars in shell or IDE run config.
- Required env vars for full diagnosis: `NVIDIA_API_KEY`, `DEEPSEEK_API_KEY` (optional but recommended), `DB_USERNAME`, `DB_PASSWORD`.
- Uploaded images saved to `backend/uploads/` with UUID filenames — directory is runtime-created.
- Java uses tabs for indentation (existing convention); TypeScript uses 2-space indent.
- Small, testable increments; error handling on all external API calls with logging via SLF4J.

### Development Workflow Rules

**Build phases (from `AGENTS.md`) — stop after each phase for user confirmation:**
1. Backend skeleton ✅ (done)
2. Data seeding ✅ (done — CSV + `/api/admin/seed`)
3. `/api/diagnose` ✅ (implemented — extend, don't rewrite from scratch)
4. Frontend core flow (in progress — Home → Loading → Results/Error navigation exists)
5. Follow-up chat (not started)
6. Polish (not started)

**Backend workflow**
- Migrations: add `V{n}__*.sql`, never edit applied migrations.
- Seed: fill `plant-disease-seed-template.csv`, then `POST /api/admin/seed`; confirm with `GET /api/health`.
- CSV header must be exact: `plant_name,disease_name,description,symptoms,causes,solution`.

**Mobile workflow**
- `npm start` / `expo start` from `mobile/`; ensure backend reachable at device IP on port 8080.

**Git**
- Only commit when user explicitly asks.
- Do not commit `.env`, credentials, or `backend/uploads/` images.

### Critical Don't-Miss Rules

**Anti-patterns — never do these:**
- ❌ Match DB diseases by symptom keywords alone (`plantMatch || symptomMatch`) — **plant name match required**.
- ❌ Pass entire disease table to AI when no plant matches — return empty list instead.
- ❌ Increase DeepSeek retry count without checking mobile 180s timeout budget.
- ❌ Use NVIDIA-specific payload fields (`chat_template_kwargs`) against official DeepSeek endpoint.
- ❌ Hardcode seed data in Java instead of CSV.
- ❌ Assume `AGENTS.md` OpenAI stack — production code uses NVIDIA + DeepSeek.

**Edge cases agents must handle:**
- Unknown/unidentified plant → `disease_name: "Unidentified Issue"`, empty DB candidates, AI uses general pathology knowledge.
- Healthy plant → `disease_name: "Healthy"`, `is_healthy: true`.
- Missing API keys → `IllegalStateException` for vision; DeepSeek missing → warn + fallback to NVIDIA text.
- Empty/missing image → `400` with `"Image file is required"`.

**Security**
- Never log API keys or commit `.env`.
- No auth layer yet — do not expose admin/seed endpoints publicly without discussing first.

**Performance**
- Vision/text NVIDIA: 25s timeout, 2 retries. DeepSeek: 90s read timeout, **1 attempt** then fallback.
- Keep diagnosis end-to-end under **3 minutes** for mobile client compatibility.

---

## Usage Guidelines

**For AI Agents:**
- Read this file before implementing any code.
- Follow ALL rules exactly as documented.
- When in doubt, prefer the more restrictive option.
- Cross-check root `AGENTS.md` for phase gating — do not skip ahead without user confirmation.
- Update this file if new patterns emerge (e.g., follow-up chat, OpenAI migration).

**For Humans:**
- Keep this file lean and focused on agent needs.
- Update when technology stack changes (note: `AGENTS.md` AI section is stale vs. actual NVIDIA/DeepSeek implementation).
- Review quarterly for outdated rules.
- Remove rules that become obvious over time.

Last Updated: 2026-08-01
