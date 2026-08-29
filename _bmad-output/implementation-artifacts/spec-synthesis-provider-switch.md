---
title: 'Config-togglable synthesis provider (default DeepSeek) + DeepSeek latency'
type: 'feature'
created: '2026-08-22'
status: 'done'
baseline_commit: '45877ecf52f96b997877bff56a0cc5e66838d51d'
review_loop_iteration: 0
context:
  - _bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** OpenAI billing is blocked ($5 prepaid). Demo still needs a working synthesizer. Stakeholder already allowed DeepSeek for MVP if speed/quality are OK. Switching providers must not require another code edit. Past DeepSeek calls were 60–90s+, eating the 180s budget.

**Approach:** Add `ACTIVE_SYNTHESIS_PROVIDER` (`deepseek` | `openai`, default **`deepseek`**). `diagnosePlant()` calls the matching existing method. NVIDIA text stays fallback on missing key or primary failure. This does **not** reverse D-7/D-8 — OpenAI remains the intended target; flip the env var when billing is approved. Latency: keep model **`deepseek-v4-pro`**; send V4 **`thinking: disabled`** (V4 defaults thinking ON — likely cause of 60–90s); log prompt size + HTTP ms + usage tokens if present; cap plant-name candidates like symptom matches (see tradeoff below). Do not switch to `deepseek-v4-flash` in this story.

## Boundaries & Constraints

**Always:**
- Default provider `deepseek`. Valid values: `deepseek`, `openai` (case-insensitive). Unknown value → log error and treat as `deepseek`.
- NVIDIA text fallback unchanged for both primaries.
- Keep both `synthesizeDiagnosisWithDeepSeek` and `synthesizeDiagnosisWithOpenAi`. No duplicate prompt stacks.
- Confidence-tier rules / synthesis system prompt unchanged except thinking flag on the DeepSeek HTTP body.
- Docs: AD-6 + PRD decision log note this is a **temporary runtime default**, reversible, not a D-7/D-8 reversal.
- Unit tests cover both provider branches and that the other method is not called.

**Ask First:**
- Changing default to `openai`.
- Switching DeepSeek model to `deepseek-v4-flash` (faster, likely weaker).
- Raising candidate caps above the values in Design Notes.
- Re-enabling DeepSeek thinking mode.

