---
title: 'Session-only diagnosis history on Home'
type: 'feature'
created: '2026-08-29'
status: 'done'
route: 'one-shot'
---

# Session-only diagnosis history on Home

## Intent

**Problem:** After a diagnosis, the user has no in-app way to reopen earlier results from the same session without running the pipeline again.

**Approach:** Keep a React state list at the app root (image URI, diagnosis JSON, timestamp). On successful diagnose, append. Home shows cards (`plant_name — disease_name`, thumbnail, date). Tap opens Results. No backend, login, or disk persistence.

## Suggested Review Order

**App-level state**

- Provider wraps navigation so Home, Loading, and Results share one list.
  [`App.tsx:8`](../../mobile/App.tsx#L8)

- In-memory list only; `addDiagnosis` prepends a unique session id.
  [`DiagnosisSessionContext.tsx:24`](../../mobile/src/context/DiagnosisSessionContext.tsx#L24)

**Write on success only**

- Store after a 200 with `plant_name` and `disease_name`; pass `sessionId` into Results.
  [`LoadingScreen.tsx:111`](../../mobile/src/screens/LoadingScreen.tsx#L111)

**Cards and reopen**

- Heading is existing diagnosis fields, not a generated title.
  [`DiagnosisSessionContext.tsx:18`](../../mobile/src/context/DiagnosisSessionContext.tsx#L18)

- Home “This session” cards: thumbnail, date, tap → Results with that id.
  [`HomeScreen.tsx:109`](../../mobile/src/screens/HomeScreen.tsx#L109)

- Distinct Results routes so history taps are not a stale stack screen.
  [`AppNavigator.tsx:32`](../../mobile/src/navigation/AppNavigator.tsx#L32)
