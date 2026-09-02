---
name: Upavana Plant Doctor Experience
status: final
created: 2026-08-01
updated: 2026-08-29
sources:
  - _bmad-output/planning-artifacts/prds/prd-plant-doctor-2026-08-01/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md
---

# EXPERIENCE.md — Upavana Plant Doctor

Spines win on conflict with mocks or live UI. PRD FR-10 / FR-12 / FR-19 and P6 are adopted here (2026-08-29).

## Foundation

- **Form factor:** Mobile — React Native (Expo ~54), portrait, single-column.
- **Visual identity:** See `DESIGN.md` — Upavana brand palette and logo rules. P6: calm result framing, never alert/danger chrome on diagnosis.
- **Scope:** MVP photo diagnosis flow (Home → Loading → Results | Error).

## Information Architecture

| Screen | Purpose | Entry | Exit |
|--------|---------|-------|------|
| **Home** | Capture intent; photo source choice | App launch | Loading (with `imageUri`) |
| **Loading** | Upload + wait; engagement tips | Home | Results or Error |
| **Results** | Display Diagnosis | Loading success | Home (Diagnose Another) |
| **Error** | Friendly failure + retry | Loading failure | Home (Try Again) |

No tabs or drawer in MVP. Unidentified Issue is a **Results** state, never Error.

## Voice and Tone

Friendly, reassuring, plain language — aligned with `{DESIGN.md}` Brand & Style (P6). Celebrate healthy plants (P4). Diagnosis copy names the plant and the finding; it does **not** shout an alert. Never alarmist for `Unidentified Issue`. Error copy (timeout/network only) avoids technical jargon (no stack traces, no raw API errors).

**Forbidden on Results:** the string “Issue Detected”; warning/danger verbs as headlines; ⚠️.

## Component Patterns

- **Photo actions (Home):** Two explicit choices — camera (primary CTA) vs. gallery (secondary outline). Permissions requested inline before picker launch.
- **Loading engagement:** Rotating status messages (15s) + plant-care tips (6s). Spinner uses `{colors.secondary}`.
- **Results hierarchy:** Logo → **result header** (plain) → identified plant → diagnosis → symptoms → **treatment (FR-19 branch)** → confidence (subdued) → disclaimer → CTA.
- **Healthy detection:** Prefer `is_healthy` from API; fallback to disease_name string match. Healthy uses the green celebratory card (P4), not the issue result header.
- **Treatment branch (FR-19):** Switch on `treatment_type`, not on whether `treatment_steps` is a non-empty array (AD-2). Missing `treatment_type` → render `solution` with the **single-action** layout.

### Result header (all non-healthy)

Not a banner. Same card chrome as other Results cards (`{components.result-header}`).

| | Copy |
|--|------|
| **High / Medium** | No status slogan. Plant name is already in IDENTIFIED PLANT. Header line can be omitted, **or** a single calm line such as the diagnosis name only — never “Issue Detected.” |
| **Low — Unidentified Issue** | Diagnosis card shows **Unidentified Issue**. Directly under it (same card or next), locked support line: *We couldn’t confidently identify this from our curated research, so we’re being cautious rather than guessing.* Do not use “we cannot help you.” |

Optional quiet accent: 3px `{colors.primary}` or `{colors.warning-text}` left edge on the **diagnosis** card only — not a filled amber slab.

**Medium vs High:** Do not use a second alarm treatment. Distinguish via `confidence_note` (Medium must say the plant type was not confirmed in the KB). Visual weight of the diagnosis card stays the same; the note is the differentiator.

### Treatment — `single_action`

- Section label: **NEXT STEP** (not a multi-step checklist heading).
- Layout: `{components.treatment-single}` — one short instruction from `solution`.
- Do not show `treatment_steps`.

### Treatment — `care_plan`

- Section label: **CARE PLAN**.
- Layout: `{components.treatment-plan}` — numbered/day-labeled **rows**, visually distinct from NEXT STEP (inset list + day labels, not one body paragraph).
- Each row: `day` as the label (display as given, e.g. “Day 1”, “Day 2–3”) + `action`.
- 3–5 steps. If count is invalid, do not invent rows; fall back to `solution` in the single-action layout until the API is complete.
- `solution` may appear as one muted summary line **above** the list, not as a duplicate essay.

Healthy outcomes typically use `single_action` (continue current care).

## State Patterns

| State | Visual | Copy |
|-------|--------|------|
| **Healthy** | Green card `{colors.healthy-background}`; optional 🎉 | “Plant looks Healthy!” |
| **High / Medium issue** | Plain result cards; no ⚠️; no alert fill | Plant + `disease_name`; treatment per FR-19 |
| **Unidentified (Low)** | Same plain result chrome as High/Medium (not Error, not warning banner) | Locked FR-12 support line + Low-tier labeling in `confidence_note` / `solution`; FR-19 treatment |
| **Loading** | Centered spinner + status + tip card | Progressive status messages |
| **Timeout** | Error screen, warm background | “Request Timed Out” |
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
- Issue vs healthy is communicated by **wording and card structure**, not color or emoji alone. Do not rely on ⚠️.
- Care-plan rows: day label associated with its action (same row or `accessibilityLabel` combining both).

## Key Flows

**UJ-1 — Priya diagnoses yellowing pothos**

1. Opens app → Home with Upavana color logo.
2. Taps **Take Photo** (orange CTA) → camera → confirms crop.
3. Loading: tips rotate; status advances.
4. Results: **calm result cards** (plant, diagnosis, NEXT STEP or CARE PLAN). No warning banner. Climax: she knows what to do without feeling scolded.
5. Taps **Diagnose Another Plant** (orange CTA) → Home.

**UJ-2 — Marcus checks healthy snake plant**

1. Home → **Choose from Gallery** (green outline).
2. Loading → Results with green healthy card.
3. Reads a short NEXT STEP; feels reassured.

**UJ-3 — Demo failure recovery**

1. Loading exceeds 180s → Error with timeout copy.
2. **Try Again** (orange CTA) → Home.

**UJ-4 — Unidentified Issue (Low)**

1. Photo does not match the KB.
2. Results (not Error): diagnosis **Unidentified Issue**, locked conservative copy, still a NEXT STEP or CARE PLAN.
3. Confidence note is subdued; she is not treated as a system failure.

## Logo Asset Map

| Asset | Path | Use |
|-------|------|-----|
| Color | `mobile/assets/upavana/logo-color.png` | Home, Results |
| White | `mobile/assets/upavana/logo-white.png` | Dark overlays (future) |
| Black | `mobile/assets/upavana/logo-black.png` | Monochrome contexts |

## Implementation Reference

Theme tokens: `mobile/src/theme/colors.ts` — must stay aligned with `DESIGN.md` frontmatter colors.

Live `ResultsScreen.tsx` still uses ⚠️ / “Issue Detected” and a single solution paragraph — **spine wins**; implement FR-19 layouts in a follow-up story.

Key-screen sketch: [`mockups/results-calm.html`](mockups/results-calm.html) (illustrative; spines win on conflict).
