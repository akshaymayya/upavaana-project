---
title: 'Commit one diagnosis and format solution text'
type: 'feature'
created: '2026-08-30'
status: 'done'
review_loop_iteration: 0
baseline_commit: '2b8c5bcec8c0dd0a8f53577d4faa3529e80b0d92'
context: []
---



## Intent

**Problem:** With several KB candidates, synthesis blends competing causes into one diagnosis. `solution` is a single run-on (today’s prompt even asks for 4–6 numbered steps), so pest evidence (holes/chew marks) gets mixed with generic yellowing/wilt advice.

**Approach:** Prompt (and user guidance) must pick **one** primary diagnosis from the strongest vision evidence. Specific, unambiguous signs (holes, chew marks, visible insects) outrank generic multi-cause signs (yellowing, wilting) when they disagree. Secondary theories belong only in `confidence_note`. `solution` is markdown-lite: lead sentence, `\n` between steps, `**key action`** for the mobile renderer. Results parses only `**bold**` and newlines — not full markdown.

## Boundaries & Constraints

**Always:** One `disease_name`. Primary treatment matches that diagnosis. Healthy/Unidentified rules already in prompts stay. JSON remains a raw object (no ``` fences around the payload). Markup lives **inside** string values only.

**Ask First:** Shipping FR-19 `treatment_type` / `treatment_steps` JSON fields in this change. Changing vision or retrieval scoring.

**Never:** Full markdown (lists, headers, links). Blending two diseases into `disease_name` or equal-weight steps in `solution`. New UI screens.

## I/O & Edge-Case Matrix


| Scenario             | Input / State                                                                    | Expected Output / Behavior                                                                                      | Error Handling                        |
| -------------------- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| Pest vs generic wilt | Vision: clear holes/chew marks; candidates include pest + yellowing/wilt disease | `disease_name` pest-related; `solution` leads with pest treatment (`**…**`); wilt/fertilizer/pH not equal steps | Extra theory → `confidence_note` only |
| Single candidate     | One PLANT_NAME match                                                             | Same format rules; still `\n` + `**`                                                                            | N/A                                   |
| Unidentified         | No KB fit, vision lists damage                                                   | Unidentified Issue; formatted general guidance, not Healthy                                                     | Existing healthy-consistency coerce   |
| Renderer             | `solution` with `**neem oil spray**` and `\n`                                    | Results shows bold + separate lines                                                                             | Unmatched `**` shown as plain text    |
| Healthy              | Vision: no symptoms                                                              | Short formatted continue-care; no pest lead                                                                     | N/A                                   |




## Code Map

- `backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java` -- `synthesisSystemPrompt` (esp. rules 6–8), NVIDIA text system prompt, `synthesisUserPrompt` match guidance
- `mobile/src/screens/ResultsScreen.tsx` -- `solution` currently one `Text`
- `mobile/src/components/` -- new markdown-lite text component (create)
- `mobile/src/types/diagnosis.ts` -- `solution` only unless FR-19 is in scope (Ask First)



## Tasks & Acceptance

**Execution:**

- [x] `NvidiaClientService.java` -- Replace “4 to 6 numbered steps / no markdown” with: one diagnosis; weight specific visual evidence over generic patterns; `solution` = lead sentence + newline-separated short steps; wrap the key treatment phrase in `**`; secondary causes only in `confidence_note`. Mirror on NVIDIA fallback prompt. User prompt: when candidates conflict, prefer holes/chew/insects over yellowing/wilt.
- [x] `mobile/src/components/FormattedGuidanceText.tsx` (or similar) -- Parse `**…**` and `\n` into `Text` / nested bold; no other markdown
- [x] `ResultsScreen.tsx` -- Render `solution` through that component
- [x] Unit tests -- Prompt contains prioritization + `**` / newline instructions; parser covers bold, multi-line, unmatched stars

**Acceptance Criteria:**

- Given vision with holes/chew marks and a weaker yellowing candidate, when synthesis guidance is applied, then prompts instruct a pest-primary `disease_name` and pest-first `solution`, not a blended plan.
- Given a `solution` string with `**neem oil spray**` and line breaks, when Results renders, then the phrase is bold and steps are visually separated.
- Given `confidence_note`, when a second cause is possible, then it is not duplicated as equal `solution` steps.



## Spec Change Log



## Design Notes

Today rule 7 **requires** a 4–6 step essay — that fights this spec. Drop it.

Markup: `**double asterisks**` only. JSON rule 6 stays “no code fences around JSON”; inner `**` is required.

FR-19 `treatment_steps` is **out** unless you approve Ask First. Prompt may say “if you emit treatment_steps, same line/bold rules” without adding schema fields.

Failure-case gold: disease pest/chewing; first line pest action in `** **`; not a six-step mix of neem + fertilizer + pH.

## Verification

**Commands:**

- `cd backend && mvnw.cmd -q test -Dtest=NvidiaClientServiceVisionPromptTest` -- extend or add synthesis-prompt assertions
- Mobile parser test via Jest if the app already has a test runner; otherwise a tiny `formatGuidance.test.ts` if Expo/Jest is configured

**Manual checks (if no CLI):**

- Re-run the holes + yellowing photo after API restart; `solution` has newlines in JSON; Results shows bold + breaks

## Suggested Review Order

**Synthesis commit + format**

- Evidence rules: holes/chew beat yellowing; Healthy stays short.
  [`NvidiaClientService.java:64`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L64)

- Same rules on Groq/OpenAI/DeepSeek system prompt.
  [`NvidiaClientService.java:72`](../../backend/src/main/java/com/plantdoctor/service/NvidiaClientService.java#L72)

**Results markdown-lite**

- `**bold**` plus newline split (also literal `\\n` and CRLF).
  [`formatGuidance.ts:8`](../../mobile/src/utils/formatGuidance.ts#L8)

- Nested `Text` for bold spans.
  [`FormattedGuidanceText.tsx:10`](../../mobile/src/components/FormattedGuidanceText.tsx#L10)

- Recommended Action uses the formatter.
  [`ResultsScreen.tsx:80`](../../mobile/src/screens/ResultsScreen.tsx#L80)

**Tests**

- Prompt substring checks.
  [`NvidiaClientServiceSynthesisPromptTest.java:8`](../../backend/src/test/java/com/plantdoctor/service/NvidiaClientServiceSynthesisPromptTest.java#L8)


