# MindMirrorAI — Implementation Plan

> **Repository:** `Bavasurthi05/MindMirrorAI`
> **Created:** 2026-07-24
> **Scope:** Build the platform from skeleton to functional product.

## Current State
- **Frontend:** UI complete, mock data only (no API client, no auth context, no data fetching).
- **Backend:** Hexagonal skeleton. `AuthUseCase` interface unimplemented, no JWT filter, only `/auth/health`.
- **ML service:** `/health` only.
- **Database:** `roles` + `users` only; schema/entity mismatch.

## Critical Blockers
1. Duplicate main class (`com.project` + `com.project.mentalhealth`).
2. `User` entity vs `init.sql` schema mismatch (`ddl-auto=validate` breaks startup).
3. JWT filter not wired into Spring Security.

---

## Phase 0 — Fix Blockers & Foundations
- Remove duplicate main class; keep `com.project.mentalhealth.MentalHealthBackendApplication`.
- Add Flyway (`flyway-core`, `flyway-mysql`); convert schema to versioned migrations matching JPA model.
- Add `Role` entity + `role_id` FK on `User`.

## Phase 1 — Backend Authentication
- `JwtTokenProvider` + `JwtAuthenticationFilter`; BCrypt `PasswordEncoderPort` adapter.
- Implement `AuthService` (register/login); expand `AuthController` with register/login/refresh/logout/verify-email/forgot-password/reset-password.
- Refresh, email-verification, password-reset token tables + entities; `EmailSenderPort` (dev no-op).

## Phase 2 — Frontend Integration Layer
- `src/lib/api.ts` HTTP client with JWT attach/refresh interceptor.
- `AuthContext` + `useAuth`; enforce route gating in `ProtectedLayout`/`AdminLayout`.
- React Query for server state; wire auth pages to endpoints.

## Phase 3 — Core Domain Features
For each: migration → entity + repo → ports → service → v1 controller → connect frontend page.
1. Journal, 2. Questionnaire, 3. Mood tracking, 4. Triggers, 5. Recovery plans, 6. Reports.
Replace `/prediction-results`, `/weekly-insights`, `/profile`, `/settings` placeholders.

## Phase 4 — ML Service
- Endpoints: `/analyze/journal`, `/predict/mood`, `/detect/triggers`, `/insights/weekly`.
- SHAP/LIME explainability payloads; backend `MlServiceClient` outbound port.

## Phase 5 — Admin & RBAC
- `@PreAuthorize` role checks; admin analytics endpoints → AdminDashboardPage.

## Phase 6 — Cross-cutting Hardening
- CORS, externalized secrets, tests (JUnit/MockMvc, Vitest, pytest), CI, Dockerized frontend, observability, rate limiting.

---

## Verification
1. Backend boots on migrated schema; `/actuator/health` = UP.
2. Register → login returns JWT; protected routes reject invalid tokens.
3. Frontend login persists session; protected pages redirect; Journal CRUD hits real API.
4. ML endpoints return predictions + explanations.
5. `docker compose up --build` starts all four services.
6. Test suites green.

## Assumptions
- Flyway migrations, React Query, JWT access + refresh tokens.
- Dev no-op email adapter; lightweight ML baselines first.

---

# Gap-Closure Plan (rev. 2026-07-27)

> Phases 0–3 of the original plan are largely **DONE**. Auth, Journal, Questionnaire, Mood,
> Trigger, Recovery, Report, Analysis, and Admin overview are implemented with real API wiring
> (React Query + JWT), Flyway migrations `V1–V6`, and a FastAPI ML service exposing
> `/analyze/journal`, `/predict/mood`, `/detect/triggers`, `/insights/weekly`.
>
> This revision maps the **remaining gaps against the MindMirror AI design document** and
> sequences the work to close them. ML approach: **real but lightweight** (TF-IDF + Random
> Forest + SHAP) with a pluggable interface so XLNet/RoBERTa transformers can be dropped in later.

