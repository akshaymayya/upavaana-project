# PRD Addendum: UPAVANA Plant Doctor MVP — Technical Reference

_Supplements `prd.md`. For architecture and dev agents. Gate 2 aligned with brownfield codebase._

---

## Brownfield Code References

| Concern | Location |
|---------|----------|
| Pipeline orchestration | `backend/.../DiagnosisService.java` |
| Vision + synthesis clients | `backend/.../NvidiaClientService.java` |
| RAG candidates | `backend/.../DiseaseCandidate.java`, `findCandidateDiseases()` |
| API boundary | `backend/.../DiagnoseController.java` |
| Diagnosis JSON shape | `backend/.../DiagnosisResult.java` |
| Mobile upload + timeout | `mobile/src/screens/LoadingScreen.tsx` |
| Results + disclaimer | `mobile/src/screens/ResultsScreen.tsx` |
| UPAVANA tokens | `mobile/src/theme/colors.ts` |
| Agent rules | `_bmad-output/project-context.md` |
| Architecture invariants | `ARCHITECTURE-SPINE.md` (AD-3/AD-4 **pending update** for D-4) |

---

## API Contract

### `POST /api/diagnose`

**Request:** `multipart/form-data`, field **`image`** (required).

**Success `200`:**

```json
{
  "plant_name": "Money Plant",
  "disease_name": "Root Rot",
  "symptoms_matched": "Yellowing leaves, soft stems",
  "solution": "Let the soil dry, then water less often.",
  "confidence_note": "High — plant and symptoms match knowledge base record",
  "is_healthy": false,
  "treatment_type": "single_action",
  "treatment_steps": null
}
```

`treatment_type` is `"single_action"` or `"care_plan"`. When `care_plan`, `treatment_steps` is an array of `{ "day": string, "action": string }` (3–5 items). When `single_action`, omit `treatment_steps` or send `null` / `[]`.

**Care-plan example:**

```json
{
  "treatment_type": "care_plan",
  "solution": "Treat spider mites over several days.",
  "treatment_steps": [
    { "day": "Today", "action": "Isolate the plant and wipe leaves with a damp cloth." },
    { "day": "Day 3", "action": "Spray neem oil on top and underside of leaves." },
    { "day": "Day 7", "action": "Repeat spray; check for new webbing." }
  ]
}
```

**Error `400` / `500`:** `{ "error": "Human-readable message" }`

### Operations

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/health` | DB + `plants` / `diseases` / `queries` counts |
| POST | `/api/admin/seed` | CSV seed (`?csvPath=` optional) |

---

## Diagnosis Pipeline (Active MVP — per D-7 / D-8)

```
Upload image
  → Save uploads/{uuid}.ext
  → Vision (NVIDIA NIM only) — D-1
  → findCandidateDiseases() — D-4:
       Phase A: PLANT_NAME matches → High confidence tier
       Phase B: SYMPTOM_PATTERN matches → Medium confidence tier
       Neither → Low tier (Unidentified Issue)
  → Synthesis — D-7 / D-8 (active):
       Primary: OpenAI API (OPENAI_API_KEY)
                gpt-4o ONLY + response_format json_schema (D-8; OQ-7 resolved)
       Fallback: NVIDIA NIM text model (must E2E-test before demo — DAC-6)
  → Persist Query
  → Return Diagnosis JSON

Inactive (code retained, not called in active path):
  → DeepSeek API (synthesizeDiagnosisWithDeepSeek) — superseded D-2/D-5
