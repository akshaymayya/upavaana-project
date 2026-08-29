---
title: 'Honor ACTIVE_SYNTHESIS_PROVIDER env and shorten NIM timeout'
type: 'bugfix'
created: '2026-08-29'
status: 'done'
route: 'one-shot'
---

# Honor ACTIVE_SYNTHESIS_PROVIDER env and shorten NIM timeout

## Intent

**Problem:** `ACTIVE_SYNTHESIS_PROVIDER=groq` could be set in the process environment while `diagnosePlant()` still logged `deepseek`, because routing used the YAML-bound field (often already resolved to the default). NVIDIA NIM DeepSeek then waited 90s before fallback.

**Approach:** Resolve provider from Spring `Environment` key `ACTIVE_SYNTHESIS_PROVIDER` first (strip quotes), then the bound property. Cut NIM DeepSeek HTTP read timeout to 30s.

## Suggested Review Order

**Env wins over YAML default**

- Prefer OS/env `ACTIVE_SYNTHESIS_PROVIDER` even if the bound field is `deepseek`.
  [`DiagnosisProperties.java:45`](../../backend/src/main/java/com/plantdoctor/config/DiagnosisProperties.java#L45)

- Inject `Environment` when the properties bean is created.
  [`DiagnosisPropertiesConfiguration.java:12`](../../backend/src/main/java/com/plantdoctor/config/DiagnosisPropertiesConfiguration.java#L12)

**Fail NIM faster**

- 30s read timeout so fallback is not blocked for 90s.
  [`NvidiaClientService.java:55`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L55)

**Proof**

- Env `groq` + bound `deepseek` still calls Groq, not DeepSeek.
  [`DiagnosisServiceTest.java:75`](../../backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java#L75)
