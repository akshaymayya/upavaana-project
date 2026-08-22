---
name: Upavana Plant Doctor Experience
status: final
created: 2026-08-01
updated: 2026-08-01
sources:
  - _bmad-output/planning-artifacts/prds/prd-plant-doctor-2026-08-01/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md
---

# EXPERIENCE.md — Upavana Plant Doctor

## Foundation

- **Form factor:** Mobile — React Native (Expo ~54), portrait, single-column.
- **Visual identity:** See `DESIGN.md` — Upavana brand palette and logo rules.
- **Scope:** MVP photo diagnosis flow (Home → Loading → Results | Error).

## Information Architecture

| Screen | Purpose | Entry | Exit |
|--------|---------|-------|------|
| **Home** | Capture intent; photo source choice | App launch | Loading (with `imageUri`) |
| **Loading** | Upload + wait; engagement tips | Home | Results or Error |
| **Results** | Display Diagnosis | Loading success | Home (Diagnose Another) |
| **Error** | Friendly failure + retry | Loading failure | Home (Try Again) |

No tabs, drawer, or history stack in MVP.

## Voice and Tone

Friendly, reassuring, plain language — aligned with `{DESIGN.md}` Brand & Style. Celebrate healthy plants. Never alarmist for `Unidentified Issue`. Error copy avoids technical jargon (no stack traces, no raw API errors).

## Component Patterns

- **Photo actions (Home):** Two explicit choices — camera (primary CTA) vs. gallery (secondary outline). Permissions requested inline before picker launch.
- **Loading engagement:** Rotating status messages (15s) + plant-care tips (6s). Spinner uses `{colors.secondary}`.
- **Results hierarchy:** Logo → status banner → plant → diagnosis → symptoms → solution → confidence → disclaimer → CTA.
- **Healthy detection:** Prefer `is_healthy` from API; fallback to disease_name string match. Healthy banner uses celebratory copy.

## State Patterns

| State | Visual | Copy |
|-------|--------|------|
| **Healthy** | Green banner `{colors.healthy-background}` | "Plant looks Healthy!" |
| **Issue detected** | Amber banner `{colors.warning-background}` | "Issue Detected" |
| **Unidentified** | Same as issue; diagnosis text shows API value | Per backend |
| **Loading** | Centered spinner + status + tip card | Progressive status messages |
| **Timeout** | Error screen, warm background | "Request Timed Out" |
| **Network error** | Error screen | Connection-specific title |

## Interaction Primitives

- **Primary CTA:** `{colors.cta}` — Take Photo, Diagnose Another, Try Again.
- **Secondary:** Outline `{colors.primary}` — Choose from Gallery.
- **Back to start:** `navigation.popToTop()` from Results/Error.
- **No pull-to-refresh, no swipe-back customization** in MVP.

## Accessibility Floor

- Logo images have `accessibilityLabel="Upavana"`.
- Text contrast: primary text on background meets WCAG AA for body sizes.
- Touch targets ≥ 44pt vertical on buttons (18px padding + 16pt text).
- Status communicated by text + emoji, not color alone.

## Key Flows

**UJ-1 — Priya diagnoses yellowing pothos**

1. Opens app → Home with Upavana color logo.
2. Taps **Take Photo** (orange CTA) → camera → confirms crop.
3. Loading: tips rotate; status advances.
4. Results: issue banner, plant name, diagnosis, solution in branded cards.
5. Taps **Diagnose Another Plant** (orange CTA) → Home.

**UJ-2 — Marcus checks healthy snake plant**

1. Home → **Choose from Gallery** (green outline).
2. Loading → Results with green healthy banner.
3. Reads maintenance solution; feels reassured.

**UJ-3 — Demo failure recovery**

1. Loading exceeds 180s → Error with timeout copy.
2. **Try Again** (orange CTA) → Home.

## Logo Asset Map

| Asset | Path | Use |
|-------|------|-----|
| Color | `mobile/assets/upavana/logo-color.png` | Home, Results |
| White | `mobile/assets/upavana/logo-white.png` | Dark overlays (future) |
| Black | `mobile/assets/upavana/logo-black.png` | Monochrome contexts |

## Implementation Reference

Theme tokens: `mobile/src/theme/colors.ts` — must stay aligned with `DESIGN.md` frontmatter colors.
