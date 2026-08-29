---
title: 'Fix synthesis reliability — NVIDIA NIM DeepSeek primary, Groq option, drop paid DeepSeek/OpenRouter'
type: 'bugfix'
created: '2026-08-29'
status: 'done'
baseline_commit: '45877ecf52f96b997877bff56a0cc5e66838d51d'
review_loop_iteration: 0
context:
  - _bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Official DeepSeek (`api.deepseek.com`) always 402s. OpenRouter is too rate-limited. Those attempts run first and waste time even though NVIDIA-hosted DeepSeek already produced valid JSON (~46s) on our NVIDIA quota.

**Approach:** When `ACTIVE_SYNTHESIS_PROVIDER=deepseek`, call NVIDIA NIM DeepSeek only (`NVIDIA_API_KEY`). Add `groq` as a config-selected OpenAI-compatible primary. NVIDIA vision-hosted text remains the last fallback for every primary. Keep `openai` as a switch, not the MVP default.

## Boundaries & Constraints

**Always:**
- `ACTIVE_SYNTHESIS_PROVIDER`: `deepseek` (default) | `groq` | `openai` (case-insensitive). Unknown → log error, treat as `deepseek`.
- `deepseek` primary = NVIDIA integrate `POST /chat/completions` with `DEEPSEEK_NIM_MODEL` (default `deepseek-ai/deepseek-v4-pro-0813`). Auth = `NVIDIA_API_KEY`. No HTTP to `api.deepseek.com` or `openrouter.ai`.
- `groq` primary = Groq OpenAI-compatible chat completions. Default model `openai/gpt-oss-120b` (`llama-3.3-70b-versatile` shut down 2026-08-16 for free/developer). Override with `GROQ_MODEL`. Missing/placeholder `GROQ_API_KEY` → NVIDIA text fallback (same as OpenAI missing key).
- Final fallback for all primaries: existing NVIDIA text `synthesizeDiagnosis` (`NVIDIA_TEXT_MODEL`).
- Reuse existing synthesis system/user prompts. Do not duplicate prompt stacks.
- Vision, RAG (`findCandidateDiseases`), `/api/diagnose` JSON, mobile: unchanged.

**Ask First:**
- Changing default `ACTIVE_SYNTHESIS_PROVIDER` away from `deepseek`.
- Using a Groq model other than Groq’s documented Llama-3.3 replacement (`openai/gpt-oss-120b` or `qwen/qwen3.6-27b`).

**Never:**
- Official DeepSeek or OpenRouter as any hop in the live chain.
- Hardcoding a single synthesizer (must stay env-switchable).
- Changing vision model, RAG scoring, API contract, or mobile screens.
- Reversing D-7/D-8: `openai` stays available when billing is approved.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| DeepSeek default | `ACTIVE_SYNTHESIS_PROVIDER` unset or `deepseek`; `NVIDIA_API_KEY` set | NIM DeepSeek synthesis; log shows NVIDIA-hosted model id | NIM fail → NVIDIA text |
| Groq selected | `ACTIVE_SYNTHESIS_PROVIDER=groq`; valid `GROQ_API_KEY` | Groq chat completions; DeepSeek NIM not called | Groq fail → NVIDIA text |
| Groq no key | provider `groq`; empty/placeholder key | NVIDIA text only | Warn in logs |
| OpenAI selected | provider `openai` | Existing OpenAI path | Unchanged |
| Unknown provider | `ACTIVE_SYNTHESIS_PROVIDER=foo` | Treated as `deepseek` | Error log |
| No NVIDIA key, deepseek | Missing `NVIDIA_API_KEY` | Cannot run NIM DeepSeek | Fall through to NVIDIA text (which also needs the key) or existing vision error path |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java` -- today posts to `DEEPSEEK_BASE_URL` first; NIM DeepSeek is secondary. Rewrite `synthesizeDiagnosisWithDeepSeek` to NIM-only. Add `synthesizeDiagnosisWithGroq`.
- `backend/src/main/java/com/plantdoctor/service/DiagnosisService.java` -- ternary OpenAI vs DeepSeek; add Groq branch.
- `backend/src/main/java/com/plantdoctor/config/DiagnosisProperties.java` -- resolve `groq`.
- `backend/src/main/java/com/plantdoctor/config/GroqProperties.java` -- new, mirror `OpenAiProperties`.
- `backend/src/main/java/com/plantdoctor/PlantDoctorApiApplication.java` -- enable Groq properties.
- `backend/src/main/resources/application.yml` -- groq block; default NIM model; comments that official DeepSeek/OpenRouter are unused.
- `backend/.env.example` -- create/update: `GROQ_API_KEY` optional; `DEEPSEEK_API_KEY` unused.
- `backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java` -- Groq vs DeepSeek vs OpenAI routing.

## Tasks & Acceptance

**Execution:**
- [x] `backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java` -- NIM DeepSeek is the only body of `synthesizeDiagnosisWithDeepSeek`; delete official/OpenRouter request loop; add Groq method (OpenAI-style JSON parse + NVIDIA text fallback)
- [x] `backend/src/main/java/com/plantdoctor/config/DiagnosisProperties.java` -- `PROVIDER_GROQ`; `useGroq()`
- [x] `backend/src/main/java/com/plantdoctor/config/GroqProperties.java` -- `GROQ_API_KEY`, `GROQ_BASE_URL` default `https://api.groq.com/openai/v1`, `GROQ_MODEL` default `openai/gpt-oss-120b`
- [x] `backend/src/main/java/com/plantdoctor/service/DiagnosisService.java` -- route groq / openai / deepseek
- [x] `backend/src/main/java/com/plantdoctor/PlantDoctorApiApplication.java` -- register Groq properties
- [x] `backend/src/main/resources/application.yml` -- groq + `DEEPSEEK_NIM_MODEL` default `deepseek-ai/deepseek-v4-pro-0813`; comment unused `DEEPSEEK_API_KEY`
- [x] `backend/.env.example` -- document keys; official DeepSeek unused
- [x] `backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java` -- Groq calls Groq not DeepSeek/OpenAI; default still DeepSeek not Groq

