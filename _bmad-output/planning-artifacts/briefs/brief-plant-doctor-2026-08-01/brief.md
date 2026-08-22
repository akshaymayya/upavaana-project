---
title: Plant Doctor — Product Brief
status: draft
created: 2026-08-01
updated: 2026-08-03
author: Mary (Business Analyst)
prepared_for: Aksha → Supervisor (sir) approval
approval_gate: Gate 1 — Brief must be APPROVED before PRD
next_artifact: PRD (after sir sign-off)
supersedes_sections:
  - AI provider strategy (2026-08-01 NVIDIA-only wording)
related:
  - _bmad-output/planning-artifacts/course-corrections/bmad-consent-workflow-and-ai-stack-2026-08-03.md
---

# Product Brief: Plant Doctor

## Executive Summary

Plant Doctor is a mobile app that helps indoor plant owners understand what is wrong with their houseplants and what to do about it. A user photographs a plant; the app returns a structured diagnosis — plant identity (best guess), detected issue, matched symptoms, and recommended treatment — **grounded in the team's proprietary plant-health research** via retrieval-augmented generation (RAG), not generic AI guesswork alone.

The product targets casual home gardeners who notice something is off but lack expertise to name diseases or pests. Competitors like Planta offer broad plant-care ecosystems; Plant Doctor deliberately scopes to a **focused MVP**: photo-in, diagnosis-out.

This is a **brownfield** effort. Backend (Spring Boot + MySQL), CSV seeding, `/api/diagnose`, and rough React Native screens exist. Development proceeds under **BMAD methodology with supervisor consent at every planning stage** before the next artifact or major implementation change.

**Stakeholder-directed technology stack:** Java (Spring Boot), React Native, MySQL, and a **cost-efficient dual-model OpenAI strategy** for text AI — with **NVIDIA NIM retained for vision/image analysis** (team proposal, pending sir approval).

---

## Governance — BMAD Stage Gates

Sir requires **explicit approval before each phase**. No PRD, architecture, or provider migration proceeds without sign-off.

| Gate | Artifact | Owner | Approval question |
|------|----------|-------|-------------------|
| **1** | This brief | Analyst (Mary) | *Is this the problem and scope we are solving?* |
| **2** | PRD | PM (John) | *Are requirements correct?* |
| **3** | Architecture | Architect (Winston) | *Is this the technical contract?* |
| **4** | UX specs | Designer (Sally) | *Is this the demo experience?* |
| **5** | Epics & stories | PM | *Is this the build order?* |
| **6+** | Dev / demo | Dev | Checkpoint per story |

**Reply required:** `APPROVED` or listed changes before the next gate.

---

## Problem Statement

### The pain

Houseplant owners face:

1. **Uncertainty** — Is it watering, light, disease, or pests?
2. **Information overload** — Web search returns conflicting, non-personalized advice.
3. **Generic AI risk** — LLMs confidently misidentify plants or prescribe wrong-species treatments.
4. **No clear "you're fine" outcome** — Healthy plants get awkward empty states.

### Cost of the status quo

- Plants decline from well-intentioned mistreatment.
- Users abandon apps that feel inaccurate.
- Proprietary research knowledge remains unused without a product front door.

### Why now

The diagnosis pipeline is technically proven. Stakeholders require a **demo-ready MVP within one month**. Planning runs **in parallel** with delivery — but **not ahead of sir's approval** at each gate.

---

## Target Users

### Primary: Casual indoor plant owners

| Attribute | Description |
|-----------|-------------|
| **Who** | Adults with houseplants; beginners to intermediate |
| **Context** | Notices visual symptoms; wants a quick answer without long articles |
| **Jobs to be done** | "What is wrong?" → "What should I do this week?" |
| **Success** | Actionable, plain-language advice; clear "Healthy" when appropriate |

### Out of scope (MVP)

Commercial nurseries, agricultural operations, professional diagnosticians.

---

## The Solution (MVP)

A React Native / Expo app connected to a Spring Boot API and MySQL knowledge base:

1. User **takes or uploads a photo**.
2. **NVIDIA NIM vision model** analyzes the image → text description (morphology, plant guess, symptoms).
3. System **retrieves candidate diseases** from proprietary `plants` / `diseases` tables (RAG).
4. **OpenAI `gpt-4o`** with **strict JSON schema** synthesizes the final structured diagnosis, grounded in retrieved records where applicable.
5. App displays: **plant name**, **issue** (or "Healthy"), **symptoms matched**, **solution**, **confidence note**.

**Future (post-MVP):** Follow-up chat using **`gpt-4o-mini`** (~90% of ongoing AI traffic).

**Confidentiality:** Research content grounds AI responses inside the system only. Raw research must not be exposed outside structured DB usage.

