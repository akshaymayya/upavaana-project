# Adversarial review — Architecture Spine (Plant Doctor MVP)

**Subject:** `_bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md`  
**Lens:** Cynical / one-level-down divergence (two units obey every AD, still incompatible)  
**Focus:** `DiagnosisResult.treatment_type` / `treatment_steps` (AD-2, AD-7)  
**Date:** 2026-08-29  
**Verdict:** **FAIL** — the spine does not seal the FR-19 treatment contract. Two builder units can satisfy every adopted AD and still ship JSON and UI that do not interoperate.

---

## Attack method

Take AD-1 through AD-14 as the entire builder contract. Independently specify two units at the next altitude (backend `DiagnosisResult` + synthesis schema, and mobile `DiagnosisData` + Results). Each unit cites only the spine. If they cannot round-trip FR-19 treatment without a hallway conversation, the AD failed its **Prevents** clause (“two clients inventing different treatment JSON”).

They failed.

---

## Construction 1 — `day` is an unconstrained string (AD-2 hole)

AD-2: `treatment_steps` is ordered `{ "day": string, "action": string }[]`. No grammar, locale, calendar, uniqueness, or sort key. AD-7 repeats `{ day, action }` and never pins `json_schema` types for those properties. The PRD addendum’s `"Today"` / `"Day 3"` / `"Day 7"` example is **not** in the spine.

**Unit A — Synthesis / Java (legal):** `json_schema` (or DeepSeek prompt) types `day` as string. Prompts say “label the day.” Model emits:

```json
"treatment_type": "care_plan",
"treatment_steps": [
  { "day": "Today", "action": "Isolate and wipe leaves." },
  { "day": "Day 3", "action": "Spray neem oil." },
  { "day": "Day 7", "action": "Repeat spray." }
]
```

**Unit B — Synthesis / Java (also legal):** Same ADs. Implementer treats `day` as a schedule index. Schema uses `"type": "string"` still, but prompt says “use day numbers.” Model emits:

```json
"treatment_type": "care_plan",
"treatment_steps": [
  { "day": "1", "action": "Isolate and wipe leaves." },
  { "day": "2", "action": "Spray neem oil." },
  { "day": "3", "action": "Repeat spray." }
]
```

**Unit C — still legal, worse:** AD-7 never requires `day` to be JSON string in schema. Implementer omits `type` on `day` or sets `"type": ["string", "integer"]`. Strict schema on OpenAI emits `"day": 1` (number). Jackson `record` with `String day` fails or coerces depending on config; TypeScript `day: string` is a lie at runtime.

**Mobile A:** Renders `step.day` as the row title (addendum-shaped copy works; `"1"` looks broken).  
**Mobile B:** `parseInt(day, 10)` / `/^Day (\d+)$/` to sort and show “Day N”. `"Today"` → `NaN`; order collapses.

All four combinations **obey AD-2 and AD-7**. Results UI and persisted `queries.result_json` are not a single product.

**Name the hole:** **`day` string format is unspecified** (and AD-7 does not pin JSON type, so **number vs string** is also legal).

---

## Construction 2 — `treatment_type` literals vs Java/TS enum case (AD-2 hole)

AD-2: only `"single_action"` or `"care_plan"`. Naming conventions say Java packages and TypeScript `strict`, not Jackson enum serialization. Brownfield `DiagnosisResult` is a record of `String` fields already named in snake_case — a natural next step is a typed enum.

**Unit D — Backend (claims AD-2):**

```java
public enum TreatmentType { SINGLE_ACTION, CARE_PLAN }

public record DiagnosisResult(..., TreatmentType treatment_type, List<TreatmentStep> treatment_steps)
```

Default Jackson writes enum **constant names**: `"SINGLE_ACTION"`, `"CARE_PLAN"`. Property naming `SNAKE_CASE` does **not** rewrite enum values. Implementer argues the domain enum *is* those two values; JSON is an encoding detail the spine forgot.

**Unit E — Backend (also claims AD-2):** `String treatment_type` with values exactly `"single_action"` / `"care_plan"`, matching the rule’s quoted literals.

**Unit F — Mobile:** `treatment_type === "care_plan"` (and TypeScript union `'single_action' | 'care_plan'`). Unit D’s payload never matches; UI always falls through to the “missing `treatment_type` → use `solution` as a single instruction” compat path even when a care plan is present. Unit E works.

Unit D is the weasel reading: the AD quoted JSON tokens but never said “wire format is lowercase snake_case; Java enums MUST use `@JsonProperty` / `@JsonValue`, not `WRITE_ENUMS_USING_TO_STRING` of the constant name.” Consistency Conventions do not mention enum JSON. A second reader who copies the quoted strings is compatible with mobile; the enum reader is not.

**Name the hole:** **`treatment_type` enum case / wire tokens vs Java enum constant names** (`single_action` vs `SINGLE_ACTION`). Same risk if TS uses `SingleAction` / `CarePlan` in a serializer.

---

## Construction 3 — omit / `null` / `[]` vs “present only” (AD-2 self-contradiction)