**Never:**
- Hardcode one provider again.
- Delete OpenAI or DeepSeek methods.
- Change vision, RAG scoring formula, or mobile timeout.
- Silent quality downgrade to Flash.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Default | Env unset | DeepSeek primary | NVIDIA if no `DEEPSEEK_API_KEY` |
| OpenAI mode | `ACTIVE_SYNTHESIS_PROVIDER=openai` | OpenAI primary | NVIDIA if no/fail OpenAI |
| DeepSeek mode | `=deepseek` | DeepSeek primary | NVIDIA if no/fail DeepSeek |
| Bad value | `=claude` | Log + DeepSeek | Same as default |
| DeepSeek HTTP fail | Key set, call fails | NVIDIA text | No OpenAI unless provider is openai |
| Prompt bound | Many plant-name hits | At most cap plant-name + 3 symptom | Log counts before/after cap |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/plantdoctor/service/DiagnosisService.java` -- hardcoded OpenAI call; unbounded plant-name matches
- `backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java` -- DeepSeek: 1 attempt, 90s read, model from yml `deepseek-v4-pro`, no thinking flag (V4 default = thinking)
- `backend/src/main/java/com/plantdoctor/config/NvidiaProperties.java` -- DeepSeek key/url/model
- `backend/src/main/java/com/plantdoctor/config/OpenAiProperties.java` -- OpenAI key
- `backend/src/main/resources/application.yml`
- `backend/.env.example`
- `backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java`
- `_bmad-output/planning-artifacts/architecture/.../ARCHITECTURE-SPINE.md` -- AD-6
- `_bmad-output/planning-artifacts/prds/prd-plant-doctor-2026-08-01/prd.md` -- §9.3

## Investigation (pre-impl)

1. **Retries:** DeepSeek `maxAttempts = 1`. Not retry-stacking. Leave at 1.
2. **Model:** `deepseek-v4-pro`. V4 **thinking is on by default** — extra reasoning tokens, high latency. Flash is faster/cheaper; **not** selected here (quality risk). **Fix:** keep Pro, set `thinking: { type: "disabled" }` on the request. Tradeoff: less chain-of-thought, faster JSON — flagging, not hiding.
3. **Prompt size:** Symptom matches already capped at 3. **Plant-name matches are uncapped** (every matching disease row). Not a full KB dump, but can still be large. **Fix:** cap plant-name candidates at **5** (keep all if ≤5; if more, keep first 5 in current scan order). Tradeoff: rare extra plant-name hits dropped. Log `promptChars` + candidate counts.
4. **Network vs model:** Today we log total HTTP ms only. Add: candidate count, prompt character length, HTTP duration, and `usage` prompt/completion/reasoning tokens if the JSON has them. No live before/after numbers in this environment until a real call is made — report those from logs after first demo run.

## Tasks & Acceptance

**Execution:**
- [x] New `@ConfigurationProperties` (e.g. `app.diagnosis` / `DiagnosisProperties`) -- `activeSynthesisProvider` from `ACTIVE_SYNTHESIS_PROVIDER`, default `deepseek`
- [x] `DiagnosisService.java` -- branch to DeepSeek vs OpenAI; inject properties; cap plant-name candidates at 5; log counts
- [x] `NvidiaClientService.java` -- DeepSeek body: `thinking.type=disabled`; log promptChars + duration + usage tokens; keep 1 attempt / 90s
- [x] `application.yml` + `.env.example` -- document switch and DeepSeek vs OpenAI keys
- [x] `DiagnosisServiceTest.java` -- both provider values; verify unused method `never()`
- [x] AD-6 + PRD decision log -- temporary default DeepSeek; D-7/D-8 still target; env switch

**Acceptance Criteria:**
- Given default env, when `diagnosePlant` runs, then DeepSeek method is called and OpenAI method is not.
- Given `ACTIVE_SYNTHESIS_PROVIDER=openai`, when `diagnosePlant` runs, then OpenAI method is called and DeepSeek is not.
- Given DeepSeek primary fails or key missing, when synthesis runs, then NVIDIA `synthesizeDiagnosis` is used.
- Given many plant-name matches, when retrieval runs, then at most 5 plant-name candidates are sent.
- Given AD-6/PRD, when read, then they state this is a reversible config default, not a D-7/D-8 reversal.

## Spec Change Log

## Design Notes

DeepSeek request addition (do not change prompts):

```json
"thinking": { "type": "disabled" }
```

Plant-name cap = 5; symptom cap stays 3. Log line example: `DeepSeek HTTP 1840ms promptChars=4200 candidates=4 usage prompt=1100 completion=280`.

## Verification

**Commands:**
- `cd backend; .\\mvnw.cmd test` -- BUILD SUCCESS

**Manual (when DeepSeek key available):**
- One diagnose with default provider; confirm log provider=deepseek, thinking disabled, HTTP ms (this is the “after”). We have no stored “before” ms in-repo — previous 60–90s is the baseline from earlier testing.

## Suggested Review Order

**Runtime routing**

- Default DeepSeek vs OpenAI is decided here, then the existing method is called.
  [`DiagnosisService.java:72`](../../backend/src/main/java/com/plantdoctor/service/DiagnosisService.java#L72)

- Env value is normalized; unknown values log and use DeepSeek.
  [`DiagnosisProperties.java:30`](../../backend/src/main/java/com/plantdoctor/config/DiagnosisProperties.java#L30)

**DeepSeek latency + fallback**

- V4 thinking disabled; prompt size and usage logged; NVIDIA text if the call fails.
  [`NvidiaClientService.java:371`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L371)

- Plant-name RAG capped at 5 in scan order (symptom cap still 3).
  [`DiagnosisService.java:133`](../../backend/src/main/java/com/plantdoctor/service/DiagnosisService.java#L133)

**Config and docs**

- Single env switch; NVIDIA text default is gpt-oss-20b after 8b EOL.
  [`application.yml:25`](../../backend/src/main/resources/application.yml#L25)

- Temporary DeepSeek default; D-7/D-8 still the target.
  [`ARCHITECTURE-SPINE.md:130`](../planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md#L130)

**Tests**

- Both provider branches and the plant-name cap.
  [`DiagnosisServiceTest.java:46`](../../backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java#L46)

