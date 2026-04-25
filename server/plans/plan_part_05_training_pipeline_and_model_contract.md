# Part 5: Training Pipeline and Model Contract

## 1) Purpose
Define the exact training/scoring lifecycle and stable contract between data persistence, Python jobs, and Spring Boot serving.

Phase 1 uses LightFM as baseline, but this document enforces a model-agnostic interface so future algorithms can plug in without API or schema redesign.

---

## 2) Pipeline Goals

1. Produce reproducible model runs from canonical data snapshots.
2. Generate top-N recommendations in batch and persist them for API serving.
3. Keep runtime serving fully decoupled from Python execution.
4. Preserve algorithm swapability through stable inputs/outputs.
5. Ensure failed runs do not corrupt active recommendation sets.

---

## 3) Operating Model

Batch-only model:
- Training, scoring, and evaluation run as internal Python jobs.
- Spring Boot serves persisted recommendation outputs only.
- Promotion to active recommendation version is explicit and reversible.

Recommended initial cadence:
- Scoring: daily (or more frequently if operations budget allows).
- Retraining: weekly (or manual trigger when large ingestion delta).
- Evaluation: every train run, plus periodic benchmark reruns.

---

## 4) Stable Contract Specification

## 4.1 Train Contract

### Inputs
- Interaction dataset from `user_item_interactions`:
  - `user_id`, `item_id`, `status`, `progress`, `rating`, `interaction_timestamp`, `source`
- Item metadata features from `item_metadata`
- Embedding features from `item_embeddings` (active embedding version)
- Train config payload:
  - algorithm family
  - hyperparameters
  - weighting strategy
  - split policy (for eval-enabled train runs)
  - filtering rules

### Outputs
- `model_runs` record with:
  - `run_id`, `model_family`, `model_version`, config snapshot, data snapshot hash, status
- Artifact pointers in `model_artifacts` (optional but recommended)
- Training/evaluation metrics persisted in `model_runs.metrics_json`

### Contract Guarantees
- Deterministic run metadata given same snapshot + config.
- No overwrite of prior run artifacts.
- Clear terminal status: `succeeded`, `failed`, or `cancelled`.

---

## 4.2 Score Contract

### Inputs
- Promotable model version from prior successful run
- Eligible users:
  - users with sufficient data and/or cold-start eligibility rules
- Candidate items:
  - active items passing exclusion filters
- Score config payload:
  - top-N value
  - exclusion rules
  - diversification toggles (if enabled)

### Outputs
- Rows in `recommendations` with:
  - `user_id`, `item_id`, `score`, `rank_position`
  - `model_family`, `model_version`
  - `generated_at`
  - `explanation_inputs`
  - `is_active`
- `model_runs` status update for score run and run metrics

### Contract Guarantees
- Idempotent rerun behavior for same `run_id`/generation key.
- Write path prevents duplicate rows under uniqueness constraints.
- Active set switch happens only after full run success.

---

## 5) Data Extraction and Feature Assembly

### 5.1 Extraction Window
- Snapshot interaction data at run start.
- Record `data_snapshot_hash` from source query inputs and row counts/checksums.

### 5.2 Feature Assembly
- Build sparse feature matrix from structured metadata (genres/tags/studios/authors/format).
- Join dense text embeddings from active embedding model/version.
- Validate feature dimensions and missingness thresholds before training.

### 5.3 Data Quality Gates
Fail fast if:
- required columns missing/null above threshold,
- embedding coverage below minimum threshold for active items,
- duplicate canonical item identities detected.

---

## 6) Interaction Weighting Strategy

Weighting is applied in pipeline, not persisted destructively.

### 6.1 Default Weight Map (Phase 1 Initial)
- `completed`: +1.00
- `in_progress`: +0.70
- `planned`: +0.30
- `paused`: +0.15
- `dropped`: -0.80

### 6.2 Rating Adjustment
- If rating exists, add normalized adjustment in bounded range (for example +/-0.25).
- Keep total weight clamped to configured min/max.

### 6.3 Progress Adjustment
- Use progress ratio where available to gently scale `in_progress`.
- Never exceed `completed` weight by progress scaling alone.

### 6.4 Recency Decay
- Exponential decay by event age with configurable half-life.
- Store half-life and decay function identifier in run config for reproducibility.

### 6.5 Source Priority
- `explicit_feedback` can override or augment imported status signal.
- Rule resolution order documented in config schema.

---

## 7) Training Flow (LightFM Baseline)

