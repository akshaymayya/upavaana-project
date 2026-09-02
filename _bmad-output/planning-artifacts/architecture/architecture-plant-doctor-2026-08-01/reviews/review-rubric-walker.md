# Rubric walker — Architecture Spine

**Artifact:** `ARCHITECTURE-SPINE.md` (Plant Doctor MVP)  
**Reviewed:** 2026-08-29  
**Lens:** Good-spine checklist (feature altitude) + mandated FR-19 placement  
**Sources used:** the spine file only (no PRD, picture, or codebase re-open)

## Gate verdict

**pass-with-notes**

The spine does the job a builder contract must do: it names the live forks (OpenAI target vs DeepSeek runtime; FR-19 contract vs code lag), binds FR-1–FR-19, and puts `treatment_type` / `treatment_steps` in **both AD-2 and AD-7**. It does not silently overwrite brownfield with a pretend-clean stack. Residual risk is leftover Groq, demo-key / diagram drift vs DeepSeek, and a few Rules that are not fully checkable (unbounded `MAX_SYMPTOM_CANDIDATES`, dual MatchType labels). Deferred rows record implementation lag; they do not hide a second product decision — but they can still be misread as “don’t implement yet” against ADOPTED ADs.

## Checklist

| Criterion | Result | Notes |
| --- | --- | --- |
| Fixes real divergence for the level below; misses none | **Pass with notes** | Client/API, pipeline order, dual RAG, empty-retrieval, secrets, Flyway, no-auth, image names, KB leak, and FR-19 JSON shape are locked. Leftover **Groq** path and **MatchType vs prompt labels** can still split two implementers. |
| Every AD Rule is enforceable and prevents its stated divergence | **Pass with notes** | Most Rules are testable. Weak: AD-3 cap unnamed; AD-3 enum vs prompt strings; AD-7 “must not default every issue to…” is judgment; AD-11 demo keys omit DeepSeek. |
| Deferred does not hide a divergence two units could take | **Pass with notes** | OpenAI wiring and FR-19 code lag are named in ADs *and* Deferred. ADOPTED wins if read correctly; a unit that treats Deferred as the live contract will lag the other. |
| Named tech verified-current | **Not verified (spine-only)** | Versions are pinned and internally consistent (incl. llama-3.1-8b EOL note). This pass did not check vendors. **Groq** is named in AD-7 but absent from Stack/AD-6 — treat as stale, not current. |
| Ratifies brownfield; does not silently contradict it | **Pass with notes** | AD-6 + capability map + Deferred are honest about DeepSeek-on-the-wire. Contradictions that *are* silent: AD-11 demo keys, demo mermaid (OpenAI only), Groq in AD-7. |
| Spec capabilities covered (FR binds + map) | **Pass** | `binds` FR-1–FR-19; capability table maps each cluster. **FR-19 `treatment_type` / `treatment_steps` are in AD-2 and AD-7** (and the map row). |
| Inherited parent spine | **N/A** | Feature-altitude spine; no parent ADs declared. |
| Altitude dimensions decided, deferred, or OQ | **Pass** | Local demo env decided; CI/prod deferred; auth, chat, vector RAG, GDPR, admin lock deferred; OQ-1 seed pairs and OQ-8 climate copy remain open. |

## FR-19 (mandated)

| Placement | Present? | What is locked |
| --- | --- | --- |
| **AD-2** | Yes | Success body includes `treatment_type`, optional `treatment_steps`; enum `single_action` \| `care_plan`; steps 3–5 `{ day, action }` only for `care_plan`; omit / `null` / `[]` for `single_action`; compat for lagging producers; older clients ignore unknown keys. |
| **AD-7** | Yes | Model chooses type from the problem; no blanket checklist and no blanket single-action; schema/prompts on every synthesis path; nullable `treatment_steps` under strict `json_schema`; persist full JSON in `queries.result_json`. |
| Capability map | Yes | “Treatment shape (FR-19) → `DiagnosisResult` + schema/prompts + Results render → AD-2, AD-7”. |
| Deferred | Yes (lag, not a rival rule) | Code still omits the fields as of 2026-08-29; spine contract is ADOPTED. |

FR-19 treatment is **not** missing from the two ADs that must own it.

---

## Findings

### High

#### H1 — AD-7 still names Groq; Stack and AD-6 do not

- **Where:** AD-7 “Schema / prompts”: “OpenAI, **Groq**, NVIDIA-hosted DeepSeek, NVIDIA text fallback”.
- **Why it matters:** Builders copy the prompt-path list. Groq is not in Stack, AD-6, paradigm, or deployment. That is a silent extra provider — brownfield contradiction and an unenforceable Rule.
- **Prevents?** AD-7’s Prevents is unparseable output / wrong treatment shape, not “keep Groq in sync”. The Groq clause does not prevent a divergence; it *creates* one.
- **Disposition:** **autofix** — drop Groq from AD-7 or add a real AD + Stack pin if Groq is still in repo.

#### H2 — Demo operations still describe OpenAI-only while runtime default is DeepSeek

