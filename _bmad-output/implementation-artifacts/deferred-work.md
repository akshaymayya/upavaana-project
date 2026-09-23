# Deferred work

- source_spec: `_bmad-output/implementation-artifacts/spec-wire-openai-synthesis.md`
  summary: Uncapped plant-name RAG candidates can blow OpenAI/NVIDIA context limits; fallback repeats the same oversized prompt.
  evidence: Edge review — pre-existing dual RAG has no prompt-size cap; not introduced as a new product rule in this wiring story.

- source_spec: `_bmad-output/implementation-artifacts/spec-wire-openai-synthesis.md`
  summary: Null or empty NVIDIA vision text can NPE or still call synthesis.
  evidence: Edge review — `analyzeImage` / `findCandidateDiseases` predate this change; live path did not add a vision-null guard.

- source_spec: `_bmad-output/implementation-artifacts/spec-synthesis-provider-switch.md`
  summary: Plant-name cap keeps the first five DB rows, not the best symptom match; dropped rows can re-enter as symptom-pattern.
  evidence: Spec required first-5 scan order; ranking was Ask First. Review noted the sixth row can still join via symptom score.

- source_spec: `_bmad-output/implementation-artifacts/spec-synthesis-provider-switch.md`
  summary: DeepSeek path still has no json_schema / json_object constraint (OpenAI does).
  evidence: Frozen intent forbade prompt/schema changes except thinking disabled.

- source_spec: `_bmad-output/implementation-artifacts/spec-synthesis-reliability.md`
  summary: No WireMock/HTTP-layer test that DeepSeek synthesis never POSTs to api.deepseek.com or OpenRouter.
  evidence: DiagnosisServiceTest only verifies method routing on a mock NvidiaClientService.

- source_spec: `_bmad-output/implementation-artifacts/spec-synthesis-reliability.md`
  summary: Stacked vision + NIM (90s) + NVIDIA text retries can exceed the mobile 180s abort.
  evidence: Review of RestTemplate timeouts vs pipeline budget; pre-existing 180s client cap.

- source_spec: `_bmad-output/implementation-artifacts/spec-diagnosis-priority-formatted-solution.md`
  summary: Synthesis still has no post-parse check that solution is newline-separated or that disease_name was not blended.
  evidence: Review — isCompleteDiagnosis only requires a non-empty solution string; enforcement is prompt-only.

- source_spec: `_bmad-output/implementation-artifacts/spec-diagnosis-priority-formatted-solution.md`
  summary: formatGuidance.selftest.ts is not wired into mobile package.json scripts/CI.
  evidence: Parser tests run only via manual node --experimental-strip-types.



