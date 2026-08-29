---
title: UPAVANA Plant Doctor MVP
status: draft
gate: Gate 2 — APPROVED WITH MINOR CHANGES (incorporating stakeholder feedback 2026-08-11)
created: 2026-08-01
updated: 2026-08-11
author: John (Product Manager)
prepared_for: Aksha → Stakeholder (sir) approval
sources:
  - _bmad-output/planning-artifacts/briefs/brief-plant-doctor-2026-08-01/brief.md (Gate 1 approved)
  - _bmad-output/project-context.md
  - _bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md
resolved_open_questions:
  - OQ-3
  - OQ-4
  - OQ-7
  - D-1
  - D-4
  - D-7
  - D-8
superseded_decisions:
  - D-2
  - D-3
  - D-5
  - OQ-2
stakeholder_priorities:
  - working
  - scalable
  - simple_yet_effective
---

# PRD: UPAVANA Plant Doctor MVP

---

## 1. Product Vision — Why UPAVANA Exists

**Who we’re for, what hurts, what we do, and why we’re not “just use ChatGPT” or “just use Planta.” Plain language on purpose — our users are casual, not botanists.**

### Who we’re building for

UPAVANA is for **casual planters** — people who grow things because they enjoy it, not because it’s their job.

That includes:

- **Balcony and mini-farm growers** — a few tomatoes, chilies, herbs, or flowers in pots or a small patch.
- **Houseplant collectors** — a growing mix of indoor plants, often bought one at a time, cared for with love and guesswork.
- **The post-pandemic wave** — especially younger urban owners (Gen Z and millennials) who got into plants during lockdown and kept going. For many, plants are part of home aesthetic and identity, not just decoration.

Three people we keep in mind:

| Persona | What they’re like | What they need from us |
|---------|-------------------|------------------------|
| **The Instagram-era houseplant hobbyist** | Urban, younger, emotionally invested in their collection. Likely to download a dedicated app for something they already care about. | Fast, trustworthy answers that feel made for *their* plant — not a generic blog post. |
| **The anxious new plant parent** | Got into plants in the last 1–2 years. No baseline knowledge. Panics at the first brown spot. Googles and gets 10 conflicting answers. | One clear next step — calm, honest, actionable — not more noise. |
| **The “I already tried ChatGPT” user** | Uploaded a photo or described symptoms to a general AI and got something vague, overconfident, or hard to act on. | A tool built for this exact moment: photo in, structured answer out, grounded in real research. |

**Sharpest starting niche:** *The new, anxious Indian urban plant parent* — bought their first few houseplants recently, doesn’t know local pest/disease names in English or regional terms, and gets advice from global apps that assume different climates and growing conditions. They want a **fast, free, trustworthy answer** — not another subscription “plant lifestyle” app.

`[OPEN]` **OQ-8:** Confirm with stakeholder whether the curated research dataset is explicitly focused on Indian houseplants and local conditions. If yes, that becomes a headline differentiator in marketing and in-app copy.

### What’s going wrong for them today

When a leaf spots, yellows, or curls, casual planters don’t want a lecture — they want to know **what’s wrong and what to do now**.

| What they try | Why it fails |
|---------------|--------------|
| **Google** | Ten tabs, conflicting advice, hard to match generic articles to *this* photo. |
| **ChatGPT / Claude / other general AI** | Can analyze a photo, but answers vary every time, often sound confident when they shouldn’t, and pull from mixed-quality internet training — not your team’s vetted research. |
| **Planta, Plantora, PlantAI, and similar** | Diagnosis is buried inside big “plant lifestyle” apps (reminders, social, calculators). **Free tiers usually stop at watering reminders** — real health tools sit behind paywalls. Data is often huge but generic (crowd-sourced scale, not curated accuracy). Some apps have trust issues (unclear subscriptions, auto-enrollment complaints). |

**Market shape in one line:** broad, subscription-gated lifestyle platforms where diagnosis is one paywalled feature among many, built on massive generic databases — not a focused, honest tool for the panic moment.

### What UPAVANA does instead

UPAVANA does **one job well:** you see something wrong → you take a photo → you get a clear, structured answer.

- **Plant name** (or honest uncertainty if we’re not sure)
- **What might be wrong** (or “healthy” — that’s a win too)
- **Symptoms we noticed**
- **What to do next** — safe, practical steps
- **How confident we are** — we’d rather say “not sure” than guess wrong

No social feed. No watering calendar in v1. No paywall on the core diagnosis flow for MVP. Built for the **crisis moment** when someone is standing next to a sick plant with their phone in hand.

### Where we can genuinely win (not marketing fluff)

**1. Depth over breadth, on the one thing that matters.**

Nobody opens a plant app in a panic wanting a watering reminder and a community feed. They want to know what’s wrong **right now**. UPAVANA is deliberately simple: photo in, real answer out. That’s a strength for this use case, not a missing feature.

**2. Curated research over crowd-sourced scale.**

Planta’s “40 million plants” is a **breadth** story. UPAVANA’s story is **trust on the plants we cover** — proprietary, vetted research, not scraped forums. A smaller knowledge base that’s accurate beats a massive one that’s generic. When we don’t know, we say **Unidentified Issue** instead of making something up. Competitors rarely make that trade-off because scale is their pitch.

**3. Free, accessible diagnosis.**

Most competitors lock diagnosis behind premium. Offering real diagnosis for free (MVP) is a real wedge — especially in price-sensitive markets like India, where our sharpest niche lives.

### Why not just use ChatGPT, Claude, or another general AI?

Yes, they can look at a plant photo. That’s not the gap. The gap is **what the product is designed to do:**

