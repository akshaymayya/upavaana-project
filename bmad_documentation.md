# Plant Doctor App — BMAD Documentation

This document summarizes the development and debugging steps taken to build and refine the Plant Doctor App. It follows the **BMAD** (Background, Motivation, Approach, Details) framework.

---

## 1. Background
The goal of this project was to build the **Plant Doctor App**, a full-stack mobile application that allows users to take photos of their plants to receive an AI-powered health diagnosis.
The app relies on a hybrid architecture:
- **Frontend:** React Native (Expo) for the mobile app interface and camera integration.
- **Backend:** Java Spring Boot with MySQL for storing known plant diseases and treatments.
- **AI Integration:** NVIDIA NIM models for vision (image analysis) and DeepSeek V4 Pro for advanced reasoning (text synthesis).

## 2. Motivation
Initially, the app successfully performed basic image analysis using the NVIDIA `meta/llama-3.2-11b-vision-instruct` model. However, during end-to-end testing, several critical issues were discovered that degraded the user experience:
1. **Hallucination of Diagnosis:** The AI would confidently return incorrect diagnoses (e.g., claiming an unknown plant was a "Money Plant" with "Root Rot") because it was forced to choose from the database even when the plant didn't match.
2. **Database Contamination:** The matching logic in `DiagnosisService` used lenient OR conditions (`plantMatch || symptomMatch`). This meant a generic symptom like "yellowing" would trigger the "Money Plant" database record, polluting the AI's prompt with completely unrelated plant data.
3. **App Timeouts:** The DeepSeek V4 Pro model hosted on NVIDIA's endpoint was heavily congested, causing 90-second response delays. Because the backend was configured to retry twice (180 seconds), the mobile app would hit its strict 3-minute network limit and throw an `AbortError` before the fallback model could respond.

## 3. Approach
To solve these issues and make the app highly reliable and accurate, we took a three-pronged approach:
- **Refactoring AI Prompts:** We implemented a strict rule-based prompt for the synthesis model, forcing it to trust the Vision Analysis as the ground truth and enabling it to output "Unknown Disease" or "Unidentified Issue" when confidence is low.
- **Strict Database Matching:** We modified the core business logic to prevent cross-plant contamination. 
- **API and Timeout Optimizations:** We replaced the congested NVIDIA proxy for DeepSeek with the official DeepSeek API, and optimized the backend retry logic to prevent mobile app crashes.

## 4. Details (Step-by-Step Execution)

### Step 1: Stricter Synthesis Prompts
We modified `NvidiaClientService.java` to enforce strict constraints on the AI:
- Added a `RULE 1 — PLANT IDENTITY` directive telling the model it **must** use the plant name from the vision description.
- Introduced an `is_healthy` field to the expected JSON schema, allowing the model to explicitly state if a plant is healthy or if an issue is unidentifiable, rather than guessing.

### Step 2: Fixing Database Fallback Logic
We updated `DiagnosisService.java` so that if no diseases match the uploaded plant, it returns an **empty list** instead of dumping the entire database into the AI prompt. This forces the AI to synthesize advice independently based on its own general plant pathology knowledge.

### Step 3: Enforcing Strict Plant Matching
We found that the DB search was matching on `plantMatch || symptomMatch`. This was changed to strict `if (plantMatch)` matching. Now, if the vision model says "leafy green plant" (unknown species), the system will no longer accidentally fetch "Money Plant" just because they both share "yellow spots". 

### Step 4: Resolving Mobile App Timeouts (`AbortError`)
The backend was originally configured to retry the text synthesis model up to 2 times, each with a 90-second timeout. We reduced `maxAttempts` to 1. If the primary model fails, it immediately falls back to a fast, local Llama 3.1 8B model. This ensures the total response time stays well under the 3-minute React Native timeout.

### Step 5: Integrating Official DeepSeek API
Because the user's `nvapi-` key routed DeepSeek requests through NVIDIA's congested infrastructure, we updated the system to use a direct DeepSeek API key (`sk-...`). 
- Updated `NvidiaProperties.java` to include a dedicated `deepseekBaseUrl` pointing to `https://api.deepseek.com/v1`.
- Updated `application.yml` and `.env` with the new official key and endpoint.
- Removed NVIDIA-specific payload parameters (`chat_template_kwargs`) to ensure full compatibility with the official DeepSeek OpenAI-compatible endpoint.

### Conclusion
The application now cleanly handles unknown plants, successfully avoids hallucinations by enforcing strict database matching, and utilizes DeepSeek V4 Pro reliably without crashing the mobile client.
