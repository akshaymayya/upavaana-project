# 🌿 Plant Doctor App — Full Project Documentation

> Generated: 2026-07-29 | Status: Phases 1–4 Complete, Phase 5 Next

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Repository Structure](#3-repository-structure)
4. [Database Schema](#4-database-schema)
5. [Backend — Spring Boot](#5-backend--spring-boot)
6. [AI / Dual Model Strategy](#6-ai--dual-model-strategy)
7. [Mobile App — React Native (Expo)](#7-mobile-app--react-native-expo)
8. [API Contract](#8-api-contract)
9. [Environment Variables](#9-environment-variables)
10. [Running the Project](#10-running-the-project)
11. [Key Decisions & Fixes](#11-key-decisions--fixes)
12. [Phases & Roadmap](#12-phases--roadmap)

---

## 1. Project Overview

**Plant Doctor** is an AI-powered mobile application that lets users photograph a plant and receive an instant diagnosis: what plant it is, what disease or pest is affecting it, what symptoms are visible, and what treatment to apply.

### Core Flow
```
User takes photo → Mobile app uploads image → Spring Boot backend
→ Model 1 (Vision AI): identify plant + symptoms
→ MySQL DB: look up matching diseases
→ Model 2 (Text AI): synthesize diagnosis + treatment JSON
→ Results shown on phone screen
```

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Mobile Frontend | React Native (Expo SDK 54) |
| Backend | Java 25, Spring Boot 4.0 |
| Database | MySQL 8.0 |
| AI - Vision | NVIDIA NIM `meta/llama-3.2-11b-vision-instruct` |
| AI - Text Synthesis | NVIDIA NIM `meta/llama-3.1-8b-instruct` |
| ORM | Hibernate / Spring Data JPA |
| DB Migrations | Flyway |
| Build Tool | Maven (mvnw wrapper) |

---

## 3. Repository Structure

```
new/
├── backend/                          # Spring Boot API
│   ├── .env                          # Secrets (not committed)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   ├── uploads/                      # Temp image storage
│   └── src/
│       ├── main/
│       │   ├── java/com/plantdoctor/
│       │   │   ├── PlantDoctorApiApplication.java
│       │   │   ├── config/
│       │   │   │   └── NvidiaProperties.java
│       │   │   ├── controller/
│       │   │   │   └── DiagnoseController.java
│       │   │   ├── entity/
│       │   │   │   ├── Disease.java
│       │   │   │   └── Query.java
│       │   │   ├── repository/
│       │   │   │   ├── DiseaseRepository.java
│       │   │   │   └── QueryRepository.java
│       │   │   └── service/
│       │   │       ├── DiagnosisResult.java
│       │   │       ├── DiagnosisService.java
│       │   │       └── NvidiaClientService.java
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       │           ├── V1__create_diseases_table.sql
│       │           ├── V2__create_queries_table.sql
│       │           └── V3__seed_diseases.sql
│       └── test/
│           └── java/com/plantdoctor/
│               ├── PlantDoctorApiApplicationTests.java
│               └── service/DiagnosisServiceTest.java
│
└── mobile/                           # React Native (Expo) App
    ├── package.json
    ├── app.json
    ├── tsconfig.json
    └── src/
        ├── screens/
        │   ├── HomeScreen.tsx        # Camera + Gallery buttons
        │   ├── LoadingScreen.tsx     # Upload + AI processing
        │   ├── ResultsScreen.tsx     # Diagnosis results cards
        │   └── ErrorScreen.tsx      # Error & retry
        └── navigation/
            └── AppNavigator.tsx
```

---

## 4. Database Schema

### `diseases` table

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT PK | Auto-increment primary key |
| `plant_name` | VARCHAR(255) | Common plant name (e.g., "Hibiscus") |
| `disease_name` | VARCHAR(255) | Disease or pest name (e.g., "Mealybugs") |
| `symptoms` | TEXT | Comma-separated visual symptoms |
| `solution` | TEXT | Recommended treatment steps |
| `severity` | VARCHAR(50) | `low`, `medium`, `high` |
| `confidence_score` | DOUBLE | DB match confidence (0.0–1.0) |

### `queries` table

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `image_path` | VARCHAR(500) | Path to saved uploaded image |
| `vision_result` | TEXT | Raw vision model output |
| `diagnosis_json` | TEXT | Final JSON diagnosis result |
| `created_at` | TIMESTAMP | When the query was made |

---

## 5. Backend — Spring Boot

### `DiagnoseController.java`
- `POST /api/diagnose` — accepts `multipart/form-data` with an `image` file
- Delegates to `DiagnosisService`
- Returns `DiagnosisResult` as JSON

### `DiagnosisService.java`
1. Saves the uploaded image to `uploads/` with a UUID filename
2. Calls `NvidiaClientService.analyzeImage()` (Vision Model)
3. Extracts keywords from the vision output
4. Queries `DiseaseRepository` for matching diseases using a keyword LIKE search
5. Calls `NvidiaClientService.synthesizeDiagnosis()` with both the vision result and DB candidates
6. Saves the query history to `QueryRepository`
7. Returns the final `DiagnosisResult`

### `NvidiaClientService.java`
- **Two separate model calls** (Dual Model Strategy):
  1. `analyzeImage()` — calls the Vision NIM model with base64-encoded image
  2. `synthesizeDiagnosis()` — calls the Text NIM model with vision output + DB candidates
- **Timeout**: 25 seconds connect + read timeout per request
- **Retry logic**: up to 2 attempts with 1-second sleep between retries
- **JSON extraction**: robust `extractJson()` helper that finds `{...}` in the LLM response even if the model wraps the JSON in conversational text or markdown code fences

### `NvidiaProperties.java`
Spring Boot `@ConfigurationProperties` class binding the `nvidia.*` YAML config block. All values are overridable via environment variables.

### `DiagnosisResult.java`
The JSON response object returned by the API:
```json
{
  "plant_name": "Hibiscus",
  "disease_name": "Mealybugs",
  "symptoms_matched": "White, cottony substance on leaves, yellowing leaves",
  "solution": "Wipe affected areas with alcohol-dipped cotton swab, apply neem oil, isolate infected plant.",
  "severity": "high",
  "confidence": "High, direct match with symptoms and solution.",
  "is_healthy": false
}
```

---

## 6. AI / Dual Model Strategy

### Why Two Models?

| Step | Model | Purpose |
|---|---|---|
| **Vision** | `meta/llama-3.2-11b-vision-instruct` | Read the actual photo — identify plant species and visible symptoms |
| **Text Synthesis** | `meta/llama-3.1-8b-instruct` | Combine vision output + DB data → produce clean JSON diagnosis |

### Why Smaller Models?

The original configuration used `90B` (vision) and `70B` (text) models. These large models frequently timed out on NVIDIA's free-tier API due to high queue loads.

**Fix applied:**
- Vision: `90B → 11B` (via `NVIDIA_VISION_MODEL` env var)
- Text: `70B → 8B` (via `NVIDIA_TEXT_MODEL` env var)

The 8B/11B models are significantly faster with comparable accuracy for plant diagnosis tasks.

### What Happens When Disease Is NOT in DB?

The text synthesis prompt **always** instructs the AI to use DB candidates as primary reference, but to **synthesize its own expert advice** if no DB match is found. This means the app always returns a useful diagnosis — never a blank response — even for plants not seeded in the database.

---

## 7. Mobile App — React Native (Expo)

### SDK & Dependencies
- Expo SDK 54
- React 19.1.0 / React Native 0.81.5
- `expo-image-picker` — camera and gallery access
- `expo-constants` — reads Metro server host IP dynamically
- `@react-navigation/native` + `@react-navigation/native-stack` — screen navigation

### Screens

#### `HomeScreen.tsx`
- Displays app logo and two action buttons: **Take Photo** and **Choose from Gallery**
- Requests camera/media library permissions before launching pickers
- On image selected → navigates to `LoadingScreen` with the selected image URI

#### `LoadingScreen.tsx`
- Shows a rotating plant care tip carousel while the image is being analyzed
- Builds the multipart form-data request with the image
- **Dynamically resolves the backend URL** using `Constants.expoConfig?.hostUri` to extract the LAN IP (e.g., `192.168.0.101:8080`) — critical for physical device testing
- Sets a **120-second abort timeout** to allow backend retry logic to complete
- On success → navigates to `ResultsScreen`
- On failure → navigates to `ErrorScreen`

#### `ResultsScreen.tsx`
- Renders the diagnosis as a set of clean cards:
  - **Identified Plant** card
  - **Diagnosis** card (disease name in red for issues, green for healthy)
  - **Symptoms Observed** card
  - **Recommended Action** card
  - **Confidence** footer
- Handles `is_healthy: true` case gracefully with a green "Your plant looks healthy!" message
- "Diagnose Another Plant" button resets back to Home

#### `ErrorScreen.tsx`
- Shows a friendly error message with a Tip for Success card
- "Try Again" button navigates back to Home
- Does not display raw error stack traces to the user

---

## 8. API Contract

### `POST /api/diagnose`

**Request:**
```
Content-Type: multipart/form-data
Body: image (file)
```

**Success Response (200):**
```json
{
  "plant_name": "Hibiscus",
  "disease_name": "Mealybugs",
  "symptoms_matched": "White, cottony substance on leaves, yellowing leaves",
  "solution": "Wipe affected areas with alcohol-dipped cotton swab, apply neem oil, isolate infected plant.",
  "severity": "high",
  "confidence": "High, direct match with symptoms and solution.",
  "is_healthy": false
}
```

**Error Response (500):**
```json
{
  "error": "Error synthesizing diagnosis via NVIDIA NIM (failed after 2 attempts): Read timed out"
}
```

---

## 9. Environment Variables

File: `backend/.env` (loaded manually before server start)

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | Full JDBC connection string | `jdbc:mysql://localhost:3306/plant_doctor?...` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `Polaliamma1818` |
| `NVIDIA_API_KEY` | NVIDIA NIM API key | `nvapi-...` |
| `NVIDIA_VISION_MODEL` | Vision model ID (overrides default) | `meta/llama-3.2-11b-vision-instruct` |
| `NVIDIA_TEXT_MODEL` | Text model ID (overrides default) | `meta/llama-3.1-8b-instruct` |

> [!WARNING]
> `.env` is NOT committed to git. Keep it private.

---

## 10. Running the Project

### Backend

```powershell
# In backend/ directory

# 1. Load environment variables into the current process
Get-Content .env | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith("#") -and $line -like "*=*") {
    $parts = $line.Split("=", 2)
    $name = $parts[0].Trim()
    $val = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($name, $val, "Process")
  }
}

# 2. Start the server
.\mvnw.cmd spring-boot:run
```

Server runs at: `http://localhost:8080`

### Mobile App

```powershell
# In mobile/ directory
npx expo start --clear
```

- Scan the QR code with **Expo Go** app on your phone
- Make sure your phone and computer are on the **same Wi-Fi network**
- The app auto-detects the backend URL from Metro's LAN IP

---

## 11. Key Decisions & Fixes

### Fix 1: Backend Hanging Indefinitely
**Problem:** The NVIDIA API would sometimes hang for 3+ minutes with no response (0 bytes received).  
**Fix:** Set 25-second read/connect timeouts on the `RestTemplate`. Added retry logic (2 attempts, 1s sleep between).

### Fix 2: Vision Model Timing Out (90B → 11B)
**Problem:** `meta/llama-3.2-90b-vision-instruct` frequently timed out on NVIDIA's free tier.  
**Fix:** Switched to `meta/llama-3.2-11b-vision-instruct` via `NVIDIA_VISION_MODEL` env var.

### Fix 3: Text Model Timing Out (70B → 8B)
**Problem:** `meta/llama-3.3-70b-instruct` timed out during peak API load.  
**Fix:** Switched to `meta/llama-3.1-8b-instruct` via `NVIDIA_TEXT_MODEL` env var.

### Fix 4: LLM Returns Conversational JSON (JSON Extraction)
**Problem:** The text model sometimes wraps the JSON in conversational text or markdown fences (e.g., "Here is your diagnosis: ```json {...} ```"), causing Jackson to fail parsing.  
**Fix:** Added `extractJson()` helper that finds the first `{` and last `}` in the raw response and slices out only the JSON block before parsing.

### Fix 5: Physical Device Cannot Reach Backend
**Problem:** `localhost` on the phone refers to the phone itself, not the development computer.  
**Fix:** `LoadingScreen.tsx` reads `Constants.expoConfig?.hostUri` to extract the LAN IP that Metro Bundler is serving on (e.g., `192.168.0.101`) and constructs the API URL dynamically.

### Fix 6: Expo SDK Version Mismatch
**Problem:** Device's Expo Go was running SDK 54; the project was initialized with a newer SDK.  
**Fix:** Downgraded all Expo packages in `package.json` to align with SDK 54 compatibility targets.

---

## 12. Phases & Roadmap

| Phase | Description | Status |
|---|---|---|
| **Phase 1** | Backend skeleton — entities, repositories, controller, Spring Boot setup | ✅ Done |
| **Phase 2** | Database seeding — Flyway migrations, CSV disease data | ✅ Done |
| **Phase 3** | `/api/diagnose` endpoint — Vision AI + DB matching + Text synthesis | ✅ Done |
| **Phase 4** | React Native frontend — 4 screens (Home, Loading, Results, Error) | ✅ Done |
| **Phase 5** | Follow-up chat — in-app chat to ask questions about the diagnosis | ⏳ Next |
| **Phase 6** | Polish — animations, history screen, improved error handling | ⏳ Planned |

---

## Working Confirmed

The end-to-end flow was verified working on a physical iOS device:

- **Input:** Photo of a Hibiscus plant with white cottony substance on leaves
- **Output:** Correct identification of **Mealybugs** with actionable treatment advice
- **Confidence:** "High, direct match with symptoms and solution."