| General AI chat | UPAVANA |
|-----------------|---------|
| Trained to sound helpful — often **overconfident** when uncertain | Built to **admit uncertainty** (High / Medium / Low confidence; Unidentified Issue when needed) |
| Mixed-quality plant knowledge from the open web | Answers **grounded in our Knowledge Base** when we have a match |
| You write a new prompt every time | **One tap:** photo → same clear structure every time |
| Output format changes every reply | Same fields every time: plant, issue, symptoms, solution, confidence |
| No connection to **local** pests, diseases, or climate | `[OPEN]` OQ-8 — opportunity to localize for Indian urban growers |

For a casual planter deciding whether to cut a leaf or spray something, **a confident wrong answer is worse than an honest “I’m not sure — here’s safe general guidance.”** UPAVANA is built for that decision, not for open-ended chat.

### Why not Planta and the rest?

| | UPAVANA | Typical competitor (Planta, Plantora, PlantAI, …) |
|---|---------|-----------------------------------------------------|
| **Focus** | Single-purpose: “my plant looks sick” | All-in-one plant lifestyle platform |
| **Diagnosis access** | Free core flow (MVP) | Often paywalled; free tier = reminders only |
| **Data** | Curated, vetted research (focused set) | Massive generic / crowd-sourced databases |
| **Honesty** | Confidence tiers + Unidentified Issue | Tends toward confident-sounding answers |
| **Localization** | `[OPEN]` OQ-8 — built toward Indian urban conditions | Global defaults; climate mismatch for many Indian users |

We’re not trying to be the biggest plant app. We’re trying to be the **most useful app in the five minutes after someone notices a brown spot.**

---

## 1.1 Product Principles

**Behavioral rules for UX, AI prompts, and copy — every diagnosis must honor these.**

| # | Principle |
|---|-----------|
| P1 | Prefer **honest uncertainty** over incorrect certainty. |
| P2 | Every diagnosis leaves the user with a **safe, actionable next step**. |
| P3 | **Knowledge Base–grounded** recommendations always take precedence over general / unverified responses. |
| P4 | **Healthy** plants are celebrated as a positive outcome, not a null result. |
| P5 | **Unidentified Issue** is a valid product outcome, not an error state. |

---

## TL;DR — Executive Scan (~2 min)

**Gate 2 status:** Stakeholder replied **APPROVED WITH MINOR CHANGES** (2026-08-11). This revision incorporates that feedback. **Gate 3 blocked until Critical Decision (OQ-1) is answered.**

### What we're building

| | |
|---|---|
| **Product** | UPAVANA Plant Doctor — React Native app + Spring Boot API |
| **Who it's for** | Casual planters (balcony/mini-farm growers + houseplant hobbyists). Sharpest niche: **new, anxious Indian urban plant parent** — see §1. |
| **Core flow** | Photo → NVIDIA vision → dual RAG (MySQL) → OpenAI `gpt-4o` synthesis → structured Diagnosis JSON |
| **Confidence tiers** | High (plant-name KB) · Medium (symptom-pattern KB) · Low (Unidentified Issue + labeled general guidance) |
| **MVP boundary** | Photo in, diagnosis out. No auth, chat, store release, or CMS. |
| **Brownfield** | Backend, RAG pipeline, CSV seed, `/api/diagnose`, rough mobile screens **exist** — PRD formalizes against `project-context.md` + architecture spine. |
| **Timeline** | Demo in **1 month**; week-1 end-to-end draft. Gate 1 brief **approved**. |

### Active stack (one glance)

| Layer | Active MVP | Fallback / inactive |
|-------|------------|---------------------|
| **Vision** | NVIDIA NIM `meta/llama-3.2-11b-vision-instruct` | — |
| **RAG** | Plant-name + symptom-pattern (`DiseaseCandidate`) | — |
| **Synthesis** | OpenAI **`gpt-4o`** + `json_schema` → `DiagnosisResult` `[ACTIVE]` D-8 | NVIDIA NIM text `meta/llama-3.1-8b-instruct` |
| **Synthesis (inactive)** | DeepSeek code in repo — **not called** on active path | Retained for future |
| **DB / API** | MySQL + Flyway; `POST /api/diagnose` snake_case JSON | — |
| **Mobile** | Expo ~54; 180s client timeout; UPAVANA theme | — |

**Pipeline order (AD-5):** save image → vision → retrieve → synthesize → persist Query → respond.

### Status at a glance

| Tag | Meaning |
|-----|---------|
| `[ACTIVE]` | Settled — no input needed |
| `[SUPERSEDED]` | Replaced — kept for audit trail |
| `[OPEN]` | **Needs your decision** |
| `[CRITICAL]` | Blocks Gate 3 / demo prep |

| Area | Status |
|------|--------|
| Gate 1 brief | `[ACTIVE]` Approved |
| Gate 2 PRD | `[ACTIVE]` Approved with minor changes (this revision) |
| D-1 NVIDIA vision only | `[ACTIVE]` |
| D-4 dual RAG | `[ACTIVE]` Implemented |
| D-7 OpenAI synthesis primary | `[ACTIVE]` |
| D-8 Lock synthesis to **`gpt-4o`** | `[ACTIVE]` Resolves OQ-7 |
| OQ-3 / Unidentified Issue UX | `[ACTIVE]` Standardized (see FR-12) |
| OQ-4 disclaimer copy | `[ACTIVE]` |
| Confidence tiers (High/Med/Low) | `[ACTIVE]` FR-6 / FR-7 / FR-12 |
| AD-6 (OpenAI synthesis) | `[ACTIVE]` |
| AD-3/AD-4 spine plant-only text | `[OPEN]` Architect align to FR-6 / D-4 at Gate 3 |

### Critical Decision Before Gate 3

| ID | Question | Blocks |
|----|----------|--------|
| **`[CRITICAL]` `[OPEN]` OQ-1 / D-6** | Which **3–5 plant+disease pairs** are must-pass for the demo? | KB seeding (FR-17), SM-1, Demo Acceptance Criteria, pipeline validation, demo photos |

