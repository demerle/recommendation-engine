# Part 4: Feature Engineering and Embeddings

## 1) Purpose
Define reproducible feature/embedding pipelines that convert normalized catalog data into model-ready item representations for training and scoring.

---

## 2) Feature Taxonomy

### 2.1 Categorical Features
- `media_type`
- `format`
- release era bucket

### 2.2 Multi-label Features
- `genres`
- `tags`
- `studios`
- `authors`

### 2.3 Numeric/Boolean Features
- episode/chapter counts (bucketed)
- completion status availability flags
- popularity proxy fields (if available and allowed)

### 2.4 Text-Derived Dense Features
- embedding vector from text recipe:
  - `canonical_title + synopsis + tags`

---

## 3) Preprocessing Pipeline

Steps:
1. Extract active items and metadata snapshot.
2. Normalize missing/null values to stable defaults.
3. Canonicalize token lists (trim, lowercase policy, dedupe).
4. Build sparse feature matrix for categorical/multi-label fields.
5. Prepare text corpus and generate dense embeddings.
6. Persist outputs and provenance metadata.

Reproducibility requirements:
- deterministic preprocessing config version
- fixed tokenization and normalization settings per feature version

---

## 4) Embedding Model Selection and Versioning

Phase 1 default:
- pre-trained sentence embedding model with good semantic coverage for synopsis/tag text.

Required provenance fields (Part 2 `item_embeddings`):
- `embedding_model`
- `embedding_version`
- `embedding_dim`
- `text_signature`
- `generated_at`

Version policy:
- bump `embedding_version` when model or text recipe changes.
- maintain at least one active embedding version for train/score jobs.

---

## 5) Embedding Generation and Refresh Cadence

Generation triggers:
- new item inserted
- metadata text fields changed (text signature mismatch)
- scheduled refresh cycle
- explicit backfill admin trigger

Cadence defaults:
- daily incremental refresh for changed/new items
- full refresh only on major model/version change

---

## 6) Persistence Contract

Write targets:
- sparse/metadata features persisted in `item_metadata`
- dense embeddings persisted in `item_embeddings`

Read targets:
- training job reads active embedding version and current metadata snapshot.
- scoring job uses same feature version lineage as selected model version.

Compatibility rule:
- a model version must declare which feature/embedding version it expects.

---

## 7) Quality Gates and Coverage Thresholds

Pre-train gates:
- minimum embedding coverage for active catalog (e.g., >= 95%).
- no unresolved schema-missing required fields.
- embedding vector dimensions match declared `embedding_dim`.

Quality metrics to record:
- embedding coverage %
- text parse success %
- null synopsis ratio
- mean vector norm sanity bounds

Gate behavior:
- hard-fail train run if coverage is below threshold unless explicit override.

---

## 8) Missing Embedding Fallback Behavior
- If embedding missing for item:
  - include item in sparse-feature-only path when allowed by config.
  - mark missing embedding flag in feature assembly metadata.
- If missing rate exceeds threshold:
  - block training and raise operational alert.

---

## 9) Backfill and Change Detection

Change detection:
- compute deterministic `text_signature` from recipe inputs.
- regenerate embedding only when signature changes or version updates.

Backfill strategy:
- process items in batches with checkpointing.
- allow resume from checkpoint on failure.
- report progress and error counts by batch.

---

## 10) Cost and Performance Controls
- Batch embedding inference to maximize throughput.
- Cap memory footprint with chunked processing.
- Parallel workers tuned by environment profile.
- Distinguish interactive admin trigger from scheduled low-priority runs.

---

## 11) Implementation Sequence
1. Define feature config and version schema.
2. Implement sparse feature builder.
3. Implement text recipe composer and signature calculator.
4. Integrate embedding model inference job.
5. Implement persistence writers with upsert semantics.
6. Add quality gates and metrics emission.
7. Add incremental refresh and backfill orchestration.

---

## 12) Exit Criteria
- Feature/embedding outputs are deterministic and versioned.
- Coverage and quality gates are enforced before training.
- Missing embedding fallback behavior is explicit and testable.
- Refresh and backfill logic is operationally safe and resumable.
