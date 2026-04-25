# Part 8: API Contracts and Backend Service Design

## 1) Purpose
Define stable Spring Boot API contracts and backend service boundaries for Phase 1, aligned with:
- schema contract in Part 2,
- training/scoring contract in Part 5,
- release gates in Part 1.

Python jobs remain internal. Spring Boot is the only public backend interface.

---

## 2) Service Boundary and Ownership

### Public boundary (Spring Boot)
- Auth/authz
- Request validation
- Import trigger/status APIs
- Recommendation retrieval APIs
- Feedback submission APIs
- Response shaping and error semantics

### Internal boundary (Python jobs)
- Ingestion workers (if delegated)
- Feature/embedding generation
- Train/evaluate/score batch jobs

### Contract rule
Spring Boot does not call Python at request time. It reads persisted outputs from Postgres.

---

## 3) API Surface (Phase 1)

## 3.1 Import API
- `POST /v1/imports/anilist`
  - Trigger import for authenticated user or admin-targeted user.
- `GET /v1/imports/{importRunId}`
  - Retrieve import run status and summary.

## 3.2 Recommendations API
- `GET /v1/recommendations`
  - Return current active recommendations for authenticated user.
- `GET /v1/recommendations/versions/{modelVersion}` (internal/admin optional)
  - Return recommendations for explicit model version for debugging/A-B checks.

## 3.3 Feedback API
- `POST /v1/feedback/recommendations`
  - Submit feedback (`interested`, `not_interested`, `already_consumed`) for a recommended item.

---

## 4) DTO Contracts

## 4.1 Import Trigger Request
```json
{
  "anilistUsername": "string",
  "forceRefresh": false
}
```

Validation:
- `anilistUsername`: 2-64 chars, allowed charset policy.
- `forceRefresh`: optional boolean, default `false`.

## 4.2 Import Status Response
```json
{
  "importRunId": "uuid",
  "status": "queued|running|succeeded|failed|partial",
  "startedAt": "timestamp",
  "completedAt": "timestamp|null",
  "itemsProcessed": 0,
  "interactionsProcessed": 0,
  "errorCount": 0
}
```

## 4.3 Recommendation Response Item
```json
{
  "itemId": "long",
  "score": 0.0,
  "rank": 1,
  "modelVersion": "string",
  "generatedAt": "timestamp",
  "explanations": [
    {
      "code": "shared_genres",
      "message": "Because you liked dark fantasy titles with psychological themes.",
      "evidence": {
        "genres": ["Fantasy", "Psychological"]
      }
    }
  ]
}
```

## 4.4 Recommendations List Response
```json
{
  "userId": "long",
  "modelVersion": "string",
  "generatedAt": "timestamp",
  "items": []
}
```

## 4.5 Feedback Request
```json
{
  "itemId": 123,
  "feedbackType": "interested|not_interested|already_consumed",
  "reasonCode": "optional_string",
  "reasonText": "optional_string",
  "modelVersion": "string",
  "generatedAt": "timestamp"
}
```

Validation:
- `feedbackType` enum must match DB constraints.
- `itemId` must be valid catalog item.
- Optional reason size limits enforced.

---

## 5) Error Model

### Error envelope
```json
{
  "errorCode": "string",
  "message": "string",
  "details": {},
  "requestId": "string"
}
```

### Deterministic status mapping
- `400`: validation failure
- `401`: unauthenticated
- `403`: unauthorized
- `404`: resource not found
- `409`: conflict/idempotency violation
- `422`: semantically invalid request
- `429`: rate-limited
- `500`: unexpected server failure
- `503`: dependent service unavailable

### Canonical API error codes
- `INVALID_INPUT`
- `IMPORT_ALREADY_RUNNING`
- `IMPORT_SOURCE_UNAVAILABLE`
- `RECOMMENDATIONS_NOT_READY`
- `MODEL_VERSION_NOT_FOUND`
- `FEEDBACK_DUPLICATE_SUBMISSION`

---

## 6) Version Selection Rules for Recommendations
- Default behavior: read active promoted `model_version`.
- Internal override enabled only for privileged callers.
- If override version has no active rows for user, return `RECOMMENDATIONS_NOT_READY` with `404` or `409` based on policy.

---

## 7) Pagination, Sorting, and Stability
- Primary recommendation endpoint returns top-N ordered by `rank_position` ascending.
- Stable tie-break from scoring output is preserved.
- Optional `limit` (bounded; default 20, max 100).

---

## 8) Idempotency and Concurrency Rules
- Import trigger endpoint supports idempotency key header.
- Feedback endpoint dedupes by (`user_id`,`item_id`,`feedback_type`,`modelVersion`,`generatedAt`,`time_bucket`) policy.
- Concurrent import requests for same user:
  - either queue latest, or return conflict while run is active.

---

## 9) Caching and Latency Strategy
- Cache read-through for recommendation payload per user/model version (short TTL).
- Invalidate on activation of new recommendation generation.
- Do not cache feedback writes.

MVP performance target:
- Recommendation endpoint P95 under agreed budget for expected internal cohort.

---

## 10) Security and Access
- Auth required for user endpoints.
- Admin scope required for explicit model-version override and cross-user imports.
- Audit log for admin operations and model-version override reads.

---

## 11) Compatibility Policy
- Backward-compatible response additions only (optional fields).
- Breaking DTO changes require new endpoint version prefix.
- Enum expansion policy documented and coordinated with clients.

---

## 12) Data Mapping to Part 2 Tables
- Import trigger/status -> `ingestion_runs`, `ingestion_errors`
- Recommendation reads -> `recommendations` + `items` + `item_metadata` (for display)
- Feedback writes -> `user_feedback`

---

## 13) Implementation Sequence
1. Define DTOs and validators.
2. Implement service layer and repository queries.
3. Add controller endpoints and error mapper.
4. Add auth guards and role-based restrictions.
5. Add contract tests.
6. Add latency instrumentation.

---

## 14) Exit Criteria
- Every endpoint is fully mapped to storage and constraints.
- Error semantics are deterministic and documented.
- Model-version default/override behavior is test-covered.
- API implementation can proceed without schema changes.
