---
stepsCompleted:
  - step-01-validate-prerequisites
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-plant-doctor-2026-08-01/prd.md
  - _bmad-output/planning-artifacts/prds/prd-plant-doctor-2026-08-01/addendum.md
  - _bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/TECHNICAL-PICTURE.md
  - _bmad-output/planning-artifacts/ux/ux-plant-doctor-2026-08-01/DESIGN.md
  - _bmad-output/planning-artifacts/ux/ux-plant-doctor-2026-08-01/EXPERIENCE.md
excludedDocuments:
  - _bmad-output/project-context.md
proceedOption: B
architectureStatus: Gate 3 verbally approved 2026-08-22; formal written sign-off pending
namedGaps:
  - OQ-1 must-pass plant+disease pairs TBD — stakeholder to confirm
  - OQ-8 regional/climate focus unanswered — copy/positioning only
nonPlantPhoto: Low-tier Unidentified Issue — no new screen
openaiSynthesis: not yet live — diagnosePlant() still calls DeepSeek; wire-OpenAI is a required story in Epic 2
epicListStatus: proposed-pending-approval
uxCaveat: Confidence-tier UI and locked Unidentified Issue copy — PRD wins over Aug 1 UX spines
created: 2026-08-22
updated: 2026-08-22
---

# UPAVANA Plant Doctor MVP - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for UPAVANA Plant Doctor MVP, decomposing the requirements from the PRD, UX Design if it exists, and Architecture requirements into implementable stories.

**This step:** epic list proposed — stories not written until structure is approved and **[C]** is selected.

## Requirements Inventory

### Functional Requirements

FR1: Camera capture — request camera permission before launch; alert if denied; successful capture navigates to Loading with `imageUri`; cancel returns to Home.

FR2: Gallery selection — request media-library permission; alert if denied; selected image navigates to Loading with `imageUri`; images only (no video).

FR3: Image upload to API — `POST /api/diagnose`, `multipart/form-data`, field name `image`; URL from Expo host (`http://{metro-host-ip}:8080/api/diagnose`, not hardcoded `localhost` on device); FormData `{ uri, name, type }`; client aborts at 180 seconds → Error with timeout flag.

FR4: Loading engagement — status message updates at least every 15 seconds; tips rotate at least every 6 seconds; copy sets expectation that analysis may take a minute or more.

FR5: Vision analysis (NVIDIA NIM only, D-1) — not OpenAI vision; missing `NVIDIA_API_KEY` → clear server error; model `meta/llama-3.2-11b-vision-instruct`; up to 2 retries, 25s per attempt; vision prompt covers leaf morphology, growth habit, honest uncertainty, detailed symptoms.

FR6: Dual RAG retrieval + confidence-tier triggers (D-4) — plant-name match → High; symptom-pattern match across plants (bounded top scores) → Medium; neither → Low / Unidentified Issue path; candidates labeled `PLANT_NAME_MATCH` vs `SYMPTOM_PATTERN_MATCH`; full Knowledge Base never dumped into the synthesis prompt; plant-name matches always included when they apply; plant match preferred when both apply.

FR7: Diagnosis synthesis + confidence-tiered JSON (D-7, D-8) — primary OpenAI `gpt-4o` + `json_schema` → `DiagnosisResult`; fallback NVIDIA text `meta/llama-3.1-8b-instruct` (2 retries, 25s) on OpenAI fail/missing key; DeepSeek must not be invoked on live path; High = KB-grounded species-specific treatment OK; Medium = disease/solution allowed but `confidence_note` states plant type not confirmed in KB; Low = `disease_name` Unidentified Issue, `is_healthy` false, labeled general guidance, no species-specific certainty; Healthy = `disease_name` Healthy, `is_healthy` true; every diagnosis includes tier via `confidence_note`; pipeline ≤ 180s; NVIDIA fallback must be E2E tested (DAC-6), not assumed.

FR8: Diagnosis persistence — each successful diagnosis writes a `queries` row (`image_url`, `result_json`); image saved under `uploads/` with UUID filename. Not shown in mobile UI (MVP).

FR9: API error handling — missing/empty image → 400 `{ "error": "Image file is required" }`; processing failure → 500 `{ "error": "<message>" }` (no stack traces); success → 200 snake_case Diagnosis JSON.

FR10: Diagnosis display — Results render plant name, disease/issue, symptoms matched, solution, confidence note; UPAVANA palette; long solution scrolls; confidence tier visible (High vs Medium vs Low distinguishable).

