# Part 9: Evaluation, Experimentation, and Model Governance

## 1) Purpose
Define a rigorous, repeatable offline evaluation framework and governance process for model promotion, rollback, and experiment traceability.

---

## 2) Evaluation Protocol

## 2.1 Split Strategy
Primary strategy (Phase 1 default):
- time-based split per user where feasible.

Fallback:
- leave-one-out for sparse users that cannot satisfy time-based constraints.

Rule:
- same split protocol must be used for baseline and candidate runs in a comparison set.

## 2.2 Metric Set
Required:
- `Recall@10`
- `NDCG@10`

Optional diagnostics:
- user coverage %
- recommendation novelty proxy
- segment-level performance (cold-start vs dense-history users)

---

## 3) Baseline Definitions

Minimum required baselines:
- popularity baseline
- co-occurrence baseline

Comparison policy:
- candidate model must report lift vs at least one baseline under identical split/evaluation config.

---

## 4) Experiment Metadata Requirements

Every evaluation run must store:
- `run_id`
- `model_family` and `model_version`
- algorithm hyperparameters
- feature/embedding version lineage
- weighting/decay config
- `data_snapshot_hash`
- split protocol id/version
- metric results and confidence notes

Storage targets:
- `model_runs`
- optional linked `model_artifacts`

---

## 5) Promotion Policy

Promotion preconditions:
1. run status `succeeded`
2. metrics complete and valid
3. candidate lift meets thresholds from Part 1
4. no data quality gate violations
5. recommendation coverage threshold met

Suggested threshold policy:
- Recall@10 relative lift >= configured minimum
- NDCG@10 relative lift >= configured minimum

If one metric regresses:
- no auto-promotion
- route to manual review workflow

---

## 6) Rollback Policy

Rollback triggers:
- post-promotion quality regression
- data integrity issue in recommendation outputs
- severe operational instability

Rollback steps:
1. deactivate current model version
2. reactivate last known-good version
3. record rollback reason and operator
4. open investigation incident with run linkage

Rollback must be reversible and auditable.

---

## 7) Manual Override Process

Manual promotion/rollback allowed only for authorized operators.

Audit requirements:
- operator identity
- timestamp
- justification note
- linked run ids and metrics snapshot

Override constraints:
- cannot bypass failed data integrity checks
- must preserve traceable history in governance tables/logs

---

## 8) Regression Investigation Workflow

1. Detect regression via scheduled eval or production KPI monitor.
2. Compare candidate vs baseline and prior promoted model.
3. Check data/feature drift indicators.
4. Validate weighting and exclusion config changes.
5. Decide rollback, hotfix, or retrain path.

Required artifacts for investigation:
- metrics delta report
- run config diff
- data snapshot diff summary

---

## 9) Scheduling and Cadence
- Evaluate every train run.
- Run periodic benchmark suite (weekly or on major data shift).
- Re-run baselines on schedule to keep reference current.

---

## 10) Implementation Sequence
1. Implement evaluation runner with fixed protocol IDs.
2. Implement metric calculator and baseline comparators.
3. Persist run metadata and results.
4. Implement promotion gate checker.
5. Implement rollback utility and audit logging.
6. Add regression report generation.

---

## 11) Exit Criteria
- Evaluation protocol is fixed, documented, and reproducible.
- Promotion/rollback criteria are objective and enforceable.
- Every model lifecycle decision is fully traceable.
- Manual overrides are constrained and auditable.
