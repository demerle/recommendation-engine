# Part 1: Product Scope and Success Criteria

## 1) Purpose
Define the exact Phase 1 product boundary, measurable success criteria, and release gates for the personalized anime/manga recommender MVP.

This document is implementation-facing. Every in-scope capability must map to buildable API/job behavior and testable acceptance checks.

---

## 2) Problem Statement and Target User

### Problem Statement
Users who consume anime/manga often receive generic recommendations that over-index on popularity and under-index on individual taste. The MVP must prove that user-history-aware recommendations, enriched with structured metadata and embeddings, outperform simple popularity/co-occurrence baselines.

### Target User
Primary user profile:
- Tracks or can import history from AniList.
- Wants recommendations that match nuanced taste.
- Is willing to provide lightweight feedback to improve future recommendations.

Secondary user profile:
- New user with minimal history but willing to import quickly.
- Needs usable cold-start recommendations with clear rationale.

---

## 3) Phase 1 Objective (Measurable)
Deliver a production-like MVP that ingests AniList history, stores canonical user/item/interaction data in Postgres, trains and batch-scores a model-agnostic recommendation pipeline with LightFM baseline, serves recommendations through Spring Boot APIs, captures explicit feedback, and demonstrates measurable offline quality lift over a non-personalized baseline.

---

## 4) In-Scope Capabilities

### 4.1 Account and Identity
- User account creation and login flow for MVP environment.
- Stable internal `user_id` used across ingestion, training, and recommendation storage.

### 4.2 AniList Ingestion
- Import anime/manga list using AniList username.
- Normalize and upsert catalog entries.
- Normalize interaction status values into internal enum.

### 4.3 Canonical Data Persistence
- Store users, items, raw interactions, and explicit feedback in Postgres.
- Preserve raw interaction events needed for future re-weighting.

### 4.4 Feature Layer (Metadata + Embeddings)
- Build item features from structured metadata.
- Generate pre-trained text embeddings from title/synopsis/tags.
- Persist feature/embedding outputs for training/scoring reuse.

### 4.5 Model Training and Scoring
- Use model-agnostic training/scoring contract.
- Implement LightFM baseline model in Phase 1.
- Generate top-N recommendations per user in batch mode.
- Write versioned recommendation outputs (`model_version`, `generated_at`) to Postgres.

### 4.6 API Delivery
- Expose API endpoints for:
  - AniList import trigger/status
  - recommendation retrieval
  - feedback submission
- Spring Boot is the only public backend service; Python remains internal batch subsystem.

### 4.7 Explanation Layer
- Return structured, human-readable explanation reasons grounded in interpretable metadata.
- Explanations may use embedding-derived similarity signals, but user-facing reasons must remain interpretable.

### 4.8 Offline Evaluation
- Evaluate baseline and candidate runs using Recall@K and NDCG@K.
- Persist evaluation outcomes for model comparison.

---

## 5) Out of Scope (Phase 1)
- NovelUpdates integration.
- Dedicated manhwa ingestion pipeline.
- Graph database adoption.
- Vector database adoption.
- Real-time online training.
- Request-time Python inference service.
- Social features.
- Conversational free-text recommendation UX.
- Managed recommender platforms as production dependency (may be benchmarked later, not implemented in Phase 1).

Rationale: these items increase operational and design complexity without improving MVP validation confidence for the Phase 1 hypothesis.

---

## 6) MVP User Journeys

### 6.1 Primary Journey: Imported Personalization
1. User creates account.
2. User submits AniList username.
3. System imports list and confirms completion status.
4. Training/scoring pipeline runs on schedule or admin trigger.
5. User requests recommendations.
6. API returns ranked items and explanation reasons.
7. User submits explicit feedback.
8. Feedback is stored for future training runs.

### 6.2 Edge Journey: Sparse History
1. User has minimal imported data.
2. System still returns recommendations using available content features and global priors.
3. User feedback is captured to improve future relevance.

### 6.3 Edge Journey: Re-import
1. Existing user re-runs import.
2. Ingestion is idempotent and updates interactions without duplicating records.
3. Next training run uses refreshed snapshot.

---

## 7) Acceptance Criteria by Capability

### 7.1 Ingestion Acceptance
- Given valid AniList username, system imports user list and reports success.
- Duplicate import requests for same user are idempotent at data layer.
- Invalid username or API failure paths return actionable failure states and do not corrupt existing data.