### Other open items (not Gate 3 blockers)

| ID | Question | Owner |
|----|----------|-------|
| **`[OPEN]` OQ-5** | Post-MVP priority: chat vs KB expansion vs UX polish | Stakeholder (roadmap) |
| **`[OPEN]` OQ-6** | Who owns CSV updates during sprint when gaps found? | PM / Stakeholder (ops) |
| **`[OPEN]` OQ-8** | Does curated research have a specific regional/climate focus (e.g. Indian houseplants)? If yes, state as core differentiator. | Stakeholder |

### Demo Acceptance Criteria (checkable)

- [ ] All must-pass cases (OQ-1) → expected diagnosis + recommended action
- [ ] Healthy test images → never diagnosed with a disease
- [ ] Insufficient confidence → **Unidentified Issue**, not fabricated disease
- [ ] Species-specific treatments only when plant ID confidence is High (plant-name match)
- [ ] Stakeholder completes E2E flow without developer assistance
- [ ] **NVIDIA fallback path** meets the same criteria as OpenAI primary (deliberately tested E2E before demo)

**Technical detail:** `addendum.md` (API, env vars, pipeline, demo checklist).

---

## Full PRD — Requirements & Spec

---

## 0. Document Purpose

**Brownfield requirements doc for MVP demo — FR-1–FR-18 with testable consequences; Gate 2 approved with minor changes; Gate 3 blocked on OQ-1.**

| Field | Value |
|-------|-------|
| Gate 1 | `[ACTIVE]` Brief approved |
| Gate 2 | `[ACTIVE]` APPROVED WITH MINOR CHANGES (2026-08-11) — this revision |
| Gate 3 | `[CRITICAL]` Blocked until OQ-1 answered |
| Downstream | `bmad-architecture` (AD-3/4 align), `bmad-ux`, `bmad-create-epics-and-stories` |
| Deep dive | `addendum.md` |

### 0.1 Requirement bar (all FRs)

| Priority | Bar |
|----------|-----|
| **Working** | Testable consequences — demo-verifiable |
| **Scalable** | v1 limits noted; v2 door left open where relevant |
| **Simple yet effective** | Core diagnosis flow only; else §6 Non-Goals |

---

## 2. MVP Snapshot

**Photo → RAG-grounded Diagnosis in &lt;3 min; healthy and unidentified outcomes are first-class — not Planta parity.**

| | |
|---|---|
| **User questions** | What might be wrong? What to do this week? |
| **Output** | `plant_name`, `disease_name`, `symptoms_matched`, `solution`, `confidence_note`, `is_healthy` |
| **Differentiator** | KB-grounded when match exists; honest soft fallback when not |
| **Delivery** | 1-month demo; week-1 E2E; backend/pipeline already running |

---

## 3. Target User

**Casual planters — balcony growers, mini-farm hobbyists, and houseplant collectors who want a quick photo diagnosis, not a pro horticulture tool or a full “plant lifestyle” platform. See §1 for personas and niche.**

### 3.1 Jobs To Be Done

- **Functional:** “What’s wrong with my plant?” — identify possible disease or pest from a photo; get steps I can actually do this week.
- **Emotional:** Less panic when I see a brown spot; real relief when the plant is healthy.
- **Contextual:** Answer in under a minute while I’m standing next to the plant — not a 45-minute Google rabbit hole.

### 3.2 Non-Users (v1)

Professional growers, commercial nurseries, users needing offline mode or certified agricultural advice, users who mainly want watering reminders, plant libraries, social feeds, or subscription lifestyle features.

### 3.3 Key User Journeys

**UJ-1. Priya diagnoses her yellowing pothos**

Priya, 28, lives in Bangalore. She got into plants during the pandemic — now has pothos, a money plant, and a few herbs on the balcony. She’s not an expert. She sees yellow leaves, Googles “pothos yellow leaves,” gets conflicting advice, and feels worse. She opens UPAVANA instead and photographs the leaves.

- Home → **Take Photo** → Loading (tips + status) → Results with plant name, issue, symptoms, solution.
- If the Knowledge Base has a match, the solution reflects that research (High or Medium confidence tier).
- If not, she sees **Unidentified Issue** (Low tier) with **general guidance** — not a hard block or error wall.

**UJ-2. Marcus checks a healthy snake plant**

Marcus uploads a gallery photo. Results show a positive **Healthy** state (`is_healthy: true`, green styling, encouraging copy).

**UJ-3. Stakeholder demo**

Pre-seeded device, known demo photo (once stakeholder names must-pass pairs — `[CRITICAL]` OQ-1), full flow under 3 minutes without developer help. Solution text for KB hits should be verifiable against database records. NVIDIA fallback path pre-tested to the same Demo Acceptance Criteria.

---

## 4. Glossary

**Term reference — skip if familiar.**

| Term | Meaning |
|------|---------|
| **Diagnosis** | API JSON: `plant_name`, `disease_name`, `symptoms_matched`, `solution`, `confidence_note`, `is_healthy`. |
| **Knowledge Base** | MySQL `plants` and `diseases` tables, filled from CSV seeding. |
| **RAG** | Retrieve-Augment-Generate: Vision → find candidates in KB → AI synthesis using those records when relevant. |
| **Vision Analysis** | NVIDIA NIM step: image → text (plant guess, symptoms, morphology). |
| **Synthesis** | **OpenAI `gpt-4o`** (active primary for MVP) with structured JSON output; NVIDIA text fallback. DeepSeek client code retained in repo but **not** in the active pipeline. |
| **Plant-name match** | Candidate found because vision text matches plant `name` or `common_names`. Maps to **High** confidence tier. |
| **Symptom-pattern match** | Candidate found because vision symptoms align with a disease’s `symptoms` or `disease_name` **across any plant**. Maps to **Medium** confidence tier. |
| **Confidence tier** | High / Medium / Low classification of every diagnosis; communicated via `confidence_note` and Results UX. |
| **Query** | Server-side audit row (`queries` table); not shown in mobile UI in MVP. |
| **Unidentified Issue** | Low-tier soft outcome: system could not confidently identify the issue; intentionally conservative; user still gets safe, actionable general guidance. Valid product outcome — not an error. |