```

### RAG rules (PRD FR-6 — must not regress)

1. **Dual retrieval** — plant-name and symptom-pattern (not plant-only).
2. **Bounded candidates** — never pass entire KB to synthesis.
3. **Match labels** — synthesis prompts distinguish `PLANT_NAME_MATCH` vs `SYMPTOM_PATTERN_MATCH`.
4. **Confidence tiers** — High / Medium / Low via `confidence_note` (FR-7).
5. **Soft unidentified** — empty or non-fitting candidates → `Unidentified Issue` + locked Low-tier copy (FR-12).
6. **JSON extraction** — strip markdown fences before deserialize.

---

## AI Provider Configuration (MVP — Active stack, D-7 / D-8)

| Role | Default model | Provider | Active? | Env vars |
|------|---------------|----------|---------|----------|
| Vision | `meta/llama-3.2-11b-vision-instruct` | NVIDIA NIM | **Yes** | `NVIDIA_API_KEY`, `NVIDIA_BASE_URL` |
| Synthesis primary | **`gpt-4o`** (locked — D-8) | OpenAI | **Yes** | `OPENAI_API_KEY`, `OPENAI_BASE_URL` (optional) |
| Synthesis fallback | `meta/llama-3.1-8b-instruct` | NVIDIA NIM | **Yes** (fallback; E2E required) | `NVIDIA_API_KEY` |
| Synthesis (legacy) | `deepseek-v4-pro` | DeepSeek API | **No** — code retained | `DEEPSEEK_API_KEY`, `DEEPSEEK_BASE_URL` |

**Supersedes:** D-2, D-3, D-5, OQ-2 (DeepSeek primary); OQ-7 → D-8 (`gpt-4o` only for MVP).

**Implementation note (brownfield):** `DiagnosisService` may still call DeepSeek until a dev story wires OpenAI — this addendum documents **target** active stack per stakeholder decision.

### Timeout budget (mobile 180s total)

| Step | Timeout | Retries |
|------|---------|---------|
| NVIDIA vision | 25s | 2 |
| OpenAI `gpt-4o` synthesis | ≤ 60s read (target; tune in impl.) | 1–2 |
| NVIDIA text fallback | 25s | 2 |

---

## Symptom-Pattern Scoring (Implementation Reference)

Current constants in `DiagnosisService` (tunable without API change):

- `MIN_SYMPTOM_SCORE = 4`
- `MAX_SYMPTOM_CANDIDATES = 3`
- Scoring: disease name in vision text, multi-word disease name tokens, symptom phrase keyword overlap, capped keyword hits.

Tests: `DiagnosisServiceTest` — plant match, symptom pattern when misidentified, plant match precedence.

---

## Database Schema (Summary)

- **plants:** `id`, `name`, `common_names`
- **diseases:** `id`, `plant_id`, `disease_name`, `description`, `symptoms`, `causes`, `solution`
- **queries:** `id`, `image_url`, `result_json`, `created_at`

Schema changes: new Flyway `V{n}__*.sql` only.

---

## Mobile ↔ API Checklist

| Item | PRD | Notes |
|------|-----|-------|
| `is_healthy` on client | FR-11 | Must be on `DiagnosisData` |
| Disclaimer exact copy | FR-18 | See PRD §4.7 |
| Unidentified styling | FR-12 | Warning tone, helpful copy |
| UPAVANA colors | FR-10 | `colors.ts` / DESIGN.md |

---

## CSV Seed Format

```text
plant_name,disease_name,description,symptoms,causes,solution
```

From `backend/`:

```powershell
curl.exe -X POST http://localhost:8080/api/admin/seed
```

---

## Demo Preparation Checklist

- [ ] **`[CRITICAL]` OQ-1 answered** — 3–5 must-pass plant+disease pairs named
- [ ] MySQL running; Flyway up to date
- [ ] `NVIDIA_API_KEY` set
- [ ] `OPENAI_API_KEY` set (active synthesis — **`gpt-4o`** per D-8)
- [ ] `DEEPSEEK_API_KEY` **not required** for MVP (optional; inactive path)
- [ ] CSV seeded for must-pass pairs; `/api/health` shows expected counts
- [ ] Backend on `:8080` from `backend/`
- [ ] Mobile uses Metro host IP (not localhost on device)
- [ ] Test photos for each must-pass pair (primary OpenAI path)
- [ ] Healthy images → Healthy, never disease
- [ ] Low-confidence photo → Unidentified Issue + locked FR-12 copy
- [ ] Results: no ⚠️ / “Issue Detected”; `single_action` vs `care_plan` render distinctly (FR-19)
- [ ] **NVIDIA fallback path forced/tested E2E** — same DAC as primary (DAC-6)
- [ ] Stakeholder dry-run without developer assistance
- [ ] Disclaimer visible on Results

---

## Architecture & UX follow-up (2026-08-29 — P6 + FR-19)

Product source of truth is `prd.md` (P6, FR-10, FR-12, FR-19). **AD-2 / AD-7 adopted 2026-08-29.** Remaining stale until UX update:

| Artifact | What to change |
|----------|----------------|
| **AD-2 / AD-7** | Done — see `ARCHITECTURE-SPINE.md`. Code still lags (Java record + schema + prompts). |
| **UX `EXPERIENCE.md` / `DESIGN.md`** | Done 2026-08-29 — P6 result header; NEXT STEP vs CARE PLAN layouts; Unidentified locked copy. Live Results screen still lags. |

Do **not** treat current Results warning styling as still required.

---

## Scalability Hooks (v2-Ready, v1 Simple)

| Area | MVP | Future without rebuild |
|------|-----|------------------------|
| KB growth | CSV seed | Same tables; optional CMS later; human review of Low/Med `queries` |
| History | `queries` persisted | Add API + mobile screen |
| Chat | Out of scope | Mini-tier model on same backend (post-demo cost opt) |
| Auth | None | New AD + endpoints |
| Model swap | Env vars + service classes | Pipeline order fixed (AD-5) |
| Auto-ingest AI into KB | Forbidden | Human-verified research only |
