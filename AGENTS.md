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

- Frontend: React Native
- Backend: Java, Spring Boot (`backend/`)
- Database: MySQL
- AI: OpenAI (`gpt-4o-mini` for chat/light tasks; `gpt-4o` + JSON schema for final diagnosis)

## Phases

1. Backend skeleton — **current**
2. Data seeding — **current**
3. `/api/diagnose`
4. Frontend core flow
5. Follow-up chat
6. Polish

Full API contract, schema, and screen list are in the initial project brief (conversation / duplicate in repo as needed).