---

## 5. Features

**FR-1–FR-18 — all requirements and testable consequences below.**

---

### 5.1 Photo Capture & Upload

**Home → camera or gallery → Loading. Permissions on demand.**

#### FR-1: Camera capture

**Consequences (testable):**
- Camera permission requested before launch; alert if denied.
- Successful capture navigates to Loading with `imageUri`.
- Cancel returns to Home.

#### FR-2: Gallery selection

**Consequences (testable):**
- Media library permission requested; alert if denied.
- Selected image navigates to Loading with `imageUri`.
- Images only (no video).

**Scalability note:** Same flow supports future deep links or share extension without changing the API contract.

---

### 5.2 Diagnosis Request & Loading

**Multipart upload to `/api/diagnose`; spinner + tips until response or 180s abort.**

#### FR-3: Image upload to API

**Consequences (testable):**
- `POST /api/diagnose`, `multipart/form-data`, field name **`image`**.
- URL: `http://{metro-host-ip}:8080/api/diagnose` (from Expo host — not hardcoded `localhost` on device).
- FormData shape: `{ uri, name, type }`.
- Client aborts at **180 seconds** → Error with timeout flag.

#### FR-4: Loading engagement

**Consequences (testable):**
- Status message updates at least every 15 seconds.
- Tips rotate at least every 6 seconds.
- Copy sets expectation that analysis may take up to a minute or more.

---

### 5.3 Diagnosis Pipeline (Backend)

**`save image → NVIDIA vision → dual RAG → OpenAI gpt-4o synthesis → Query → JSON` (AD-5). Confidence tiers High / Medium / Low.**

#### FR-5: Vision analysis (NVIDIA NIM only)

`[ACTIVE]` **D-1** — NVIDIA NIM only; not OpenAI vision.

**Consequences (testable):**
- Missing `NVIDIA_API_KEY` → clear server error (not silent failure).
- Default model: `meta/llama-3.2-11b-vision-instruct` (configurable via env).
- Up to **2 retries**, **25s** timeout per attempt; failures logged.
- Vision prompt asks for leaf morphology, growth habit, honest uncertainty, and detailed symptoms (current `NvidiaClientService.analyzeImage` behavior).

**Scalability note:** Vision provider is swappable behind one service method; pipeline shape unchanged.

#### FR-6: Knowledge Base retrieval (dual RAG) + confidence tier triggers

`[ACTIVE]` **D-4** — plant-name **and** symptom-pattern matching (implemented).

Retrieval uses **both**:
1. **Plant-name matching** — disease included when vision text matches plant `name` or comma-separated `common_names`. → **High** confidence tier trigger.
2. **Symptom-pattern matching** — when not already included, score diseases by overlap between vision text and `symptoms` + `disease_name` across **all plants**; include top matches above a minimum score threshold (current `DiagnosisService.findCandidateDiseases` with `DiseaseCandidate` types `PLANT_NAME` and `SYMPTOM_PATTERN`). → **Medium** confidence tier trigger.
3. Neither match → empty or non-fitting candidates → **Low** confidence tier (Unidentified Issue path in FR-7 / FR-12).

**Consequences (testable):**
- Plant-name matches are always included when they apply.
- Symptom-pattern matches appear when symptoms align even if plant name does not (e.g., leaf spot pattern on wrong plant guess).
- Candidates are labeled for synthesis (`PLANT_NAME_MATCH` vs `SYMPTOM_PATTERN_MATCH` in prompts).
- The full Knowledge Base is **never** dumped into the synthesis prompt — only scored top candidates (bounded list).
- Unit tests cover: plant-name path; symptom-only path when plant misidentified; plant match preferred when both apply.
- Match type determines confidence tier (see FR-7 confidence table).

**Scalability note:** CSV + MySQL scale to more plants/diseases without code changes; scoring thresholds tunable via constants.

#### FR-7: Diagnosis synthesis + confidence-tiered response

`[ACTIVE]` **D-7** (2026-08-08) — OpenAI primary; NVIDIA text fallback; DeepSeek **in codebase, not on active path**.

`[ACTIVE]` **D-8** (2026-08-11) — Active synthesis model locked to **`gpt-4o`** for MVP demo. Quality over inference cost; cost optimization deferred post-demo. Resolves OQ-7.

`[SUPERSEDED]` D-2, D-3, D-5 (DeepSeek as MVP primary).

`[ACTIVE]` **OQ-3** — soft **Unidentified Issue** + general guidance; not hard unsupported-plant block.

| Topic | Detail |
|-------|--------|
| **Reason for D-7** | DeepSeek slow + unsatisfactory quality in testing; OpenAI free dev tier sufficient for MVP |
| **Active path** | `OPENAI_API_KEY` → Chat Completions + **`response_format: json_schema`** → `DiagnosisResult`; model **`gpt-4o`** only (D-8) |
| **Fallback** | OpenAI fail/missing key → NVIDIA NIM text `meta/llama-3.1-8b-instruct`, **2 retries**, **25s**/attempt |
| **Inactive** | `synthesizeDiagnosisWithDeepSeek` / `DEEPSEEK_*` — retained, not invoked on MVP path |
| **Budget** | Total pipeline ≤ mobile **180s** |

##### Confidence-tiered response (required)