AD-2 in one breath:

- `treatment_steps` **present only** when `treatment_type` is `"care_plan"`.
- Omit, **`null`, or `[]`** when `single_action`.

`[]` **is present**. The rule authorizes a payload that its own “present only” clause forbids.

**Unit G — Producer:** `single_action` + `"treatment_steps": []` (explicitly allowed).  
**Unit H — Producer:** `single_action` and omits the key (also allowed).  
**Unit I — Producer:** `single_action` + `null` (also allowed). AD-7 even *requires* nullable array in OpenAI schema so the model can emit `null`.

**Unit J — Mobile (legal reading of “optional treatment_steps”):**

```ts
if (data.treatment_steps) { /* care plan list */ } else { /* single_action solution */ }
```

In JavaScript, `[]` is truthy. Unit G renders an **empty care-plan list** for a single action. Units H/I render `solution`. Same AD, three on-the-wire shapes, two UIs.

**Unit K — Mobile (other legal reading):** Branch only on `treatment_type`. Then `[]` is ignored. Unit J and Unit K both implement FR-19 “visually distinct” using different predicates the spine never chooses.

**Name the hole:** **three legal absences (`omit` | `null` | `[]`) plus JS truthiness; “present only” contradicts `[]`.**

---

## Construction 4 — `care_plan` + `solution` dual body (AD-2 / AD-7 silent)

AD-2: for `single_action`, `solution` is one short instruction; do not send a multi-day list. For `care_plan`, `solution` is still a required-looking `DiagnosisResult` field with **no** rule. AD-7: steps 3–5 when care plan; `solution` still undescribed. PRD “may be a one-line summary” is not imported into the AD.

**Unit L:** `care_plan` with a full essay in `solution` **and** 3–5 steps (steps copy the essay).  
**Unit M:** `care_plan` with `solution` one-liner; details only in `treatment_steps`.  
**Unit N — Mobile:** Always shows `solution` (current Results pattern). Care plan looks like a multi-day paragraph **plus** a list, or like a short line plus a list.  
**Unit O — Mobile:** Hides `solution` when `treatment_type === "care_plan"`. Unit L’s essay never appears.

Both mobiles can cite AD-2 (fields exist; ignoreUnknown; FR-19 distinct layouts). The spine never says which field is canonical on screen.

---

## Construction 5 — invalid length / invalid type: no reject rule

AD-2: **3–5** items (minimum 3). PRD elsewhere says “3–5 steps **maximum**” (could be read as 1–5). The spine does not import a clamp, HTTP 500, or re-prompt.

**Unit P:** `extractJson()` + deserialize; if 2 or 6 steps, still `200` and persist.  
**Unit Q:** Post-validate; 2 steps → 500 `{ "error": "..." }`.  
**Unit R:** Truncate to 5 or pad to 3.

All can claim they implemented “3–5”. Demo photos flap between success and error depending on which unit synthesized.

AD-7: model **must not** default every issue to checklist **and must not** default every issue to `single_action`. That is an untestable prompt vibe, not a validator. Two prompt authors will encode opposite “when in doubt” heuristics; both satisfy the AD.

---

## Other findings (spine still weasel-grade)

- AD-3 uses `MatchType.PLANT_NAME` / `SYMPTOM_PATTERN` in code and `PLANT_NAME_MATCH` / `SYMPTOM_PATTERN_MATCH` in prompts; AD-7’s High trigger is `` `PLANT_NAME` candidate ``. Two `DiseaseCandidate` + prompt units will disagree on the literal the model is told to honor; Medium copy rules then fire on the wrong tier.
- `symptoms_matched` is named in AD-2 and never typed. Brownfield and addendum use a **string**; a greenfield unit can emit `string[]`. ignoreUnknown does not help when the key exists with the wrong JSON type. Mobile `string` vs `join(',')` diverges.
- AD-7 mandates Groq in “every synthesis prompt path”; AD-6 and Stack have no Groq. Two units: one adds a Groq client “because AD-7 listed it,” one does not.
- AD-6 runtime default is DeepSeek; AD-11 demo keys are `NVIDIA_API_KEY` + `OPENAI_API_KEY`. DeepSeek-default unit requires `DEEPSEEK_API_KEY` and can omit OpenAI; ops unit following AD-11 ships without DeepSeek and fails the default path.
- AD-6 “missing NVIDIA key is a clear server error” vs AD-2 errors only 400 or 500 — no 401/502/504. One unit maps NIM 401 to 500 `"error"`; another invents 503. Mobile that only special-cases 400/500 vs timeout 180s behaves differently.
- `confidence_note` must include High / Medium / Low “or healthy” — not a token grammar. Unit A: `"High — ..."`. Unit B: `"Confidence: HIGH"`. Unit C: healthy uses `"Low"` because AD-7’s Healthy row does not map to a tier word. Mobile that styles on `/High/` vs `/healthy/i` splits.
- Healthy: AD-7 `disease_name` `Healthy`, `is_healthy: true`. FR-11 (not fully restated) also allows name *containing* “Healthy”. Two Results units disagree on `is_healthy === false` + `disease_name: "Healthy plant"`.
- `MAX_SYMPTOM_CANDIDATES` and “minimum score” are named, not numbered. Dual RAG units return different candidate set sizes; Medium vs High rates diverge with no AD break.
- AD-2 success body lists fields but not which are required in `json_schema`. One schema requires `treatment_type`; lagging producer + “missing treatment_type is not filled with a care plan” fights strict schema. OpenAI path then cannot emit the compat shape AD-2 promised.
- Deferred **Ship FR-19 fields in code** while AD-2/AD-7 are ADOPTED. Backend unit ships fields; mobile unit defers “until FR-19 UI.” Compat says older clients ignore unknown keys — that protects old mobile, not new mobile against old backend. The deferred line is an explicit license for the producer/consumer split the ADs claim to prevent.
- UI palette `#F5F8F5` vs PRD/UX `#F7FAF8` — two screen units both “follow architecture” vs “follow PRD” and ship different tokens. Spine pretends it owns palette.
- AD-13 `uploads/<uuid>.<ext>` — allowed extensions, content-type, max bytes unspecified. Two diagnose controllers accept HEIC vs JPEG-only; vision units then fail “honestly” on the other’s uploads.
- Pipeline ≤ 180s vs vision 25s × 2 + synthesis 25s × 2 + DeepSeek unbounded in the AD. Two timeout units: one 180s wall clock on `diagnosePlant()`, one per-client 25s only; mobile abort vs late 200.
- Status `approved-verbal` / “formal written sign-off pending” on an ADOPTED contract — builders cannot tell whether AD-2 revision 2026-08-29 is law or a draft. Two sprint units freeze different revisions.