FR11: Healthy plant UX (P4) — healthy when `is_healthy === true` or `disease_name` indicates health; green celebratory styling; never error or empty/null state; `DiagnosisData` includes `is_healthy`.

FR12: Issue and Unidentified Issue UX (P1, P5) — High/Medium non-healthy use warning (amber) styling; Medium never looks like High plant-confirmed KB grounding; Low Unidentified Issue uses issue styling (not Error screen), locked copy intent (could not confidently identify; conservative not speculative; safe general guidance; not from curated research yet); never fabricated disease name or species-specific certainty; never presented as hard failure.

FR13: Diagnose another — CTA resets to Home; no diagnosis history in mobile UI (MVP). Repeat-user back-to-back flow.

FR14: Timeout error — Error screen with `isTimeout: true`; explains server busy / network; Try Again → Home. Unidentified Issue is not this path.

FR15: Network and server errors — friendly copy; show server `error` message when present; no stack traces / jargon; Try Again → Home.

FR16: CSV seeding — `POST /api/admin/seed` loads CSV (`plant_name,disease_name,description,symptoms,causes,solution`); re-seed skips duplicate plant+disease pairs (case-insensitive); `GET /api/health` returns `plants` and `diseases` counts.

FR17: Demo seed coverage — KB seeded before demo; health endpoint non-zero counts; **must-pass pairs = OQ-1 / D-6 — pairs TBD, stakeholder to confirm; do not invent species.** Seeding priority follows stakeholder list once provided.

FR18: Assistive disclaimer (OQ-4 locked) — visible on Results: "This diagnosis is for general guidance only and isn't a substitute for professional horticultural advice."

### NonFunctional Requirements

NFR1: Performance — mobile client max 180s abort; NVIDIA vision/text 25s, 2 retries; OpenAI `gpt-4o` synthesis target ≤ 60s within 180s budget; loading UI non-blocking.

NFR2: Security / secrets — API keys and DB creds via env vars only; `.env` gitignored; no PII collection; admin seed not on public internet (demo LAN only).

NFR3: Reliability / demo — pre-seed KB after OQ-1 answered; pre-test photos; NVIDIA fallback E2E before demo (DAC-6 / SM-9); same DAC as primary path.

NFR4: Platform lock — Expo ~54, TypeScript strict; Java 21, Spring Boot 4.0, MySQL 8, Flyway-only schema; no stack change without approval.

NFR5: Aesthetic / tone — UPAVANA warm/organic (`DESIGN.md`); celebrate healthy; calm Unidentified Issue; Product Principles P1–P5 on every diagnosis output.

NFR6: Confidentiality — only synthesized Diagnosis fields cross the API; no public raw KB export or disease-table dump (AD-14).

NFR7: Cost — NVIDIA vision + NVIDIA text fallback; OpenAI `gpt-4o` for MVP synthesis; DeepSeek no active spend; `gpt-4o-mini` deferred post-demo.

NFR8: Accessibility floor (`EXPERIENCE.md`) — logo `accessibilityLabel="Upavana"`; WCAG AA contrast for body text; touch targets ≥ 44pt; status by text + emoji, not color alone.

NFR9: No auth in MVP (AD-10) — no user accounts, sessions, or client API keys.

NFR10: Demo environment — device and laptop on same Wi-Fi; backend `:8080`; Expo host-derived API URL.

### Additional Requirements

**Product Principles (acceptance on every diagnosis-output story):**

- P1: Honest uncertainty over false confidence.
- P2: User always leaves with a safe, actionable next step (including Low / Unidentified Issue).
- P3: KB-grounded answer takes precedence over general/unverified when both could apply.
- P4: Healthy is celebratory, not blank.
- P5: Unidentified Issue is a valid, tested outcome — not a bug.

**Demo Acceptance Criteria (each must map to a verification story later):**

- DAC-1: All must-pass demo cases (OQ-1 — **pairs TBD, stakeholder to confirm**) produce expected diagnosis + recommended action.
- DAC-2: Healthy test images are never diagnosed with a disease.
- DAC-3: Insufficient confidence → Unidentified Issue, not a fabricated disease.
- DAC-4: Species-specific treatments only when High (plant-name match).
- DAC-5: Stakeholder completes E2E flow without developer help.
- DAC-6: NVIDIA fallback path E2E, same criteria as OpenAI primary — dedicated story, not assumed.

**Architecture (ADs that bind implementation):**