1. Start run; create `model_runs` row with status `running`.
2. Extract snapshot datasets and compute `data_snapshot_hash`.
3. Build interaction matrix and item feature matrix.
4. Apply weighting transforms and validation gates.
5. Train LightFM baseline with configured hyperparameters.
6. Evaluate on configured split protocol.
7. Persist metrics and artifact pointers.
8. Mark run `succeeded` or `failed` with reason.

Required metadata captured per run:
- feature set version
- embedding model/version
- weighting config
- random seed
- training duration

---

## 8) Batch Scoring Flow and Filtering Rules

1. Select successful, promotable model version.
2. Load model artifact and candidate universe.
3. Apply exclusion rules before ranking:
   - already consumed/completed items,
   - hard-blocked items from negative feedback,
   - inactive catalog items.
4. Generate ranked candidates per user.
5. Enforce top-N and deterministic tie-break policy.
6. Write recommendations into staging scope.
7. Validate row counts/ranks.
8. Activate new generation atomically; deactivate prior generation.

Optional Phase 1.5 extension:
- Light diversification by media subtype or tag entropy threshold.

---

## 9) Artifact and Version Management

Version schema recommendation:
- `model_family`: `lightfm`
- `model_version`: semantic + run stamp, e.g. `lightfm_v1.0.0_run_20260425_001`

Artifact pointers:
- model binary path/URI
- feature encoder metadata
- config snapshot JSON
- checksum and size

Promotion policy:
- only successful runs with valid metrics and completeness checks are promotable.

---

## 10) Model Governance

### 10.1 Required Run Logging
Every run must capture:
- config snapshot
- data snapshot identifier/hash
- metrics
- status and timing
- operator or trigger source

### 10.2 Promotion Criteria
Promote candidate version only if:
- run status `succeeded`,
- metrics available and pass threshold policy,
- recommendation completeness checks pass,
- no critical validation failures.

### 10.3 Rollback Policy
If regression or bad data is detected:
- deactivate current version,
- reactivate last known-good version,
- record rollback event in run notes/status transitions.

---

## 11) Failure Handling and Recovery

Failure classes:
- data extraction failure
- feature assembly failure
- training convergence/runtime failure
- scoring write failure
- activation switch failure

Recovery semantics:
- retries for transient failures with bounded attempts,
- idempotent reruns keyed by run identifiers,
- no partial active-set switch on failure,
- dead-letter/manual intervention path for repeated hard failures.

Runbook minimums:
- error codes and actionable remediation guidance,
- commands/scripts to rerun from safe checkpoint,
- verification checklist before reactivation.

---

## 12) Scheduling and SLAs

Scheduling:
- cron or scheduler-triggered Python CLI jobs for Phase 1.
- manual admin trigger supported for emergency reruns.

SLA definitions (to tune in ops plan):
- train job completion window target,
- score job completion window target,
- freshness SLA from ingestion completion to available recommendations.

Operational alerts:
- failed run status,
- stale recommendation generation age,
- large drop in recommendation coverage.

---

## 13) Contract Tests and Reproducibility

### 13.1 Contract Tests
- Validate input query schemas and expected columns.
- Validate output schema and DB constraints for recommendations.
- Validate rank continuity and uniqueness per user/model/generation.

### 13.2 Reproducibility Tests
- Re-run training with identical snapshot + config + seed and compare metric drift tolerance.
- Verify model run metadata fully reconstructs data/feature selection and weighting behavior.

### 13.3 Integration Tests
- End-to-end: ingestion snapshot -> train -> score -> API read path.
- Ensure Spring Boot serves latest promoted recommendation version correctly.

---

## 14) Cross-Component Contract Matrix

| Producer | Contract Artifact | Consumer | Validation |
|---|---|---|---|
| Ingestion workflow | `user_item_interactions`, `items`, `item_metadata` | Train job | schema + quality gate |
| Embedding job | `item_embeddings` | Train job | embedding coverage/dim checks |
| Train job | `model_runs`, `model_artifacts` | Score job | run status/version checks |
| Score job | `recommendations` | Spring Boot API | schema, completeness, active set checks |
| Feedback API | `user_feedback` | Future train job | ingestion + transformation checks |

---

## 15) Open Decisions and Defaults

Open decisions:
1. Final decay half-life value by media type.
2. Negative feedback override semantics priority over imported statuses.
3. Whether to include per-user candidate pre-filter caps for performance.

Phase 1 defaults:
- Single global decay half-life.
- Explicit negative feedback treated as strongest suppression signal.
- Candidate caps enabled only if scoring runtime exceeds SLA.

---

## 16) Exit Criteria
- Train and score contracts are explicit enough for independent Spring/Python implementation.
- Failed runs cannot corrupt active recommendation sets.
- Reproducibility path exists for every successful run.
- Contract fields map cleanly to Part 2 schema and Part 1 release gates.