## Design-doc coverage snapshot

| Module | State | Gap to close |
|--------|-------|--------------|
| 1 Authentication | 🟡 | Real **Profile** page (view/edit, avatar, change password) |
| 2 Dashboard | 🟢 | Confirm widgets bind to real data; add streak + goals widgets |
| 3 Assessment | 🟡 | Add **Social Media Input** as 3rd analyze method |
| 4 AI Prediction | 🟡 | Replace heuristics with **TF-IDF + Random Forest**; pluggable XLNet |
| 5 Explainable AI | 🟡 | **SHAP** feature contributions + feature-breakdown UI |
| 6 Trigger Detection | 🟢 | Optionally upgrade to model-backed extraction |
| 7 Virtual Mirror | 🟡 | Bind radar/circular/progress to **real metrics**, not dummy values |
| 8 Recovery Center | 🟢 | — |
| 9 Analytics Dashboard | 🟡 | **Heatmap calendar, Emotion timeline, data-driven radar, dedicated Analytics page** |
| 10 Reports | 🟡 | **PDF export** (client jsPDF and/or server-side) |
| 11 Admin Panel | 🟡 | **User CRUD, dataset monitoring, model-accuracy view, user feedback review** |
| ML/DL module | 🔴 | Text cleaning, tokenization, TF-IDF, Random Forest + **accuracy comparison**; RoBERTa/XLNet pluggable; SHAP |
| Standout features | 🔴 | **Dark/Light theme, PDF export, mood streak, weekly goals, journal reminders, emergency help, feedback & rating** |

---

