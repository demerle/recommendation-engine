# Part 11: Testing Strategy and Quality Gates

## 1) Purpose
Define an actionable testing and release-gating strategy that prevents regressions across API contracts, schema evolution, data quality, model pipeline behavior, and recommendation outputs.

---

## 2) Test Pyramid by Component

### Unit tests
- normalization mappers
- weighting logic
- exclusion/filtering logic
- explanation reason selector/template rendering
- DTO validators and error mappers

### Integration tests
- repository and query correctness against Postgres
- ingestion upsert/idempotency behavior
- train/score contract serialization and persistence
- API endpoint behavior with real service wiring

### End-to-end tests
- import -> feature/embedding -> train -> score -> API read -> feedback
- model activation/rollback flow

---

## 3) Data Quality Test Suite
- status mapping validity tests
- null/malformed payload handling tests
- duplicate event prevention tests
- feature/embedding coverage threshold tests
- recommendation rank continuity and uniqueness tests

---

## 4) Contract Tests

### API contract tests
- request schema validation
- response schema stability
- error code/status determinism
- model-version default and override behavior

### Pipeline contract tests
- train input extraction columns and types
- score output schema (`model_version`, `generated_at`, rank fields)
- governance metadata persistence in `model_runs`

---

## 5) Deterministic Recommendation Tests
- fixed-seed deterministic scoring fixtures
- tie-break deterministic order tests
- explanation deterministic reason selection fixtures
- no-empty-recommendation behavior for eligible users

Tolerance policy:
- deterministic tests exact-match for order and reason codes.
- metric tests allow configured tolerance band.

---

## 6) Migration Verification (Flyway)
- migration up/down safety checks in CI (where rollback model allows)
- schema invariant checks after each migration wave
- data compatibility checks across wave boundaries
- seeded migration rehearsal in staging-like environment

---

## 7) CI Gate Policy

Pre-merge mandatory gates:
- unit tests
- API contract tests
- schema migration checks
- core integration tests
- lint/static analysis

Nightly/extended gates:
- full end-to-end pipeline suite
- evaluation regression benchmarks
- failure injection scenarios

Blocker policy:
- any pre-merge gate failure blocks merge.
- repeated nightly regressions trigger incident/ticket triage.

---

## 8) Release Quality Gates (Mapped to Part 1)
- import flow pass rate meets threshold
- recommendation availability and coverage checks pass
- evaluation metrics and promotion gates satisfied
- explanation payload validity pass rate at target
- operational reliability gates (from Part 10) pass

---

## 9) Test Data and Fixture Management
- canonical fixture sets for:
  - dense-history user
  - sparse-history user
  - cold-start user
- deterministic snapshot fixtures versioned with schema/contract changes
- sanitization rules for any real-derived datasets

---

## 10) Failure Triage Workflow
1. classify failure type: contract/data/model/infra.
2. identify first bad commit/build.
3. assign owner by component matrix.
4. define fix-forward path and retest scope.
5. record incident for recurring classes.

---

## 11) Implementation Sequence
1. Implement unit and contract test scaffolding.
2. Add integration tests for DB and service boundaries.
3. Add deterministic recommendation and explanation fixtures.
4. Add migration verification jobs.
5. Configure CI gate matrix (pre-merge/nightly).
6. Add release checklist automation.

---

## 12) Exit Criteria
- Test strategy is executable with clear gate ownership.
- CI gates reliably prevent contract/schema regressions.
- Release gates map directly to MVP success criteria.
- Deterministic fixtures cover critical ranking and explanation paths.
