# Deferred work

- source_spec: `_bmad-output/implementation-artifacts/spec-wire-openai-synthesis.md`
  summary: Uncapped plant-name RAG candidates can blow OpenAI/NVIDIA context limits; fallback repeats the same oversized prompt.
  evidence: Edge review — pre-existing dual RAG has no prompt-size cap; not introduced as a new product rule in this wiring story.

- source_spec: `_bmad-output/implementation-artifacts/spec-wire-openai-synthesis.md`
  summary: Null or empty NVIDIA vision text can NPE or still call synthesis.
  evidence: Edge review — `analyzeImage` / `findCandidateDiseases` predate this change; live path did not add a vision-null guard.
