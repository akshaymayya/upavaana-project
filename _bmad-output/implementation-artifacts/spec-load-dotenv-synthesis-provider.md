---
title: 'Load .env for synthesis provider and cut NIM timeout'
type: 'bugfix'
created: '2026-08-29'
status: 'done'
route: 'one-shot'
---

# Load .env for synthesis provider and cut NIM timeout

## Intent

**Problem:** `ACTIVE_SYNTHESIS_PROVIDER=groq` in `.env` (confirmed with echo/Get-Content) never reached the JVM, so diagnose still used DeepSeek. NVIDIA NIM DeepSeek also waited 90s before fallback.

**Approach:** Load the first existing `.env` into system properties at process start (do not override real OS env). Resolve provider from Environment, then process env/sysprop, then YAML. NIM DeepSeek HTTP read timeout is 25s.

## Suggested Review Order

**Why echo lied**

- `.env` is copied into system properties before the Spring context starts.
  [`PlantDoctorApiApplication.java:21`](../../backend/src/main/java/com/plantdoctor/PlantDoctorApiApplication.java#L21)

- Parser skips comments and does not overwrite an existing OS env var.
  [`EnvFileLoader.java:51`](../../backend/src/main/java/com/plantdoctor/config/EnvFileLoader.java#L51)

**Routing**

- `groq` wins even when the bound YAML field is still `deepseek`.
  [`DiagnosisProperties.java:43`](../../backend/src/main/java/com/plantdoctor/config/DiagnosisProperties.java#L43)

- Diagnose logs os.env / sysprop / bound so a mismatch is visible.
  [`DiagnosisService.java:73`](../../backend/src/main/java/com/plantdoctor/service/DiagnosisService.java#L73)

**Timeout**

- NIM DeepSeek read timeout is 25s, not 90s.
  [`NvidiaClientService.java:58`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L58)

**Tests**

- Env `groq` + bound `deepseek` calls Groq, not DeepSeek.
  [`DiagnosisServiceTest.java:75`](../../backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java#L75)