- Brownfield: layered monolith + linear pipeline AD-5: save → vision → retrieve → synthesize → persist → respond.
- AD-1: no AI calls from mobile.
- AD-2: snake_case Diagnosis contract including `is_healthy`.
- AD-3 / AD-4 (revised): dual RAG; empty candidates only when neither match; bounded list only.
- AD-6: OpenAI `gpt-4o` is the **target** primary; NVIDIA vision required; NVIDIA text fallback; DeepSeek must not run on `diagnosePlant()`. **Not built yet:** live `diagnosePlant()` still calls DeepSeek. Epic 2 must include an explicit “Wire OpenAI, stop calling DeepSeek” story. Other stories that mention OpenAI are **target-state ACs**, not already done.
- AD-8 / AD-9: Flyway + CSV-only KB.
- AD-11 / AD-12 / AD-13: secrets, Expo host URL, UUID uploads.
- No starter/greenfield template — brownfield repo (`backend/`, `mobile/`).

**Named gaps (do not invent answers):**

- **OQ-1 / D-6:** 3–5 must-pass plant+disease pairs — **TBD, stakeholder to confirm.** Blocks DAC-1 seed photos and FR-17 priority list. Stories may exist as placeholders only.
- **OQ-8:** Regional/climate focus of research — unanswered. Does not block stories; do not assume Indian-local copy as locked differentiator.
- **OQ-6:** Who owns CSV updates during sprint — unanswered; ops process not assumed.
- **Non-plant photo (decided 2026-08-22):** Same as Low / Unidentified Issue. Vision expresses honest uncertainty; retrieval finds no plant-name or symptom-pattern match. **No new screen. No separate outcome.**
- **First-time “what to photograph” coaching on Home:** PRD/UX specify Home actions and Loading tips, not a dedicated onboarding tutorial. Do not invent a new screen unless confirmed.
- **Anxious-user reassurance:** tone is specified (P2, FR-12, EXPERIENCE voice); no separate persona-gated features.
- UX spines (2026-08-01) do not distinguish High vs Medium visually; Unidentified is “same as issue.” **PRD FR-10 / FR-12 win** for those stories.

**Out of MVP (do not story):** auth, chat, history UI, reminders, catalog, social, store release, CMS, offline AI, raw KB browse, auto-ingest AI guesses into KB.

### UX Design Requirements

UX-DR1: Theme tokens — implement `DESIGN.md` colors in `mobile/src/theme/colors.ts` as single source of truth; do not hardcode old prototype greens (`#1B4332`, `#2D6A4F`, `#52B788`).

UX-DR2: Color logo — `logo-color.png` on Home and Results (`background`); do not use color logo on dark surfaces.

UX-DR3: Primary button — CTA `#FE8D10`, white label, 16px radius — Take Photo, Diagnose Another, Try Again.

UX-DR4: Secondary button — transparent, 2px `#003A33` border — Choose from Gallery.

UX-DR5: Info cards — white surface, 20px radius, hairline `#DDE8E3` border; 24px screen padding; 16px section gaps.

UX-DR6: Status banner — Healthy = green `#017B00` on `#E6F4E6`, copy “Plant looks Healthy!”; Issue = amber `#C45A00` on `#FFF4E8`, “Issue Detected”.

UX-DR7: Confidence-tier Results treatment — **PRD overrides UX here:** High vs Medium vs Low must be distinguishable in UI/copy (FR-10, FR-12). UX file currently only has Healthy / Issue / Unidentified-same-as-issue.

UX-DR8: Unidentified Issue — **PRD locked copy/experience (FR-12)**; issue styling not Error screen; reassuring, not “we cannot help you.” UX “same as issue” is insufficient alone.

UX-DR9: Results hierarchy — logo → status banner → plant → diagnosis → symptoms → solution → confidence → disclaimer → CTA.

UX-DR10: Disclaimer component — centered meta text below confidence note; FR-18 exact wording.

UX-DR11: Loading — secondary-green spinner; status ≥15s; tips ≥6s (`EXPERIENCE.md` / FR-4).

UX-DR12: Error screens — timeout “Request Timed Out”; network/connection title; warm error background; orange Try Again; no stack traces.

UX-DR13: Navigation — Home → Loading → Results | Error; `popToTop()` back to Home; no tabs, drawer, history, pull-to-refresh.

UX-DR14: Typography — platform system fonts; card labels 11pt uppercase muted; plant name ~22pt extra-bold primary; body 14–15pt; footer 11pt muted.