| Tier | Trigger | Response |
|------|---------|----------|
| **High** | Plant-name match in KB (`PLANT_NAME_MATCH`) | Grounded answer — `solution` reflects curated research directly. Species-specific treatment allowed. |
| **Medium** | No plant-name match, but symptom-pattern match (`SYMPTOM_PATTERN_MATCH`) | Grounded answer using matched disease/solution; `confidence_note` explicitly states the pattern is consistent with the disease but **exact plant type was not confirmed** in KB. |
| **Low** | No plant-name match AND no symptom-pattern match | General AI knowledge (**not** KB-grounded), explicitly labeled — e.g. *"This isn't in our curated research yet, so here's general guidance: [advice]. If this looks urgent, consider consulting a local nursery or plant expert."* `disease_name` = **Unidentified Issue**; `is_healthy` = `false`. Never claims species-specific certainty. |

**Consequences (testable):**
- When a **plant-name** candidate fits plant + symptoms, `solution` reflects KB treatment (may be enhanced, not contradicted) — High tier.
- When only **symptom-pattern** candidates fit, synthesis may use that disease name and solution; `confidence_note` explains pattern may apply across species and exact plant was not confirmed in KB — Medium tier.
- When no candidate fits, `disease_name` is `Unidentified Issue`, `is_healthy` is `false`, `solution` gives **general care guidance** with Low-tier labeling — see FR-12 locked copy.
- Healthy plant → `disease_name` `Healthy`, `is_healthy` `true`.
- Output is valid JSON per `DiagnosisResult`; markdown fences stripped before parse.
- Every diagnosis includes a confidence tier communicated via the existing **`confidence_note`** field.
- Medium and Low tier responses are never mistakable for a High-tier KB-grounded plant-specific answer.
- Low-tier responses never claim species-specific certainty.
- Species-specific treatments only when High tier (plant identification confidence sufficiently high).
- **Active path:** When `OPENAI_API_KEY` is set → OpenAI Chat Completions with **`response_format: json_schema`** matching `DiagnosisResult`; model **`gpt-4o`** (D-8).
- **Fallback:** On OpenAI failure or missing key → NVIDIA NIM text (`meta/llama-3.1-8b-instruct`), up to **2 retries**, **25s** timeout per attempt.
- **NVIDIA fallback validation:** Fallback path must be **deliberately tested end-to-end before the stakeholder demo** (not assumed because code exists). Same Demo Acceptance Criteria as primary path. Pre-demo checklist item.
- **Inactive:** DeepSeek is **not** invoked on the active MVP path (code may remain for future re-enable).
- Total pipeline stays within mobile **180s** budget.

**Scalability note:** Synthesis interface accepts `List<DiseaseCandidate>`; provider selection is config-driven without API or mobile changes.

**Roadmap (non-MVP):** Low/Medium diagnoses already persist in `queries`. Post-MVP, humans can review recurring gaps to prioritize KB research additions. **Out of scope forever without human verification:** automated ingestion of unverified AI guesses into the Knowledge Base — user/AI-guessed content must never become “grounded” data without human review (preserves P3 trust principle).

#### FR-8: Diagnosis persistence

**Consequences (testable):**
- Each successful diagnosis creates a `queries` row with `image_url` and `result_json`.
- Image saved under `uploads/` with UUID filename.

**Scalability note:** `queries` table supports future history/analytics and post-MVP KB gap analysis; MVP does not expose it to users.

#### FR-9: API error handling

**Consequences (testable):**
- Missing/empty image → `400`, `{ "error": "Image file is required" }`.
- Processing failure → `500`, `{ "error": "<message>" }` (no stack traces).
- Success → `200`, Diagnosis JSON, **snake_case** fields.

---

### 5.4 Results Presentation (UPAVANA)

**Results screen — UPAVANA tokens (`DESIGN.md`, `colors.ts`); healthy / High / Medium / Low (Unidentified) states.**

#### FR-10: Diagnosis display

**Consequences (testable):**
- Renders: plant name, disease/issue, symptoms matched, solution, confidence note.
- Uses UPAVANA palette (e.g. background `#F7FAF8`, primary `#003A33`, CTA `#FE8D10`).
- Long solution text scrolls.
- Confidence tier is visible via `confidence_note` (and any UX treatment distinguishing High vs Medium vs Low).

#### FR-11: Healthy plant UX

**Consequences (testable):**
- Healthy when `is_healthy === true` **or** `disease_name` indicates health (e.g. contains "Healthy").
- Green celebratory styling; never shown as error or empty state (P4).
- Mobile `DiagnosisData` includes `is_healthy` from API.

#### FR-12: Issue-detected and Unidentified Issue UX (standardized)

**High / Medium issue states:** Non-healthy uses warning styling (amber accents per design system). Confidence note in subdued footer style. Medium must be visually/textually distinguishable from High — never mistakable for plant-confirmed KB grounding.

**Low tier — Unidentified Issue (locked experience):**

Implements P1 (honest uncertainty) and P5 (valid outcome, not error).

| Element | Locked behavior / copy intent |
|---------|-------------------------------|
| **Outcome** | `disease_name` = **Unidentified Issue**; `is_healthy` = `false` |
| **Message** | System **could not confidently identify** the issue |
| **Framing** | Diagnosis is **intentionally conservative**, not speculative — better to say we don’t know than invent a disease |
| **Action** | User still receives **safe, actionable** general guidance in `solution` |
| **Labeling** | Must state guidance is **not** from curated research yet (align with FR-7 Low-tier example) |
| **UI** | Issue styling (not Error screen); reassuring, helpful — not “we cannot help you” |
| **Never** | Fabricated disease name; species-specific treatment certainty; presentation as a hard failure |

**Canonical Low-tier guidance pattern (solution / confidence_note):**

