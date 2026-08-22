---
title: Plant Doctor Brief — Addendum
status: draft
created: 2026-08-03
parent: brief-plant-doctor-2026-08-01/brief.md
purpose: Technical and stakeholder context that supports the brief but does not belong in the 1–2 page core
---

# Brief Addendum — Plant Doctor

## Stakeholder AI Requirements (Verbatim Intent)

Sir directed:

- **Backend:** Java (Spring Boot)
- **Frontend:** React Native
- **Database:** MySQL
- **AI:** Cost-efficient OpenAI integration with dual-model strategy:
  - **`gpt-4o-mini`** — conversation, chat understanding, follow-up questions, high-frequency tasks (~90% traffic)
  - **`gpt-4o`** — complex synthesis with **strict structured outputs** (`response_format: json_schema`) (~10% traffic)
- **Development:** Investigate OpenAI free/low-cost options during build

## Brownfield Context (August 2026)

| Component | Status |
|-----------|--------|
| Spring Boot API + Flyway + MySQL | Built |
| CSV seed pipeline | Built |
| `POST /api/diagnose` | Built |
| RAG pipeline (vision → retrieve → synthesize) | Built |
| React Native screens (Upavana branding) | Rough draft |
| DeepSeek synthesis in code | **Interim** — not sir's target stack |
| OpenAI integration | **Not yet implemented** — pending Gate 3 |

## RAG Retrieval — Options for Sir (D-4)

| Mode | Behavior | Trade-off |
|------|----------|-----------|
| **Plant-name only** | Candidates only when vision text matches plant `name` or `common_names` | Safer against cross-species contamination; misses cases when vision misidentifies plant |
| **Symptom-pattern** | Also surface diseases whose symptoms/disease names match vision text across all plants | Better accuracy when plant ID is wrong; requires careful scoring thresholds |
| **Both** | Plant matches first; symptom-pattern as supplemental when plant match weak or absent | Recommended team position — **pending approval** |

Current codebase implements symptom-pattern fallback (August 2026 bug fix). Architecture spine (AD-3/AD-4) still documents plant-name-only — **requires architecture course correction after sir decides**.

## Proposed OpenAI Fallback Chain

1. Primary: `gpt-4o` + `DiagnosisResult` JSON schema
2. Fallback: `gpt-4o-mini` + same schema (rate limit / transient failure)
3. Not used: DeepSeek, NVIDIA text for synthesis

Vision remains NVIDIA-only unless sir directs otherwise.

## Demo Timeline (Unchanged)

| Milestone | Target |
|-----------|--------|
| Gate 1 brief approval | Week of 2026-08-03 |
| PRD + architecture approval | Before provider migration |
| Week 1 draft | End-to-end diagnose on device |
| Month 1 | Stakeholder demo |

## References

- Course correction doc: `_bmad-output/planning-artifacts/course-corrections/bmad-consent-workflow-and-ai-stack-2026-08-03.md`
- Prior brief draft: 2026-08-01 (AI provider sections superseded by this revision)
