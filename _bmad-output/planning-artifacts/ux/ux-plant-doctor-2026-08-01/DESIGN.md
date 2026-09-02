---
name: Upavana Plant Doctor
description: Mobile plant health diagnosis — Upavana brand identity
status: final
created: 2026-08-01
updated: 2026-08-29
colors:
  primary: '#003A33'
  primary-dark: '#002A24'
  secondary: '#017B00'
  secondary-light: '#E6F4E6'
  cta: '#FE8D10'
  cta-dark: '#E07D00'
  background: '#F7FAF8'
  surface: '#FFFFFF'
  text-primary: '#003A33'
  text-secondary: '#3D5C55'
  text-muted: '#7A9189'
  border: '#DDE8E3'
  healthy: '#017B00'
  healthy-background: '#E6F4E6'
  warning-text: '#C45A00'
  warning-background: '#FFF4E8'
  error: '#B71C1C'
  error-background: '#FDF5F5'
typography:
  title:
    note: 'Platform native bold — iOS Title 2 / Android Headline Medium'
  body:
    note: 'Platform native — iOS Body / Android Body Large'
  label:
    note: '11pt uppercase tracked labels for card sections'
  meta:
    note: '11pt uppercase for footer/disclaimer'
rounded:
  sm: 12px
  md: 16px
  lg: 20px
  xl: 24px
spacing:
  '1': 4px
  '2': 8px
  '3': 12px
  '4': 16px
  '5': 24px
  '6': 32px
components:
  primary-button:
    background: '{colors.cta}'
    text: '#FFFFFF'
  secondary-button:
    border: '{colors.primary}'
    text: '{colors.primary}'
  result-header:
    background: '{colors.surface}'
    border: '{colors.border}'
    text: '{colors.text-primary}'
  treatment-single:
    background: '{colors.surface}'
    border: '{colors.border}'
    accent: '{colors.primary}'
  treatment-plan:
    background: '{colors.background}'
    border: '{colors.border}'
    day-label: '{colors.text-muted}'
    action: '{colors.text-secondary}'
---

## Brand & Style

Upavana conveys **nature's embrace** — warm, trustworthy, and organic. The app helps casual plant owners diagnose issues from a photo; the visual language should feel **reassuring, not clinical, and never like a warning or danger alert** (P6). Brand colors are extracted from the confidential Upavana logo (outer leaves, inner leaves, cupped hands).

Logo usage:
- **Color logo** (`logo-color.png`) — Home and Results on `{colors.background}`.
- **White logo** (`logo-white.png`) — reserved for dark overlays or future dark surfaces.
- **Black logo** (`logo-black.png`) — monochrome/small-icon contexts only.

Do not reproduce or redistribute logo artwork outside the app binary.

Logo usage:
- **Color logo** (`logo-color.png`) — Home and Results on `{colors.background}`.
- **White logo** (`logo-white.png`) — reserved for dark overlays or future dark surfaces.
- **Black logo** (`logo-black.png`) — monochrome/small-icon contexts only.

Do not reproduce or redistribute logo artwork outside the app binary.

## Colors

Palette sampled from `IC_Colour-19.png`:

- **Primary (`#003A33`)** — dark teal-green from outer leaves and wordmark. Headlines, body text, secondary button borders, shadows.
- **Secondary (`#017B00`)** — vibrant medium green from inner leaves. Loading spinner, tip headers, healthy-state accents.
- **CTA (`#FE8D10`)** — warm amber-orange from cupped hands. All primary action buttons (Take Photo, Diagnose Another, Try Again).
- **Background (`#F7FAF8`)** — soft off-white with green tint. Screen canvas.
- **Surface (`#FFFFFF`)** — cards and elevated content.

Semantic colors (not in logo, derived for UX):
- **Healthy** uses `{colors.secondary}` on `{colors.healthy-background}`. Celebratory, not a null result (P4).
- **Diagnosis result (all non-healthy, including Unidentified Issue)** uses the same **plain result frame** as other info cards: `{colors.surface}`, `{colors.border}`, `{colors.text-primary}`. No filled amber/red alert banner. `{colors.warning-text}` / `{colors.warning-background}` are **quiet accents only** (e.g. a 3px left edge on the diagnosis card) — never a full-width warning strip, never paired with ⚠️.
- **Errors** (timeout / network / server — Error screen only) use `{colors.error}` on `{colors.error-background}`; CTA buttons remain orange. Unidentified Issue is **not** this path.

Avoid: generic placeholder greens from early prototypes; red primary CTAs; gradients.

## Typography

Platform system fonts. Hierarchy:
- Card labels: 11pt, bold, uppercase, `{colors.text-muted}`, letter-spacing 1.
- Plant name: 22pt, extra-bold, `{colors.text-primary}`.
- Body: 14–15pt, `{colors.text-secondary}`, line-height 20–22.
- Footer/disclaimer: 11pt, `{colors.text-muted}`.

## Layout & Spacing

Screen horizontal padding: 24px. Card internal padding: 20–24px. Section gap: 16px. Major vertical rhythm: 32–40px between hero and actions.

Logo on Home: 280×72 max, centered. Logo on Results: 200×52 compact header.

## Elevation & Depth

Subtle shadows using `{colors.primary}` at 4–6% opacity. Cards use 1px `{colors.border}` hairline. No heavy elevation.

## Shapes

Cards: `{rounded.lg}` (20px). Buttons: `{rounded.md}` (16px). Status pills: 16px. Illustration circle: full round (90px diameter).

## Components

- **Primary button** — `{colors.cta}` fill, white label, `{rounded.md}`, light orange shadow.
- **Secondary button** — transparent, 2px `{colors.primary}` border, `{colors.primary}` label.
- **Info card** — `{colors.surface}`, `{rounded.lg}`, hairline `{colors.border}`.
- **Result header** — `{components.result-header}`. Same card language as info cards. Headline is plant + diagnosis (plain result), **not** an alert title. No warning icon. Healthy may keep a small celebratory mark (🎉) on the green healthy card only.
- **Treatment — single action** — `{components.treatment-single}`. One info-card. Section label **NEXT STEP**. A 3px `{colors.primary}` left edge. One short paragraph (`solution`). No day list, no numbered multi-step stack.
- **Treatment — care plan** — `{components.treatment-plan}`. Distinct from the single-action card: inset on `{colors.background}` inside a surface card, section label **CARE PLAN**. Each step is its own row with a `{colors.border}` divider: day label (11pt uppercase tracked `{colors.text-muted}`, e.g. `DAY 1` or `DAY 2–3`) stacked above the action (`{colors.text-secondary}`). 3–5 rows. Do not dump the plan as one paragraph in the single-action layout.
- **Disclaimer** — centered meta text below confidence note on Results.

Sketch (spines win): [`mockups/results-calm.html`](mockups/results-calm.html).

## Do's and Don'ts

**Do:** Use `mobile/src/theme/colors.ts` as the single source of truth in code. Use color logo on light screens. Keep CTAs orange.

**Don't:** Hardcode old prototype hex values (`#1B4332`, `#2D6A4F`, `#52B788`). Don't use the color logo on dark backgrounds. Don't embed logo in API or documentation artifacts. **Don't** use ⚠️, “Issue Detected”, filled amber/red alert banners, or danger language on Results — including Unidentified Issue. Don't render `care_plan` as the same paragraph card as `single_action`.