> We couldn’t confidently identify this issue from our curated research, so we’re being cautious rather than guessing. Here’s general guidance: [safe next steps]. If this looks urgent, consider consulting a local nursery or plant expert.

**Consequences (testable):**
- Non-healthy uses warning styling (amber accents per design system).
- **`Unidentified Issue`** uses issue styling but copy is **reassuring and helpful** — general guidance in `solution`, not “we cannot help you.”
- Confidence note in subdued footer style.
- Low-tier copy communicates: could not confidently identify; intentionally conservative; safe actionable guidance remains.
- Medium and Low never look like High-tier plant-confirmed KB answers.

#### FR-13: Diagnose another

**Consequences (testable):**
- **Diagnose Another Plant** resets to Home.
- No diagnosis history in mobile UI (MVP).

---

### 5.5 Error Handling (Mobile)

**Timeout (180s), network, and server errors → Error screen + Try Again → Home. Unidentified Issue is NOT this path.**

#### FR-14: Timeout error

**Consequences (testable):**
- Error screen with `isTimeout: true`; explains server busy / network; **Try Again** → Home.

#### FR-15: Network and server errors

**Consequences (testable):**
- Friendly copy; server `error` message shown when present; no stack traces; **Try Again** → Home.

---

### 5.6 Knowledge Base Management (Operations)

**CSV seed only — not user-facing. Must-pass pairs from `[CRITICAL]` OQ-1.**

#### FR-16: CSV seeding

**Consequences (testable):**
- `POST /api/admin/seed` loads CSV (`plant_name,disease_name,description,symptoms,causes,solution`).
- Re-seed skips duplicate plant+disease pairs (case-insensitive).
- `GET /api/health` returns `plants` and `diseases` counts.

**Scalability note:** Adding plants/diseases is data-only (CSV) until an admin CMS is justified post-MVP.

#### FR-17: Demo seed coverage

**Consequences (testable):**
- Knowledge Base is seeded before demo; health endpoint shows non-zero counts.
- **Must-pass plant+disease pairs are defined by stakeholder (OQ-1 / D-6)** — PM does **not** assume specific species in this PRD.
- Seeding priority follows stakeholder list once provided.
- Until OQ-1 is answered, Gate 3 / demo prep for SM-1 and Demo Acceptance Criteria remain blocked.

---

### 5.7 Legal & Trust (MVP)

**Approved disclaimer on Results — `[ACTIVE]` OQ-4.**

#### FR-18: Assistive disclaimer

`[ACTIVE]` **OQ-4** — exact copy:

> **This diagnosis is for general guidance only and isn't a substitute for professional horticultural advice.**

**Consequences (testable):**
- Disclaimer visible on Results (footer or below confidence note).
- Wording matches above (allow minor typography/casing; meaning unchanged).

---

## 6. Non-Goals (Explicit)

**Out of MVP — do not add opportunistically.**

- User accounts, auth, mobile diagnosis history
- Follow-up chat (post-MVP; synthesis stack already separable)
- Reminders, plant catalog, social, payments, ads
- Offline / on-device AI
- Admin CMS (CSV only)
- App Store / Play Store release
- Raw research export or browse
- Professional or certified diagnostic claims
- Planta-style feature parity
- Automated ingestion of unverified AI/user guesses into the Knowledge Base (no human verification)

---

## 7. MVP Scope

**In vs out — mirrors §6 for scope boundary.**

### 7.1 In Scope

| Area | What ships |
|------|------------|
| Mobile | Home, Loading, Results, Error — UPAVANA branding |
| API | `POST /api/diagnose`, health, admin seed |
| AI | NVIDIA vision; dual RAG; **OpenAI `gpt-4o` synthesis (active)** + NVIDIA text fallback; DeepSeek code present but inactive |
| Outcomes | Healthy; High/Medium KB-grounded issue; Low Unidentified Issue |
| Ops | CSV seeding, demo on device + Wi-Fi; NVIDIA fallback E2E validation |
| Trust | Approved disclaimer (FR-18); Product Principles |

### 7.2 Out of Scope

See §6. Each deferred item should name **why** (simplicity) and **what stays open** (e.g. `queries` table for future history / KB gap review).

---

## 8. Success Metrics

**Demo: sick KB hit + healthy + soft unidentified — stakeholder solo, happy path &lt;3 min. See also Demo Acceptance Criteria.**

### 8.1 Demo Acceptance Criteria (explicit, checkable)

**All of the following must pass for demo success:**

| # | Criterion |
|---|-----------|
| DAC-1 | All must-pass demo cases (OQ-1) produce the **expected diagnosis and recommended action** |
| DAC-2 | Healthy plant test images are **never** diagnosed with a disease |
| DAC-3 | When confidence is insufficient, system returns **Unidentified Issue** instead of fabricating a diagnosis |
| DAC-4 | Species-specific treatments are only recommended when plant identification confidence is **High** (plant-name match) |
| DAC-5 | Stakeholder can complete the **end-to-end flow without developer assistance** |
| DAC-6 | The **NVIDIA fallback path** satisfies the same acceptance criteria as the OpenAI primary path (deliberately tested E2E before demo) |

### 8.2 Primary success metrics

| ID | Metric | Validates |
|----|--------|-----------|
| **SM-1** | Stakeholder completes UJ-3 (must-pass photo → KB-grounded solution) without dev help, &lt; 3 min | FR-3,5,6,7,10; DAC-1, DAC-5 |
| **SM-2** | Healthy photo → positive Healthy UX | FR-11; DAC-2 |
| **SM-3** | Week-1 end-to-end on device | FR-1–10 minimum |

### 8.3 Secondary

