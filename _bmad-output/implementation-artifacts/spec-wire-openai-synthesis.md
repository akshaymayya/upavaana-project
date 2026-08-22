---
title: 'Wire OpenAI gpt-4o into diagnosePlant() (stop DeepSeek on live path)'
type: 'feature'
created: '2026-08-22'
status: 'done'
baseline_commit: '0c87bfea1c09e59620e5177f303f25d27453d8b3'
review_loop_iteration: 0
context:
  - _bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Live `DiagnosisService.diagnosePlant()` still calls `synthesizeDiagnosisWithDeepSeek`. Docs (D-7, D-8, AD-6) require OpenAI `gpt-4o` as primary synthesis. That gap is the work.

**Approach:** Add OpenAI Chat Completions synthesis (`gpt-4o` + `json_schema` matching `DiagnosisResult`). Call it from `diagnosePlant()`. On missing/failing `OPENAI_API_KEY`, call existing NVIDIA text `synthesizeDiagnosis`. Leave DeepSeek methods and `DEEPSEEK_*` config in the repo unused on this path.

## Boundaries & Constraints

**Always:**
- Model `gpt-4o` only (not `gpt-4o-mini`).
- `response_format` JSON schema must match `DiagnosisResult` fields: `plant_name`, `disease_name`, `symptoms_matched`, `solution`, `confidence_note`, `is_healthy`.
- OpenAI read timeout ≤ 60s; NVIDIA text fallback stays 25s / 2 retries.
- Reuse existing synthesis prompts / candidate formatting (`PLANT_NAME_MATCH` / `SYMPTOM_PATTERN_MATCH`).
- `NVIDIA_API_KEY` still required for vision.
- Log clearly: OpenAI attempt vs NVIDIA fallback; never log API keys or image bytes.

**Ask First:**
- Changing RAG (AD-3/AD-4), vision provider, API JSON shape, or deleting DeepSeek code.

**Never:**
- Call DeepSeek from `diagnosePlant()`.
- Delete `synthesizeDiagnosisWithDeepSeek` or `DEEPSEEK_*`.
- OpenAI vision, `gpt-4o-mini`, new endpoints, mobile UI, RAG redesign.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Happy path | `OPENAI_API_KEY` set, OpenAI succeeds | Diagnosis JSON from OpenAI `gpt-4o` | N/A |
| Missing OpenAI key | `OPENAI_API_KEY` blank | NVIDIA text `synthesizeDiagnosis` | Log fallback; no DeepSeek |
| OpenAI HTTP/parse fail | Key set, call fails | NVIDIA text after OpenAI failure | Log failure; then NVIDIA 2×25s |
| NVIDIA fallback fail | OpenAI unavailable and NVIDIA text fails | Request fails as today (500) | No DeepSeek retry |
| Unit: diagnosePlant | Mocked OpenAI success | Never invokes DeepSeek method | Mockito verify no DeepSeek |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/plantdoctor/service/DiagnosisService.java` -- live path still calls DeepSeek (line ~69)
- `backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java` -- vision, NVIDIA text `synthesizeDiagnosis`, unused-on-live DeepSeek method
- `backend/src/main/java/com/plantdoctor/service/DiagnosisResult.java` -- snake_case record for schema
- `backend/src/main/java/com/plantdoctor/config/NvidiaProperties.java` -- NVIDIA + DeepSeek env bindings
- `backend/src/main/java/com/plantdoctor/PlantDoctorApiApplication.java` -- `@EnableConfigurationProperties`
- `backend/src/main/resources/application.yml` -- `${ENV}` placeholders
- `backend/.env.example` -- document `OPENAI_API_KEY` (happy path); `DEEPSEEK_API_KEY` optional
- `backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java` -- mocks DeepSeek today; switch to OpenAI method

## Tasks & Acceptance

**Execution:**
- [x] `backend/.../config/OpenAiProperties.java` (+ yml + enable) -- bind `OPENAI_API_KEY`, optional `OPENAI_BASE_URL` default `https://api.openai.com/v1`, model default `gpt-4o`
- [x] `backend/.../NvidiaClientService.java` -- add `synthesizeDiagnosisWithOpenAi`; RestTemplate read timeout 60s; missing key or failure → `synthesizeDiagnosis`; do not call DeepSeek from this method
- [x] `backend/.../DiagnosisService.java` -- synthesis step calls OpenAI method only
- [x] `backend/.env.example` -- `OPENAI_API_KEY` required for demo happy path; DeepSeek optional/unused
- [x] `backend/.../DiagnosisServiceTest.java` -- mock OpenAI method; verify DeepSeek never called on `diagnosePlant`

**Acceptance Criteria:**
- Given a successful diagnose, when logs are read, then OpenAI is called and DeepSeek is not.
- Given blank/invalid OpenAI key, when diagnose runs, then NVIDIA text synthesis runs and returns `DiagnosisResult`.
- Given `mvnw test`, when run from `backend/`, then existing tests pass.

## Spec Change Log

## Design Notes

OpenAI chat completions body should include `response_format.type = json_schema` with a strict object schema of the six `DiagnosisResult` properties (all required). Reuse the existing DeepSeek system/user prompt text so High/Medium/Low rules stay identical. After response, keep using `extractJson` + `ObjectMapper` into `DiagnosisResult`.

Default URL: `https://api.openai.com/v1/chat/completions`.

## Verification

**Commands:**
- `cd backend; .\\mvnw.cmd test` -- expected: BUILD SUCCESS, `DiagnosisServiceTest` green

**Manual checks (if keys available):**
- `POST /api/diagnose` with a photo; logs show OpenAI not DeepSeek; body matches `DiagnosisResult`.
- Temporarily unset/invalidate `OPENAI_API_KEY`; same upload uses NVIDIA text; valid JSON (DAC-6). If keys are absent in this environment, document that and still ship unit-test proof DeepSeek is unused.

## Suggested Review Order

**Live path**

- Entry point: diagnose now calls OpenAI, not DeepSeek.
  [`DiagnosisService.java:69`](../../backend/src/main/java/com/plantdoctor/service/DiagnosisService.java#L69)

- Primary synthesis + NVIDIA fallback; gpt-4o locked.
  [`NvidiaClientService.java:268`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L268)

- Blank or placeholder key skips OpenAI without a network call.
  [`NvidiaClientService.java:271`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L271)

- Incomplete JSON / truncated reply becomes NVIDIA fallback, not a half diagnosis.
  [`NvidiaClientService.java:340`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L340)

**Config**

- Env binding for OpenAI key and base URL.
  [`OpenAiProperties.java:5`](../../backend/src/main/java/com/plantdoctor/config/OpenAiProperties.java#L5)

- `openai:` block in application.yml.
  [`application.yml:35`](../../backend/src/main/resources/application.yml#L35)

**Tests**

- Unit test asserts live path never calls DeepSeek.
  [`DiagnosisServiceTest.java:69`](../../backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java#L69)