---

## AI Strategy (Stakeholder + Team Proposal)

### Sir's dual-model OpenAI direction

| Share | Use case | Model |
|-------|----------|-------|
| ~90% | Chat, follow-ups, high-frequency tasks | `gpt-4o-mini` |
| ~10% | Diagnosis synthesis, structured output | `gpt-4o` + `json_schema` |

**Development:** Use OpenAI free/low-cost dev options where available during build.

### Proposed pipeline (pending Gate 1 approval)

```
Photo → NVIDIA Vision → RAG (MySQL) → OpenAI gpt-4o (JSON) → Mobile
```

| Stage | Provider | Notes |
|-------|----------|-------|
| Vision | NVIDIA NIM | Keep current implementation |
| Retrieval | MySQL | RAG over plants/diseases — **architecture unchanged** |
| Synthesis | OpenAI `gpt-4o` | Replaces interim DeepSeek path in codebase |
| Fallback | OpenAI `gpt-4o-mini` | On primary failure (proposed) |
| Chat (later) | OpenAI `gpt-4o-mini` | Phase 5 |

**Remove after approval:** DeepSeek synthesis from MVP path.

---

## What Makes This Different

| Differentiator | Notes |
|----------------|-------|
| **RAG over proprietary research** | Answers anchored to curated disease/pest knowledge |
| **Focused MVP** | Diagnosis only — not full plant-care platform |
| **Cost-conscious AI** | Mini model for volume; flagship only for structured synthesis |
| **Honest outcomes** | Healthy, unidentified, and confidence states — no forced false positives |

**Limitation:** Differentiation depends on **KB coverage** and **retrieval quality**.

---

## MVP Scope

### In scope

| Area | Included |
|------|----------|
| Core flow | Photo → loading → results or error |
| Diagnosis output | Plant name, disease, symptoms, solution, confidence, `is_healthy` |
| Backend | `POST /api/diagnose`, health check, CSV seeding |
| Knowledge base | `plants` + `diseases` from proprietary CSV |
| RAG | Vision → retrieve → synthesize |
| Demo | End-to-end on device against running backend |

### Out of scope (MVP)

Follow-up chat, user accounts, plant library/reminders, offline mode, payments, admin CMS, store release, professional accuracy claims.

---

## Success Criteria (Demo)

**Must-have (blockers):**

1. Symptomatic plant photo → structured diagnosis within acceptable wait (target: under 3 minutes).
2. Healthy plant → explicit positive "Healthy" outcome.
3. Known KB plant+disease → solution reflects retrieved research.
4. Unknown plant/issue → "Unidentified Issue" with generic guidance — no fabricated cross-species match.
5. Stable demo environment (backend, DB seeded, mobile connected).
6. No raw research corpus exposed.

**Should-have:** Polished UX, repeatability, week-1 working draft.

---

## Key Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Timeline vs. UX quality | High | Lock MVP scope; polish core screens only |
| KB coverage gaps | High | Prioritize demo-script plants in CSV seeding |
| AI latency | Medium | Stay within mobile 180s budget; stable demo network |
| Vision misidentification | Medium | Morphology-first prompts; confidence note; symptom-pattern RAG fallback |
| Provider migration | Medium | Gate 3 architecture before swapping DeepSeek → OpenAI |
| Scope creep | Medium | This brief is the boundary document |
| Demo-day failure | Medium | Pre-test known demo photos against seeded KB |

---

## Open Questions for Sir (Gate 1)

| ID | Question |
|----|----------|
| **D-1** | Approve **NVIDIA NIM for vision only**? |
| **D-2** | Approve **OpenAI `gpt-4o` + JSON schema** for diagnosis synthesis? |
| **D-3** | Approve **`gpt-4o-mini` fallback** on synthesis failure? |
| **D-4** | RAG retrieval: **plant-name only**, **symptom-pattern across plants**, or **both**? |
| **D-5** | Remove **DeepSeek** entirely from MVP? |
| **D-6** | Which **3–5 plant/disease pairs** must work flawlessly in the demo? |

---

## Recommended Next Steps

1. **Sir reviews this brief** — reply `APPROVED` or changes (Gate 1).
2. **PRD** — `bmad-prd` update after Gate 1; resolve D-1–D-6 as requirements.
3. **Architecture** — `bmad-architecture` update after Gate 2; ratify AI provider chain (AD-6).
4. **Implementation** — OpenAI synthesis swap as a discrete dev story **after Gate 3 only**.

---

## Vision (Post-MVP)

Expand KB coverage, add `gpt-4o-mini` follow-up chat, and optionally care features. Long-term moat: **curated grounding data** + frictionless photo diagnosis — not Planta feature parity.