| ID | Metric | Validates |
|----|--------|-----------|
| **SM-4** | Unknown plant → Unidentified Issue + guidance, no wrong-species KB disease | FR-6,7,12; DAC-3 |
| **SM-5** | Symptom-pattern retrieval surfaces relevant disease when plant misidentified (Medium tier) | FR-6,7 |
| **SM-6** | Timeout/network → Error + retry | FR-14,15 |
| **SM-7** | Disclaimer visible on Results | FR-18 |
| **SM-8** | UPAVANA visual consistency on four screens | FR-10, UX spec |
| **SM-9** | NVIDIA fallback E2E passes DAC-1–DAC-4 before stakeholder demo | FR-7; DAC-6 |

### Counter-metrics (do not optimize)

- **SM-C1:** Species count at expense of must-pass demo reliability
- **SM-C2:** Speed over grounding quality
- **SM-C3:** Feature count before demo works
- **SM-C4:** Inference cost over `gpt-4o` quality for MVP (cost opt deferred post-demo)

---

## 9. Critical Decision, Open Questions & Decision Log

**`[CRITICAL]` OQ-1 first — then roadmap opens — then settled decisions.**

### 9.0 Critical Decision Before Gate 3

**This is not a routine open question.** OQ-1 directly blocks Knowledge Base seeding, SM-1, Demo Acceptance Criteria, demo photo prep, and pipeline validation.

| ID | Question | Owner | Blocks |
|----|----------|-------|--------|
| **`[CRITICAL]` `[OPEN]` OQ-1 / D-6** | Which **3–5 plant+disease pairs** are **must-pass** for the stakeholder demo? | Stakeholder | FR-17 seeding priority; SM-1; DAC-1; demo script; Gate 3 readiness |

**Until answered:** Do not treat demo prep / SM-1 as complete; do not assume species in seeding plans.

### 9.1 `[OPEN]` — Non-blocking / roadmap

| ID | Question | Owner | Blocks |
|----|----------|-------|--------|
| **OQ-5** | Post-MVP priority: chat vs KB expansion vs UX polish | Stakeholder | Roadmap only |
| **OQ-6** | Who owns CSV updates during sprint when gaps found? | PM / Stakeholder | FR-16 operations |
| **OQ-8** | Does curated research have a specific regional/climate focus (e.g. Indian houseplants/conditions)? If yes, state as core differentiator. | Stakeholder | Product Vision copy; marketing |

### 9.2 Roadmap notes (explicitly non-MVP)

| Item | Notes |
|------|-------|
| KB gap mining from `queries` | Low/Medium diagnoses already persisted; post-MVP manual human review to prioritize research additions |
| Auto-ingest AI guesses into KB | **Forbidden** without human verification — preserves P3 |
| `gpt-4o-mini` / cost optimization | Deferred post-demo (D-8 locks `gpt-4o` for MVP) |
| Follow-up chat | Post-MVP |

### 9.3 `[ACTIVE]` / `[SUPERSEDED]` — Decision record

| ID | Decision | Date | Status |
|----|----------|------|--------|
| **D-1** | NVIDIA NIM for **vision only** | 2026-08-08 | `[ACTIVE]` |
| **D-2** | DeepSeek **primary** synthesis for MVP | 2026-08-08 | `[SUPERSEDED]` → D-7 |
| **D-3** | NVIDIA NIM **text** as synthesis **fallback** (paired with DeepSeek primary) | 2026-08-08 | `[SUPERSEDED]` → D-7 (fallback role unchanged; primary → OpenAI) |
| **D-4** | RAG: **plant-name + symptom-pattern** matching (implemented) | 2026-08-08 | `[ACTIVE]` |
| **D-5** | DeepSeek stays for MVP; do not use OpenAI as primary | 2026-08-08 | `[SUPERSEDED]` → D-7 |
| **D-7** | **OpenAI active primary** synthesis; NVIDIA text **fallback**; **DeepSeek code retained, not in active pipeline** | 2026-08-08 | `[ACTIVE]` |
| **D-8** | MVP synthesis model locked to **`gpt-4o`** (not `gpt-4o-mini`); quality over cost; cost opt deferred post-demo | 2026-08-11 | `[ACTIVE]` Resolves OQ-7 |
| **OQ-2** | AI chain: NVIDIA vision → DeepSeek synthesis → NVIDIA text fallback (AD-6) | 2026-08-01 | `[SUPERSEDED]` → D-7 / AD-6 |
| **OQ-3** | Unmatched plant: **soft Unidentified Issue** + general guidance | 2026-08-08 | `[ACTIVE]` |
| **OQ-4** | Disclaimer copy per FR-18 | 2026-08-08 | `[ACTIVE]` |
| **OQ-7** | `gpt-4o` vs `gpt-4o-mini` | 2026-08-11 | `[ACTIVE]` Resolved → D-8 (`gpt-4o`) |

#### 9.3.3 Runtime default DeepSeek (2026-08-27) — **not a D-7/D-8 reversal**

| | |
|---|---|
| **Changed** | Live `diagnosePlant()` default provider is **DeepSeek** via `ACTIVE_SYNTHESIS_PROVIDER` (default `deepseek`) |
| **Why** | OpenAI prepaid billing blocked; stakeholder previously allowed DeepSeek for MVP |
| **Unchanged** | D-7/D-8 remain the **target** stack (`gpt-4o` + json_schema). OpenAI methods stay in the repo. Flip env to `openai` when billing is approved. NVIDIA text remains fallback. |
| **Also** | NVIDIA text model default → `openai/gpt-oss-20b` (`meta/llama-3.1-8b-instruct` EOL 2026-08-26) |

#### 9.3.1 D-7 change log (2026-08-08)

| | |
|---|---|
| **Changed** | Active synthesis: DeepSeek → **OpenAI** |
| **Why** | DeepSeek slow in testing; output quality unsatisfactory; OpenAI free dev tier OK for MVP |
| **Unchanged** | D-1 vision; NVIDIA text fallback; D-4 dual RAG; `DiagnosisResult` contract |
| **DeepSeek** | `synthesizeDiagnosisWithDeepSeek`, `DEEPSEEK_*` **remain in codebase**; **not** on active MVP path |

