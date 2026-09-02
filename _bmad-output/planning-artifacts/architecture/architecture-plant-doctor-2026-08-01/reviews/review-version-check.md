# Review — asserted-from-training-data tech / version risk

**Lens:** Live version research vs product contract. Flag only if the spine **newly** asserts unverified **library** versions.

**Spine:** `ARCHITECTURE-SPINE.md` (updated 2026-08-29)

**Trigger of interest:** AD-2 / AD-7 FR-19 JSON contract (`treatment_type`, `treatment_steps`).

**Date:** 2026-08-29

---

## Verdict

**Pass — no finding.**

AD-2 and AD-7 FR-19 changes are a **product JSON / synthesis-schema contract**. They do not name, pin, or depend on a library version. They did **not** require live version research.

The Stack table still lists library pins (Java, Spring Boot, OpenCSV, Expo, React, RN, TypeScript, React Navigation). Those pins **match the brownfield lockfiles/POM** and are **not new in this FR-19 revision**. This lens does not flag them.

---

## Scope of this lens

In scope:

- New or changed **library / framework version** strings asserted in the spine without a current, verifiable source (Maven/npm lock, official current release).
- Training-data guesses that would send builders to a wrong `pom.xml` / `package.json` pin.

Out of scope (do not flag):

- Product field names, enums, HTTP shapes, OpenAI `json_schema` **structure** (FR-19).
- Model **IDs** already bound by AD-6 / D-7 / D-8 (`gpt-4o`, NVIDIA NIM model strings).
- Jackson / Jackson-on-Spring annotations (`@JsonIgnoreProperties`) without a version number — they are the existing Spring Boot Jackson stack, not a new pin.
- Whether FR-19 is implemented in code yet (Deferred already records that).

---

## AD-2 / AD-7 FR-19 — product contract, not versions

### What changed (memlog 2026-08-29)

- **AD-2:** `DiagnosisResult` adds `treatment_type` (`single_action` | `care_plan`) and optional `treatment_steps` (3–5 `{ day, action }`). Additive JSON. Older clients ignore unknown keys. Missing `treatment_type` is not filled as a care plan.
- **AD-7:** Model chooses `treatment_type` from the problem on all tiers. `json_schema` and every synthesis prompt path describe the new fields. Persist full JSON in `queries.result_json`. Nullable `treatment_steps` under strict schema.

### Version-research test

| Claim in AD-2 / AD-7 | Kind | Live version research required? |
| --- | --- | --- |
| Field names `treatment_type`, `treatment_steps`, `day`, `action` | Product JSON | **No** |
| Enum `"single_action"` / `"care_plan"` | Product JSON | **No** |
| Cardinality 3–5 steps | Product rule | **No** |
| snake_case, `{ "error": "<message>" }` | Product JSON | **No** |
| OpenAI `json_schema` + nullable array | Provider **API feature** already in AD-6 (gpt-4o + json_schema) | **No** — not a library version |
| `@JsonIgnoreProperties(ignoreUnknown = true)` / mobile equivalent | Existing Jackson / TS parser convention | **No** — no version string |
| Compat: additive fields; lagging producer | Integration policy | **No** |

**Conclusion:** Treating FR-19 as a “look up Spring Boot / OpenAI SDK / Expo version” task would be a **false positive**. Builders implement the contract on the stack already pinned in Stack / POM / `package.json`.

---

## Stack table — library pins vs this revision

Pins in **Stack** (not introduced by FR-19):

| Name | Spine pin | Brownfield source | Newly asserted this revision? | Unverified training-data risk? |
| --- | --- | --- | --- | --- |
| Java | 21 | `backend/pom.xml` `<java.version>21</java.version>` | No | No — ratified |
| Spring Boot | 4.0.0 | `spring-boot-starter-parent` `4.0.0` | No | No — ratified |
| MySQL | 8 | Project convention / connector (unversioned major) | No | No for this lens |
| Flyway | Spring Boot managed | `spring-boot-starter-flyway` | No | No |
| OpenCSV | 5.12.0 | `opencsv` `5.12.0` in POM | No | No — ratified |
| Expo | ~54.0.0 | `mobile/package.json` `"expo": "~54.0.0"` | No | No — ratified |
| React | 19.1.0 | `"react": "19.1.0"` | No | No — ratified |
| React Native | 0.81.5 | `"react-native": "0.81.5"` | No | No — ratified |
| TypeScript | ~5.9.2 | `"typescript": "~5.9.2"` | No | No — ratified |
| React Navigation | ^7.x | `@react-navigation/native` `^7.3.14` | No | No — ratified (range matches) |

**Do not flag** these as unverified. They are code-owned seed, not FR-19 inventions.

---

## Adjacent strings that look like “versions” but are not library pins

Left **unflagged** under this lens (not new library versions; not FR-19):

| String | Where | Why not a finding here |
| --- | --- | --- |
| `meta/llama-3.2-11b-vision-instruct` | AD-6, Stack | Model ID; Gate 3 / D-7/D-8 |
| `openai/gpt-oss-20b`, llama-3.1-8b EOL 2026-08-26 | AD-6 | Model ID / provider EOL note; pre-FR-19 |
| `gpt-4o` + `json_schema` | AD-6, Stack | Target synthesis; product/provider |
| `deepseek-v4-pro` | Stack | Model family name; runtime default from AD-6 (2026-08-27). Code also uses NIM id `deepseek-ai/deepseek-v4-pro-0813`. Abbreviation vs NIM id is **not** a new library pin and **not** introduced by FR-19. Out of this lens unless a later review covers model-id drift. |

---

## Findings

**Critical:** none  
**High:** none  
**Medium:** none  
**Low:** none  

**False-positive to ignore:** “FR-19 mentions `json_schema` so research OpenAI SDK / Spring AI versions.” The spine does not pin an OpenAI Java SDK. Synthesis is HTTP + schema; Jackson is Boot-managed.

---

## Disposition

| Item | Action |
| --- | --- |
| AD-2 / AD-7 FR-19 | **Ignore** for version research — product contract |
| Stack library pins | **Ignore** — brownfield-ratified, not newly asserted |
| Spine edit for this lens | **None** |

---

## Compact return (for gate parent)

- **Verdict:** Pass — no finding.
- **Top findings:** (1) FR-19 AD-2/AD-7 is product JSON, not a library pin. (2) No new unverified library versions in this revision. (3) Stack pins match `pom.xml` / `mobile/package.json`.
- **File:** `_bmad-output/planning-artifacts/architecture/architecture-plant-doctor-2026-08-01/reviews/review-version-check.md`