**Acceptance Criteria:**
- Given provider `deepseek`, when `diagnosePlant` runs, then only `synthesizeDiagnosisWithDeepSeek` is invoked (not Groq/OpenAI).
- Given that method, when it runs, then it does not POST to `api.deepseek.com` or OpenRouter.
- Given provider `groq` and a key, when `diagnosePlant` runs, then `synthesizeDiagnosisWithGroq` is called and DeepSeek/OpenAI methods are not.
- Given Groq HTTP or parse failure, when synthesis runs, then NVIDIA text `synthesizeDiagnosis` is used.

## Design Notes

Groq base URL is `https://api.groq.com/openai/v1` + `/chat/completions`. Prefer `response_format: json_object` if the model supports it; if the API 400s, retry once without it (same pattern as current OpenRouter retry). Do not use OpenAI `json_schema` unless Groq documents support for the chosen model.

Log `HTTP {ms}` for NIM DeepSeek and Groq so live timing can be compared.

Keep unused `DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` YAML keys so old `.env` files do not crash; they must not be read for HTTP.

## Verification

**Commands:**
- `cd backend; .\mvnw.cmd test` -- expected: all tests pass, including new Groq routing tests

**Manual checks (if no CLI):**
- Restart backend with env loaded. `ACTIVE_SYNTHESIS_PROVIDER=deepseek`: logs must show `Calling NVIDIA-hosted DeepSeek deepseek-ai/deepseek-v4-pro-0813` (or configured NIM id) and must not mention `api.deepseek.com` / OpenRouter. Record HTTP ms.
- With `GROQ_API_KEY` and `ACTIVE_SYNTHESIS_PROVIDER=groq`, diagnose a real photo; logs show Groq model + HTTP ms; valid `DiagnosisResult`.
- Do not change mobile; same Wi-Fi diagnose path as today.

## Suggested Review Order

**Provider switch**

- Entry: config selects openai, groq, or NVIDIA NIM DeepSeek.
  [`DiagnosisService.java:73`](../../backend/src/main/java/com/plantdoctor/service/DiagnosisService.java#L73)

- `groq` is a first-class resolved provider, not a DeepSeek alias.
  [`DiagnosisProperties.java:40`](../../backend/src/main/java/com/plantdoctor/config/DiagnosisProperties.java#L40)

**NIM-only DeepSeek**

- Official DeepSeek/OpenRouter loop is gone; NIM then NVIDIA text only.
  [`NvidiaClientService.java:441`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L441)

- Live call uses NVIDIA integrate + `NVIDIA_API_KEY`, default Pro model.
  [`NvidiaClientService.java:499`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L499)

**Groq primary**

- OpenAI-compatible Groq; JSON object, 400-only format retry, then NVIDIA text.
  [`NvidiaClientService.java:360`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L360)

**Config**

- Default NIM model and unused official DeepSeek keys documented in YAML.
  [`application.yml:39`](../../backend/src/main/resources/application.yml#L39)

- Groq env wiring; Llama 3.3 replacement is `openai/gpt-oss-120b`.
  [`application.yml:41`](../../backend/src/main/resources/application.yml#L41)

- Operator copy of env vars; `DEEPSEEK_API_KEY` marked unused.
  [`.env.example:1`](../../backend/.env.example#L1)

**Tests**

- Default still DeepSeek; Groq/OpenAI branches do not call the others.
  [`DiagnosisServiceTest.java:47`](../../backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java#L47)