## Phase A — Real ML & Explainable AI (core innovation)
**ml-service/**
- `preprocessing.py`: text cleaning (lowercase, URL/mention/emoji strip, stopwords), tokenization.
- `models/`: train + persist a **TF-IDF + RandomForestClassifier** pipeline for mental-health
  state prediction; ship a small seed dataset + `train.py`; save with `joblib`.
- **Accuracy comparison**: `train.py` reports RF vs. a baseline (e.g., LogisticRegression/NB)
  metrics to `models/metrics.json`; expose via `GET /models/metrics`.
- **SHAP**: wrap the pipeline with a SHAP explainer; return per-feature contributions from
  `/analyze/journal` (extend `JournalAnalysisResponse.explanation` with feature weights + a
  normalized "reason" breakdown, e.g. `sleep 42%, fear 31%`).
- **RoBERTa/XLNet pluggability**: define `EmotionModel`/`PredictionModel` protocols in
  `analysis.py`; keep lexicon as default, add `transformers`-backed impls behind an env flag
  (`ML_BACKEND=heuristic|transformer`) so heavy weights are opt-in.
- New endpoint `POST /analyze/social` (reuse journal pipeline with source tag).
- Tests in `tests/` for preprocessing, prediction, SHAP shape, trigger extraction.

**backend/**
- Extend `MlAnalysisPort` + `MlServiceClient` for the new SHAP/feature payload and
  `/analyze/social`; surface via `AnalysisService`/`AnalysisController`.

## Phase B — Social Media Input (assessment method 3)
- Backend: `AnalyzeJournalRequest` variant or `AnalyzeSocialRequest` (source = social) →
  ML `/analyze/social`; persist as an `AssessmentSubmission` with `source` column (migration `V7`).
- Frontend: new **Social Media** tab/page (route `/assessment/social` or a segmented control on
  the assessment area) with paste box + "Analyze"; render sentiment/emotion/triggers + SHAP reasons.
- Add `lib/analysis.ts` mutation `useAnalyzeSocial`.

## Phase C — Missing visualizations & Analytics Dashboard
- Bind **MirrorPage** radar/circular/progress to real metrics from mood/analysis endpoints
  (replace `radarValues`/`circularStats` constants).
- New **`/analytics`** page (replace nothing existing; add to nav) containing:
  - **Radar** (react-chartjs-2 Radar) for Stress/Confidence/Sleep/Motivation/Social/Happiness.
  - **Heatmap calendar** (custom grid, green→red per-day wellness) — new `Heatmap` component.
  - **Emotion timeline** (horizontal emoji/color strip) — new `EmotionTimeline` component.
  - Weekly trend (line) + weekly stress (bar) + wellness **progress ring**.
- Backend: `AnalyticsController` (`GET /analytics/overview`) aggregating mood, emotion,
  trigger, and wellness series; DTOs for radar/heatmap/timeline.
- Implement **Weekly Insights** page (replace placeholder) bound to `/insights/weekly`.

## Phase D — Reports & PDF export
- Frontend: add `jspdf` + `html2canvas`; "Export PDF" on **ReportPreviewPage** rendering
  prediction → emotion scores → mirror → recommendations → progress (per design contents).
- Optional backend: `GET /reports/{id}/pdf` server-side generation for shareable links.

## Phase E — Standout UX features
- **Dark/Light theme**: Tailwind `darkMode: 'class'`, `ThemeContext` + toggle in `TopNav`;
  persist to `localStorage`; audit key surfaces for `dark:` variants.
- **Mood streak counter**: backend derive consecutive journaling days
  (`GET /me/streak`); dashboard widget.
- **Weekly wellness goals**: `goals` table (migration `V8`), `GoalController` CRUD + progress;
  dashboard/goals widget.
- **Daily journal reminder**: client-side scheduled notification (Notifications API) with
  opt-in in Settings; (server push out of scope initially).
- **Emergency Help section**: static, clearly-informational helpline resources component +
  route `/help`; visible from nav/footer. **Not** crisis diagnosis — disclaimer required.
- **Feedback & Rating**: `feedback` table (migration `V9`), `FeedbackController`
  (`POST /feedback`), `/feedback` page; surface in Admin.
- Implement real **Profile** and **Settings** pages (replace placeholders): profile edit,
  change password, theme + notification preferences.

## Phase F — Admin panel completeness
- **User management**: `GET/PATCH/DELETE /admin/users` (enable/disable, role change) → users table UI.
- **Dataset monitoring**: submission counts/trends → admin widget (reuse analytics aggregation).
- **Model accuracy**: proxy ML `GET /models/metrics` via `GET /admin/model-metrics`; show RF vs
  baseline comparison chart (fulfils design "Accuracy Comparison").
- **User feedback review**: list/triage submitted feedback.
- Replace `/admin/analytics` placeholder with a real analytics view.

## Phase G — Hardening & tests
- Vitest for new components (Heatmap, EmotionTimeline, theme toggle, PDF trigger).
- MockMvc tests for new controllers (analytics, admin users, feedback, goals).
- pytest for ML preprocessing/model/SHAP.
- CORS review for any new endpoints; secrets externalized; update `docs/ANALYSIS.md`.

## Suggested sequencing
1. **Phase A** (ML + SHAP) — unblocks accurate data for prediction, explainable AI, model accuracy.
2. **Phase C** (visualizations) — consumes real ML/analytics output.
3. **Phase B** (social input) + **Phase D** (PDF) — user-facing assessment/report completeness.
4. **Phase F** (admin) — depends on A (metrics) and analytics aggregation.
5. **Phase E** (standout UX) — parallelizable, low coupling.
6. **Phase G** (tests/hardening) — continuous.

## New verification (delta)
7. `/analyze/journal` returns SHAP feature contributions; `/models/metrics` shows RF vs baseline.
8. Social Media input produces analysis and persists a submission.
9. Analytics page renders radar + heatmap + emotion timeline from real data; Mirror is data-driven.
10. Report exports a valid PDF matching the design contents.
11. Theme toggle persists; streak + goals + feedback + emergency-help + profile pages functional.
12. Admin can manage users, view model accuracy comparison, and review feedback.
