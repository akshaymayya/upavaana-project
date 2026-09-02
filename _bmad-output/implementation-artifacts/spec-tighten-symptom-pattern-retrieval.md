---
title: 'Tighten symptom-pattern retrieval precision'
type: 'bugfix'
created: '2026-08-30'
status: 'draft'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Symptom-pattern RAG (`MIN_SYMPTOM_SCORE = 4` plus per-word hits on generic tokens like holes/tears/yellowing) lets weak overlaps into Medium-tier synthesis. The same pest/chewing photo can retrieve unrelated rows such as Wind Stress or Light Conditions.

**Approach:** Keep dual RAG and High/Medium/Low triggers. Raise the symptom-pattern floor, score multi-word phrases above isolated generic words, and log each qualifying match’s score plus the tokens/phrases that contributed.

## Boundaries & Constraints

**Always:** Dual RAG (AD-3): plant-name matches unchanged and still take precedence. Symptom-pattern still scores remaining diseases against vision vs `symptoms` + `disease_name`, still cap `MAX_SYMPTOM_CANDIDATES`. Empty list still means Low tier (AD-4). Logging must not include image bytes or API keys.

**Ask First:** Changing `MAX_SYMPTOM_CANDIDATES` or plant-name matching. Shipping a new Flyway/KB schema for tags.

**Never:** Vision model/prompts, `/api/diagnose` JSON contract, confidence-tier definitions (High = plant-name, Medium = symptom-pattern, Low = none). Do not dump the full KB into synthesis. Do not remove symptom-pattern retrieval.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Generic-only overlap | Vision describes chewed holes/tears; KB row “Wind Stress” symptoms only share isolated generic words (holes, tears, leaves) | Score below new minimum → not a `SYMPTOM_PATTERN` candidate | N/A |
| Phrase-rich pest overlap | Vision mentions chewed holes / insect damage; KB pest row contains those multi-word phrases | Score at or above minimum → may be included (subject to cap) | N/A |
| Specific leaf-spot pattern | Vision: brown/black lesions with yellow halos; no plant name; KB Leaf Spot has that pattern | Still eligible if phrase/specific-token score meets the new floor | N/A |
| Plant-name present | Vision names the plant | `PLANT_NAME` only for that plant’s diseases; no duplicate symptom-pattern for the same disease | N/A |
| Debug log | Any qualifying symptom-pattern candidate | INFO log: disease name, plant, integer score, contributing phrases/words | Never log image bytes |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/plantdoctor/service/DiagnosisService.java` -- `MIN_SYMPTOM_SCORE`, `scoreSymptomMatch`, `isSymptomKeyword`, candidate assembly and existing match-count logs
- `backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java` -- plant-name cap, leaf-spot mis-ID, plant-name precedence; add false-positive and phrase tests

## Tasks & Acceptance

**Execution:**
- [ ] `backend/src/main/java/com/plantdoctor/service/DiagnosisService.java` -- Raise `MIN_SYMPTOM_SCORE` so isolated generic keyword piles cannot qualify; prefer contiguous multi-word matches (e.g. chewed holes, insect damage) over single generic tokens; log score + triggers for each included symptom-pattern candidate (and optionally near-misses at debug if cheap)
- [ ] `backend/src/test/java/com/plantdoctor/service/DiagnosisServiceTest.java` -- Cover I/O matrix: wind-stress-style generic overlap excluded; phrase-rich pest included; keep leaf-spot and plant-name tests green (adjust fixtures only if the old leaf-spot text no longer meets the new floor)

**Acceptance Criteria:**
- Given vision text with pest chewing language and a KB wind/light row that only shares generic single words, when `findCandidateDiseases` runs, then that row is not a `SYMPTOM_PATTERN` candidate.
- Given a KB pest row whose symptoms contain a multi-word phrase also present in vision, when scoring, then that phrase contributes more than any one of its generic words alone.
- Given at least one symptom-pattern candidate is selected, when retrieval completes, then logs include score and contributing words/phrases for that candidate.
- Given a plant-name hit, when retrieval runs, then High-tier plant-name behavior is unchanged.

## Spec Change Log

## Design Notes

Today a comma-phrase can add +3 and up to 4 extra single-word hits (`length > 4`) — floor 4 is reachable without a specific pattern. Implementation should treat ultra-generic tokens (holes, tears, leaves, yellowing, brown, spots, damage, plant, leaf) as insufficient by themselves. Prefer `contains` of 2+ consecutive content words from the disease symptom phrase in the vision string.

Existing leaf-spot test uses “brown and black lesions” + “yellow halos”; keep that path working via phrase overlap, not by lowering the floor again.

## Verification

**Commands:**
- `cd backend && mvnw.cmd -q test -Dtest=DiagnosisServiceTest` -- expected: all tests in that class pass
