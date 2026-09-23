# Plant Doctor App — Agent Build Brief

See project rules in chat / this file for phase order and constraints.

## Ground rules

- Do NOT change the tech stack, database, or architecture without asking first.
- Build in phase order; stop after each phase for confirmation.
- Ask specific questions when ambiguous.
- Never delete working code unless asked.
- Small testable pieces; error handling on external calls.
- Secrets via environment variables; `.env` in `.gitignore`.

## Tech stack

- Frontend: React Native (Expo in `mobile/`)
- Backend: Java, Spring Boot (`backend/`)
- Database: MySQL
- AI (live code): NVIDIA NIM vision (`analyzeImage`). Synthesis is selected by `ACTIVE_SYNTHESIS_PROVIDER`: `groq`, `openai` (gpt-4o), or `deepseek` (NVIDIA-hosted DeepSeek using `NVIDIA_API_KEY`). All synthesis paths fall back to NVIDIA NIM text. See `DiagnosisService` and `NvidiaClientService`.

## Phases

1. Backend skeleton — **done**
2. Data seeding — **done**
3. `/api/diagnose` — **done**
4. Frontend core flow — **in progress**
5. Follow-up chat — **not started**
6. Polish

Full API contract, schema, and screen list are in the initial project brief (conversation / duplicate in repo as needed).