### 7.2 Data Integrity Acceptance
- Required entities (`users`, `items`, `user_item_interactions`) are persisted with non-null required fields.
- Raw status/progress/rating/timestamp/source fields are retained.
- Item canonical identifiers are unique under defined constraints.

### 7.3 Training/Scoring Acceptance
- Batch training job completes and produces model metadata and run record.
- Batch scoring job persists top-N recommendations for eligible users.
- Every recommendation row includes `model_version` and `generated_at`.
- Failed runs do not overwrite active recommendation set.

### 7.4 API Acceptance
- Recommendation endpoint returns deterministic schema with ranking score and explanation payload.
- Feedback endpoint persists user choice and reason (if provided).
- API serves recommendations from Postgres without Python runtime dependency.

### 7.5 Evaluation Acceptance
- At least one baseline method and one LightFM run are evaluated on same split protocol.
- Recall@K and NDCG@K are recorded and comparable across runs.
- Candidate model promotion is blocked unless promotion criteria are met.

---

## 8) Success Metrics

## 8.1 Product/Flow Metrics
- Import success rate: >= 95% for valid AniList usernames in internal test cohort.
- Recommendation availability: >= 99% of eligible users have non-empty recommendation set after scoring cycle.
- Feedback capture rate: >= 20% of recommendation sessions include at least one feedback event in internal pilot.

### 8.2 Recommendation Quality Metrics
- Evaluation metrics: Recall@10 and NDCG@10.
- Baseline: popularity/co-occurrence recommender trained on same data split.
- Initial quality target:
  - LightFM Recall@10 >= baseline Recall@10 + 10% relative lift
  - LightFM NDCG@10 >= baseline NDCG@10 + 5% relative lift

If one metric passes and one fails, model is not auto-promoted; run enters manual review.

### 8.3 Operational Metrics
- Scheduled job completion rate: >= 95% over rolling 14 days.
- Median scoring cycle duration within agreed operations budget (set in Part 10 runbook).
- Time from completed import to next available recommendations bounded by documented cadence.

---

## 9) Non-Functional Constraints

### 9.1 Latency and Serving
- Recommendation API should return within operationally acceptable latency for MVP workloads.
- No runtime dependence on Python scoring service.

### 9.2 Freshness
- Retraining and scoring are batch processes with explicit cadence.
- Freshness SLA must be published (for example daily scoring, weekly retraining unless manually triggered).

### 9.3 Reliability and Recoverability
- Job retries and rerun semantics must be idempotent.
- Failures must preserve last known-good recommendation set.

### 9.4 Security and Compliance (MVP level)
- Store only required user data for functionality.
- Protect credentials/tokens using environment-based configuration and least privilege.

---

## 10) Release Gates (MVP Exit)
All gates must pass before Phase 1 is considered implementation-complete:

1. **Functional Gate**
   - Import, recommendation retrieval, and feedback submission APIs are operational end-to-end.
2. **Data Gate**
   - Core schema and constraints are enforced; idempotent ingestion verified.
3. **Model Gate**
   - LightFM baseline run is reproducible; recommendations are persisted with model versioning.
4. **Quality Gate**
   - Offline metrics are computed and compared against baseline.
5. **Stability Gate**
   - Batch jobs complete at expected reliability; failure recovery path documented and tested.

---

## 11) Risks, Assumptions, and Decision Log

### 11.1 Assumptions
- AniList API remains sufficiently available and stable for MVP ingestion.
- Imported histories are representative enough for evaluation.
- Pre-trained embeddings add usable signal for sparse-user scenarios.

### 11.2 Key Risks
- Sparse or noisy interaction data may reduce measurable quality lift.
- Metadata inconsistency may degrade feature quality.
- Operational issues in scheduled jobs may reduce recommendation freshness.

### 11.3 Mitigations
- Preserve raw events for iterative weighting improvements.
- Add data quality checks and schema constraints.
- Maintain model versioned outputs and rollback workflow.

### 11.4 Decision Log Entries
- Use LightFM as baseline, not permanent algorithm commitment.
- Keep serving path batch-only via Postgres handoff.
- Require interpretable explanations even when embeddings are used.

---

## 12) Traceability to Other Plan Parts
- Part 2 must provide storage structures to satisfy all acceptance criteria above.
- Part 5 must define train/score contracts that make these release gates enforceable.