- **Where:** AD-11 “Demo needs `NVIDIA_API_KEY` + `OPENAI_API_KEY`”; deployment mermaid `Laptop --> OAI` only; Stack lists DeepSeek as runtime default.
- **Why it matters:** A demo setup from AD-11 + diagram will skip `DEEPSEEK_API_KEY` and expect OpenAI on the happy path. AD-6 already requires DeepSeek when `ACTIVE_SYNTHESIS_PROVIDER=deepseek`. Two runbooks, one spine.
- **Brownfield:** This *silently* contradicts the documented current default (unlike AD-6, which is explicit).
- **Disposition:** **autofix** — AD-11 and the demo diagram must list DeepSeek (and `DEEPSEEK_API_KEY`) for the current default; keep OpenAI as the D-7/D-8 target.

### Medium

#### M1 — AD-3 Rule is not fully enforceable

- **Where:** `MAX_SYMPTOM_CANDIDATES` with no integer; `MatchType.PLANT_NAME` / `SYMPTOM_PATTERN` vs prompt `PLANT_NAME_MATCH` / `SYMPTOM_PATTERN_MATCH`.
- **Why it matters:** Two retrieval implementations can cap differently and tag prompts differently; synthesis tiers in AD-7 key off those labels.
- **Disposition:** **autofix** — pin the cap; one canonical match-type string (Java enum ↔ prompt).

#### M2 — Deferred FR-19 / OpenAI rows can be read as competing contracts

- **Where:** Deferred “Wire OpenAI on `diagnosePlant()`”; “Ship FR-19 fields in code”.
- **Why it is not a hidden product fork:** AD-6 already states target vs runtime; AD-2/AD-7 already ADOPT FR-19. Deferred records **code lag**, which is the right brownfield pattern.
- **Residual:** A backend story that implements only Deferred-as-backlog and a mobile story that implements AD-2 Compat (“fields optional until FR-19 UI”) can ship a lagging producer + a parser that never reads the new keys — the compat clause allows it, but two units can stay diverged longer than intended.
- **Disposition:** **discuss** (optional one-liner in Deferred: “AD-2/AD-7 are the contract; this row is current-code debt, not a waiver”).

#### M3 — AD-2 vs AD-7 allow three on-the-wire shapes for “no steps”

- **Where:** AD-2 omit / `null` / `[]`; AD-7 omit/empty + nullable array for strict schema.
- **Why it matters:** Prevents “two clients inventing different treatment JSON” only if every consumer accepts all three. Easy for one parser to require `null` and another `[]`.
- **Disposition:** **autofix** or **discuss** — pick one canonical absence (`null` vs omit) and treat the others as compat-only.

### Low

#### L1 — AD-7 “must not default every issue to care_plan or single_action”

Subjective; cannot be unit-tested beyond schema + spot checks. Acceptable for a model-choice Rule; do not pretend it is mechanically enforceable.

#### L2 — AD-6 is two Rules in one AD (target D-7/D-8 vs runtime DeepSeek)

It **does** fix the divergence by making the env var the switch and stating it is not a reversal of D-7/D-8. Dense, but not a silent contradiction. Keep; do not split unless authors want easier linting.

#### L3 — Named-tech currency

Spine-only pass: Java 21, Spring Boot 4.0.0, Expo ~54, RN 0.81.5, `gpt-4o`, `deepseek-v4-pro`, NIM vision + `gpt-oss-20b` fallback. No vendor check in this review. Groq (H1) is the only name that looks **stale inside the document**.

---

## What is solid (do not churn)

- **AD-1, AD-5, AD-8–AD-10, AD-12–AD-14:** Rules match Prevents; builders can fail a PR against them.
- **AD-2 + AD-7 FR-19:** Contract, enum, 3–5 steps, compat for lagging producers, ignoreUnknown, persist `result_json`, all synthesis prompts — this is the divergence FR-19 was going to cause, and it is locked in the two ADs that own API + synthesis.
- **AD-4:** Empty retrieval only when neither match; Unidentified Issue is not an error — prevents the “dump whole KB” fork.
- **AD-6 OpenAI vs DeepSeek:** Named, dated, env-gated; Deferred “wire OpenAI” does not hide a second official provider.
- **Capability map** includes FR-19 and points at AD-2 and AD-7.
- **Altitude envelope:** local demo decided; prod/CI/auth/chat/vector/GDPR deferred; OQ-1 and OQ-8 remain open and non-blocking for spine *shape*.

## Suggested gate actions

| ID | Action | Owner |
| --- | --- | --- |
| H1 | Remove Groq from AD-7 (or fully adopt it) | autofix |
| H2 | Align AD-11 + demo diagram with DeepSeek default | autofix |
| M1 | Pin `MAX_SYMPTOM_CANDIDATES`; unify match-type names | autofix |
| M2 | One sentence: Deferred = code debt, ADs = contract | discuss |
| M3 | Canonical empty `treatment_steps` | autofix or discuss |

**Critical findings:** none.  
**Plus 3 low items** in this file (L1–L3).