#### 9.3.2 D-8 change log (2026-08-11)

| | |
|---|---|
| **Changed** | Synthesis model locked to **`gpt-4o`** for MVP |
| **Why** | Stakeholder: quality over inference cost for demo; mini deferred post-demo |
| **Resolves** | OQ-7 |

### 9.4 Architecture follow-up (Gate 3)

`[ACTIVE]` AD-6 updated for OpenAI synthesis. `[OPEN]` AD-3/AD-4 spine text still plant-only — **FR-6 / D-4 supersedes**; align at Gate 3. Also document confidence tiers and NVIDIA fallback E2E validation requirement.

---

## 10. Risk Register

**Consolidated risks + mitigations — scan before demo.**

| Risk | Mitigation |
|------|------------|
| Vision model misidentifies plant | Symptom-based RAG verification (symptom-pattern matching / Medium tier) |
| Weak Knowledge Base coverage | Must-pass seed dataset prioritization (`[CRITICAL]` OQ-1) |
| OpenAI unavailable | NVIDIA text fallback — **must be E2E tested before demo** (FR-7, DAC-6) |
| Low confidence diagnosis | Unidentified Issue flow with honest labeling (FR-12, P1/P5) |
| Confidently wrong species-specific advice | Species-specific treatments only at High tier (DAC-4) |
| Auto-polluting KB with AI guesses | Forbidden without human verification (§9.2 roadmap) |
| Demo-day failure | Pre-seed; pre-test must-pass photos; fallback images; DAC checklist |

---

## 11. Assumptions Index

- Demo on stable Wi-Fi; backend port 8080; device and laptop on same network.
- No user authentication in MVP.
- Research confidentiality: only Diagnosis fields cross API; no bulk KB export.
- `OPENAI_API_KEY` expected for active MVP synthesis; NVIDIA text fallback if OpenAI unavailable.
- DeepSeek env vars optional — not required for active MVP path (code retained only).
- Must-pass demo plants **not assumed** until OQ-1 answered.
- Image retention: no auto-delete in MVP (`uploads/` grows; policy post-demo).
- Regional focus of research (OQ-8) not assumed until stakeholder confirms.

---

## Cross-Cutting NFRs

**Timeouts, platform lock, UPAVANA tone — constraints for all FRs.**

| Area | Requirement |
|------|-------------|
| **Performance** | Mobile **180s** max; NVIDIA vision/text **25s**, 2 retries; OpenAI `gpt-4o` synthesis ≤ **60s** target (within 180s budget); loading UI non-blocking |
| **Security** | Secrets via env only; `.env` gitignored; no PII; admin seed not on public internet |
| **Reliability** | Pre-seed KB after OQ-1; pre-test photos; fallback images; NVIDIA fallback E2E before demo |
| **Platform** | Expo ~54, TS strict; Java 21, Spring Boot 4.0, MySQL 8, Flyway only — per `project-context.md` |
| **Aesthetic** | UPAVANA warm/organic; celebrate healthy; calm Unidentified Issue — `DESIGN.md`; Product Principles P1–P5 |

---

## Why Now

Pipeline works. Stakeholder needs demo within one month. Gate 1 approved; Gate 2 approved with minor changes (this revision). Gate 3 + demo prep wait on **OQ-1**.

---

## Constraints and Guardrails

| Area | Rule |
|------|------|
| **Cost** | NVIDIA NIM (vision + text fallback); OpenAI **`gpt-4o`** active synthesis (free dev tier OK); DeepSeek code only — no active API spend; mini deferred post-demo |
| **Confidentiality** | RAG in-system only; no raw KB export |
| **Stack** | Java Spring Boot + React Native + MySQL — no change without approval (`AGENTS.md`) |
| **BMAD** | No Gate 3+ new scope without approved PRD + architecture; Gate 3 blocked on OQ-1 |
| **Trust** | Product Principles P1–P5; no unverified KB auto-ingest |

---

## Appendix A: Requirement Traceability (Working Demo)

| Demo moment | Requirements |
|-------------|----------------|
| Take photo | FR-1, FR-3 |
| See loading tips | FR-4 |
| Get diagnosis | FR-5,6,7,9 |
| High-tier KB-grounded solution | FR-6,7, SM-1, DAC-1 |
| Medium-tier symptom match | FR-6,7, SM-5 |
| Healthy plant | FR-7,11, SM-2, DAC-2 |
| Low-tier Unidentified | FR-7,12, OQ-3, DAC-3 |
| Species-specific only if High | FR-7, DAC-4 |
| Disclaimer | FR-18, SM-7 |
| Error retry | FR-14,15 |
| NVIDIA fallback E2E | FR-7, DAC-6, SM-9 |
| Seed more plants later | FR-16, scalability notes |

---

## Appendix B: Pre-Demo Checklist (ops)

- [ ] `[CRITICAL]` OQ-1 answered — 3–5 must-pass plant+disease pairs named
- [ ] CSV seeded for must-pass pairs; `/api/health` shows expected counts
- [ ] Test photos for each must-pass pair verified on primary (OpenAI `gpt-4o`) path
- [ ] Healthy test images verified → Healthy, never disease
- [ ] Low-confidence photo verified → Unidentified Issue + locked copy
- [ ] **NVIDIA fallback path** forced/tested E2E — passes DAC-1–DAC-4
- [ ] Stakeholder dry-run without developer assistance (DAC-5)
- [ ] Disclaimer visible on Results
- [ ] `OPENAI_API_KEY` + `NVIDIA_API_KEY` set; backend `:8080`; device on same Wi-Fi
