---
title: 'Vision provider timeout strategy'
type: 'bugfix'
created: '2026-09-02'
status: 'done'
review_loop_iteration: 0
context: []
baseline_commit: 'ebecd2ad15e62e829925d9fadbddc173862c452a'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A shared 10s vision envelope (`remainingVisionMs`) shrinks NVIDIA’s RestTemplate read timeout to Gemini leftovers (~1.6s after an ~8.3s Gemini 503/timeout), so fallback is killed before a real NVIDIA response.

**Approach:** Give Gemini, NVIDIA Vision, and Groq independent bounded HTTP timeouts. Size the overall diagnose deadline so Gemini + NVIDIA + Groq can each complete one fair attempt. Do not cap NVIDIA by leftover vision time. Do not shrink NVIDIA to a sub-second leftover.

## Boundaries & Constraints

**Always:** Gemini primary → NVIDIA Vision fallback → Groq synthesis only. Gemini 503/429: one attempt, fail over immediately. NVIDIA gets its full configured read timeout on fallback. NVIDIA genuine timeout → `VISION_UNAVAILABLE`. Groq only after valid vision evidence. Keys/credentials unchanged. Diagnosis/evidence/pipeline rules unchanged.

**Ask First:** Adding another AI provider; changing Flyway/schema; raising any timeout above 15s read.

**Never:** Blind global timeout increases; Groq without vision; inventing diagnoses; OpenAI `image_url` NVIDIA body; optimizing for a 16s total if it starves NVIDIA.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Gemini 503 then NVIDIA OK | Gemini 503 after ~8s | NVIDIA called with full provider read timeout (~8s), then Groq | Log provider, status/kind, duration |
| Gemini timeout then NVIDIA timeout | Both vision fail | `VISION_UNAVAILABLE`, no Groq | NVIDIA uses full timeout, then stop |
| Shared vision envelope exhausted, total remaining | `remainingVisionMs` < 800, `remainingTotalMs` ample | NVIDIA fallback still runs | Do not skip on vision leftover |
| Overall deadline already elapsed | `remainingTotalMs` < 800 | Skip NVIDIA, `VISION_UNAVAILABLE` | No tiny 1.6s HTTP timeout |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/plantdoctor/service/DiagnosisLatencyBudget.java` -- `nvidiaFallbackReadTimeoutMs` currently `capTimeout(..., remainingVisionMs())`
- `backend/src/main/java/com/plantdoctor/service/RoutingPlantVisionClient.java` -- skips NVIDIA when `remainingVisionMs` < 800
- `backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java` -- per-call NVIDIA RestTemplate from leftover budget
- `backend/src/main/java/com/plantdoctor/service/GeminiPlantVisionClient.java` -- Gemini 4s/8s, 1 attempt
- `backend/src/main/java/com/plantdoctor/service/DiagnosisService.java` -- request budget + stage/total logs
- `backend/src/main/resources/application.yml` -- 16s/10s/5s/7s defaults
- `backend/src/test/java/com/plantdoctor/service/DiagnosisLatencyBudgetTest.java`
- `backend/src/test/java/com/plantdoctor/service/RoutingPlantVisionClientTest.java`

## Tasks & Acceptance

**Execution:**
- [x] `DiagnosisLatencyBudget.java` -- NVIDIA/Gemini timeouts are provider-owned; NVIDIA not capped by `remainingVisionMs`; overall default fits one Gemini + one NVIDIA + one Groq attempt
- [x] `RoutingPlantVisionClient.java` -- skip NVIDIA only when overall remaining time is exhausted, not leftover vision envelope
- [x] `NvidiaClientService.java` -- NVIDIA vision HTTP uses configured connect/read (4s/8s); log remainingTotalMs, provider, duration
- [x] `GeminiPlantVisionClient.java` -- do not shrink Gemini first call via leftover vision envelope; 503 still no retry
- [x] `application.yml` + `DiagnosisProperties` / `NvidiaProperties` -- document new defaults from observed times
- [x] `DiagnosisService.java` -- log each stage attempt duration and total request duration
- [x] `*Test.java` -- remaining vision leftover does not reduce NVIDIA read timeout; routing still falls back after Gemini 503

**Acceptance Criteria:**
- Given Gemini consumed ~8s of a 10s vision envelope, when fallback starts, then NVIDIA read timeout is the full NVIDIA bound (~8s), not ~1.6s
- Given Gemini 503, when routing, then NVIDIA is invoked promptly (no Gemini retry)
- Given NVIDIA times out at its own bound, when both vision providers fail, then `VISION_UNAVAILABLE` and Groq is not called
- Given vision succeeds, when synthesis runs, then Groq is the only synthesizer

## Spec Change Log

## Design Notes

Observed NVIDIA HTML-img + data-URI + 256 tokens: ~1.2–4.7s success. Gemini read already 8s. Groq read already 8s. Fair NVIDIA read = 8s (same order as observed successes, not 60s). Overall default = 8+8+8+4s slack = 28s. Mobile abort is 180s.

Root cause arithmetic: `remainingVisionMs = 10000 - ~8300 ≈ 1700`; `nvidiaFallbackReadTimeoutMs = min(5000, 1700)`.

## Verification

**Commands:**
- `backend/mvnw.cmd test` -- expected: all tests green
- Independent NVIDIA vision POST (production HTML-img body) -- HTTP 200 and duration logged
- Independent Groq JSON completion -- HTTP 200
- `POST /api/diagnose` after restart -- NVIDIA fallback not logged with ~1.6s read timeout after Gemini 503