---

## Findings list (descriptions only)

- `treatment_steps[].day` is specified only as `string`, with no format, locale, uniqueness, monotonic order, or calendar (Today / Day N / ISO date / integer-in-a-string all comply); AD-7 does not require `json_schema` `type: string` for `day`, so a number `1` also complies until Jackson/TS blow up.
- `treatment_type` is specified as the JSON tokens `"single_action"` and `"care_plan"` but Java/TS enum serialization is unset, so `SINGLE_ACTION` / `CARE_PLAN` (Jackson default) and lowercase snake_case can both be argued as the two allowed values.
- AD-2 both forbids `treatment_steps` except on `care_plan` and allows `[]` on `single_action`; omit vs `null` vs `[]` are three legal encodings; a consumer that branches on truthiness of `treatment_steps` treats `[]` as a care plan.
- `solution` vs `treatment_steps` ownership for `care_plan` is unset, so one unit duplicates the plan in `solution` and another uses a one-line summary; Results can show essay, list, or both.
- Step count “3–5” has no reject/clamp/reprompt rule, and sits next to PRD “maximum” language the spine did not reconcile; producers will 200, 500, or mutate the array for the same model output.
- “Must not default every issue to checklist and must not default to `single_action`” is not an enforceable decision; two prompt/schema units will pick opposite fallbacks.
- Match-type identifiers are not one token across AD-3 code, AD-3 prompts, and AD-7 triggers (`PLANT_NAME` vs `PLANT_NAME_MATCH`), so High/Medium labeling diverges.
- `symptoms_matched` has no JSON type; string vs string[] both satisfy the field list.
- AD-7 names a Groq synthesis path that AD-6 and Stack do not exist; one unit will invent Groq.
- Demo secrets (AD-11 OpenAI required) contradict DeepSeek as runtime default (AD-6).
- Error surface is only 400/500 while providers and missing keys imply other failures; mapping is free.
- `confidence_note` / Healthy tier wording is not a closed vocabulary.
- Retrieval caps and score floors are symbolic (`MAX_SYMPTOM_CANDIDATES`), so candidate sets are not determined.
- Strict `json_schema` required-property sets are not listed, so they can outlaw the “missing `treatment_type`” compat path AD-2 describes.
- Deferred “ship FR-19 in code” while the contract is ADOPTED licenses producer/consumer skew.
- Palette, image `ext`, and 180s vs per-call timeouts are under-specified relative to what mobile and vision already need to agree on.
- Gate metadata (`approved-verbal`, sign-off pending) leaves ADOPTED ADs socially optional.

---

## What would actually close AD-2 / AD-7

Wire examples **in the AD**, not only in the PRD addendum: canonical `day` grammar (and JSON type); exact `treatment_type` tokens and “Java/TS enums serialize to those tokens, never `SINGLE_ACTION`”; one absence encoding for `single_action` (`null` **or** omit, not `[]`); consumer branch = `treatment_type`, not array truthiness; `care_plan.solution` = one-line summary, list is canonical; validate 3–5 or define repair; `json_schema` enum + `day`/`action` types + required arrays on all providers that can enforce them; DeepSeek/NVIDIA prompt copies of the same enum and day grammar.

Until then, FR-19 is a slogan sitting on a hole.
