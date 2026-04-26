# Part 4 Implementation Plan

## Goal
Implement the first Python-backed feature engineering and embedding pipeline that reads normalized catalog data from Postgres, produces reproducible item features, writes embeddings/version metadata back to Postgres, and is safe enough to become the foundation for Part 5 training.

This plan assumes Part 3 is complete enough that:
- AniList imports populate `items`, `item_metadata`, `user_item_interactions`, `ingestion_runs`, and `ingestion_errors`
- the Postgres schema is the source of truth
- Spring Boot is not responsible for embedding generation logic

---

## What Part 4 Should Deliver

By the end of Part 4, we should be able to:
- read active catalog items and metadata from Postgres
- normalize metadata into deterministic model-ready features
- compose a stable text recipe per item
- generate sentence embeddings in Python
- write embedding rows with provenance/version fields
- detect changed items and refresh only what is necessary
- report coverage and fail fast when feature quality drops below threshold

Part 4 is complete when the data contract needed by Part 5 training is reliable, versioned, and test-covered.

---

## Recommended Implementation Shape

### Java / Spring Boot responsibility
- owns schema, migrations, and any future admin trigger endpoints
- does not compute embeddings

### Python responsibility
- extracts catalog snapshot from Postgres
- builds sparse-ready feature payloads
- computes text signatures
- runs embedding inference
- writes `item_embeddings` and run metadata back to Postgres

This should stay as an offline/batch workflow, not a request-time service.

---

## Phase 4A: Finalize Storage Contract

Before writing Python code, make sure the DB contract for embeddings is present and explicit.

### Files to add first
- `server/src/main/resources/db/migration/V3__create_item_embeddings_and_feature_run_tables.sql`

### Tables to add
- `item_embeddings`
- optional but recommended: `feature_runs`

### `item_embeddings` minimum fields
- `id`
- `item_id`
- `embedding_model`
- `embedding_version`
- `embedding_dim`
- `text_signature`
- `vector_json` or `vector`
- `generated_at`
- `is_active`

### `feature_runs` minimum fields
- `id`
- `run_id`
- `status`
- `embedding_model`
- `embedding_version`
- `items_seen`
- `items_embedded`
- `items_skipped`
- `error_count`
- `summary_json`
- `started_at`
- `completed_at`

### Exit criteria
- migration applies cleanly
- uniqueness and indexing rules are defined
- one item can hold multiple embedding versions safely

---

## Phase 4B: Python Project Skeleton

Create the first Python workspace for offline jobs.

### Suggested structure
- `server/python/pyproject.toml`
- `server/python/src/recommendation_engine_python/__init__.py`
- `server/python/src/recommendation_engine_python/config.py`
- `server/python/src/recommendation_engine_python/db.py`
- `server/python/src/recommendation_engine_python/feature_pipeline.py`
- `server/python/src/recommendation_engine_python/embeddings.py`
- `server/python/src/recommendation_engine_python/models.py`
- `server/python/src/recommendation_engine_python/cli.py`
- `server/python/tests/`

### Initial dependencies
- `sqlalchemy`
- `psycopg` or `psycopg2`
- `pandas`
- `sentence-transformers`
- `numpy`
- `pytest`

### Exit criteria
- Python environment installs cleanly
- CLI can connect to local Postgres
- a no-op command can run against the DB

---

## Phase 4C: Catalog Extraction and Canonicalization

Build the read path first, before embeddings.

### First Python capabilities
- load active items joined with `item_metadata`
- normalize null arrays and null text fields
- canonicalize token lists:
  - trim
  - lowercase
  - dedupe
  - stable sort
- derive feature-friendly buckets:
  - media type
  - format
  - release era bucket
  - episode/chapter bucket

### Suggested files
- `catalog_reader.py`
- `feature_normalizer.py`
- `text_recipe.py`

### Important rule
Every normalization step must be deterministic. If we run the job twice on unchanged inputs, the normalized outputs and signatures should match exactly.

### Exit criteria
- we can print/export a deterministic normalized snapshot for a fixed catalog sample
- token and bucket rules are covered by unit tests

---

## Phase 4D: Text Recipe and Signature

This is the bridge between metadata and embeddings.

### Text recipe v1
- `canonical_title`
- `synopsis`
- `tags`

### Signature behavior
- build one deterministic string representation
- hash it into `text_signature`
- regenerate embeddings only when:
  - text recipe changes
  - embedding model changes
  - embedding version changes

### Suggested outputs per item
- `item_id`
- `text_recipe`
- `text_signature`
- normalized sparse feature payload

### Exit criteria
- same input produces same signature every time
- changed synopsis/tags/title changes signature predictably

---

## Phase 4E: Embedding Inference Job

Once extraction and signatures work, add the actual model inference.

### First-pass design
- batch items in chunks
- call a single pre-trained sentence-transformer model
- write vectors back to `item_embeddings`
- mark active version explicitly

### Good first model choice
- a compact sentence-transformer model with strong general semantic performance

### Required provenance per row
- model name
- embedding version
- vector dimension
- text signature
- generated timestamp

### Exit criteria
- job can embed a small catalog slice end to end
- vectors persist successfully to Postgres
- embedding dimensions are validated before write

---

## Phase 4F: Incremental Refresh and Backfill

After the first full pass works, make it practical.

### Refresh logic
- find items with missing embedding rows for active version
- find items whose `text_signature` changed
- skip unchanged items

### Backfill behavior
- chunked processing
- resumable batches
- per-batch progress logging

### CLI commands worth adding
- `full-refresh`
- `incremental-refresh`
- `backfill-missing`

### Exit criteria
- incremental run writes fewer rows than full refresh on unchanged data
- reruns are idempotent for unchanged items

---

## Phase 4G: Quality Gates and Observability

Do this before moving to Part 5.

### Quality checks
- embedding coverage percentage
- missing synopsis ratio
- embedding dimension consistency
- null/empty text recipe count
- mean vector norm sanity range

### Failure policy
- hard-fail if coverage for active catalog drops below threshold
- hard-fail if vector dimensions mismatch declared model config

### Suggested outputs
- console summary
- `feature_runs.summary_json`
- explicit non-zero exit on failed quality gate

### Exit criteria
- bad runs fail clearly
- successful runs record enough metadata for debugging and reproducibility

---

## Testing Plan for Part 4

### Unit tests
- token normalization
- bucket generation
- text recipe composition
- signature determinism

### Integration tests
- Python DB read/write against local Postgres
- embedding row upsert behavior
- incremental refresh change detection

### Acceptance test
- import one AniList-backed catalog sample
- run feature/embedding job
- confirm `item_embeddings` contains active rows with expected version/model metadata

---

## Recommended Build Order

1. Add DB migration for `item_embeddings` and `feature_runs`
2. Scaffold Python package and DB config
3. Implement catalog extraction + normalization
4. Implement text recipe + signature logic
5. Implement embedding inference job
6. Implement write/upsert path
7. Add incremental refresh logic
8. Add quality gates and tests

This order keeps the work debuggable and gives us useful checkpoints before the model dependency is even introduced.

---

## Checkpoint Definition

Part 4 is in a good checkpoint state when:
- a Python CLI job can run locally
- it reads `items` + `item_metadata`
- it writes versioned embeddings to `item_embeddings`
- reruns are incremental and deterministic
- quality gates prevent bad training inputs from flowing into Part 5

At that point, we should be ready to start Part 5, which will likely be a Python training pipeline reading:
- `user_item_interactions`
- `items`
- `item_metadata`
- `item_embeddings`

and writing model/run outputs back into Postgres.