UX-DR15: Accessibility — logo label “Upavana”; AA contrast; ≥44pt touch targets; status not by color alone.

### FR Coverage Map

FR1: Epic 1 — Camera capture + permission denied
FR2: Epic 1 — Gallery pick + permission denied
FR3: Epic 1 — Upload to `/api/diagnose`, 180s abort
FR4: Epic 1 — Loading tips/status (first-time / anxious wait)
FR5: Epic 2 — NVIDIA vision only
FR6: Epic 2 — Dual RAG + bounded candidates + Medium when plant name is wrong
FR7: Epic 2 — Wire OpenAI `gpt-4o` (not yet live), NVIDIA text fallback, DeepSeek off, confidence-tier JSON
FR8: Epic 3 — Persist query + image
FR9: Epic 1 (client-facing errors) + Epic 2 (API 400/500 contract)
FR10: Epic 2 — Results display + High/Med/Low visible
FR11: Epic 2 — Healthy celebratory UX (P4)
FR12: Epic 2 — Issue + locked Unidentified Issue UX; non-plant uses this path
FR13: Epic 1 — Diagnose another (repeat user)
FR14: Epic 1 — Timeout Error screen
FR15: Epic 1 — Network/server Error screen, no jargon
FR16: Epic 3 — CSV seed + re-seed idempotent + health counts
FR17: Epic 3 + Epic 4 — Demo seed; OQ-1 pairs TBD
FR18: Epic 2 — Disclaimer on Results

DAC-1: Epic 4 — placeholder until OQ-1 named
DAC-2: Epic 4 — healthy never a disease
DAC-3: Epic 4 — Unidentified Issue, not fabricated
DAC-4: Epic 4 — species-specific only at High
DAC-5: Epic 4 — stakeholder solo E2E
DAC-6: Epic 4 — dedicated NVIDIA fallback E2E story

P1–P5: Epic 2 diagnosis ACs + Epic 4 checks
UX-DR1–UX-DR15: Epic 1 (Home/Loading/Error) + Epic 2 (Results)

## Epic List

### Epic 1: Take a photo and get through the wait (or recover)

First-time, anxious, and repeat users can capture or pick a photo, wait with clear status/tips, retry after timeout/network failure, and start another plant — without jargon or a dead end.

**FRs covered:** FR1, FR2, FR3, FR4, FR9 (client-facing errors), FR13, FR14, FR15  
**UX:** UX-DR2–4, UX-DR11–15 (Home, Loading, Error)  
**User value:** The app is usable even while diagnosis quality is still being finished.  
**Notes:** Foundational / mostly brownfield. Permission denied, missing/invalid upload, 180s abort. Does **not** assume OpenAI is live.

### Epic 2: Get an honest diagnosis (High / Medium / Low / Healthy)

The user sees a clear, principle-honoring answer: KB-grounded when we matched, honest Unidentified Issue when we did not (including non-plant photos), celebratory Healthy, locked disclaimer. **OpenAI `gpt-4o` is the target writer — it is not live today.**

**FRs covered:** FR5, FR6, FR7, FR9 (API contract), FR10, FR11, FR12, FR18  
**UX:** UX-DR1, UX-DR5–10, UX-DR14 — **PRD wins** on High vs Medium vs Unidentified copy  
**Must include (not already done):** Story to **wire OpenAI into `diagnosePlant()`, stop calling DeepSeek.** NVIDIA text fallback is target-state; **verified in Epic 4 (DAC-6)**, not assumed here.  
**P1–P5** on every diagnosis-output story.

### Epic 3: Keep research in the database (seed, health, save results)

Operators can seed/re-seed CSV without duplicates, see non-zero health counts, and know every successful diagnosis is stored for later gap review — without dumping the full KB into any AI prompt.

**FRs covered:** FR8, FR16, FR17 (seed mechanics; must-pass **pairs TBD**)  
**User value:** Demo and RAG have data; research stays in-system (AD-14).

### Epic 4: Demo-ready — prove every DAC

Stakeholder can run the demo alone. Each DAC-1–DAC-6 has its own check, including a **dedicated NVIDIA fallback E2E story (DAC-6)**. DAC-1 uses **OQ-1 pairs TBD — stakeholder to confirm**.

**FRs covered:** FR17 (coverage once pairs named), DAC-1–DAC-6  
**Depends on:** Epic 2 target-state (OpenAI wired) for primary-path DACs; DAC-6 forces the OpenAI-fail path.

<!-- Individual stories added in step 3 after epic-list approval. -->
