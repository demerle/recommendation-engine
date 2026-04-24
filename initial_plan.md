# Project Plan: Phase 1 MVP for a Personalized Anime/Manga Recommendation Engine

## Project Name
**Codename:** taste-graph  
**Goal:** Build a Phase 1 MVP that can import anime/manga consumption history, store users/items/interactions, compute personalized recommendations using a hybrid recommender, and expose a simple API/UI for recommendation + feedback.

---

# 1. Phase 1 Objective

Build a working MVP that answers this question:

> Can we produce noticeably better recommendations than generic “users who liked X also liked Y” by combining user history with structured item metadata?

Phase 1 will **not** attempt:
- full all-media support
- a full knowledge graph
- deep LLM-native recommendation logic
- perfect item ontology
- social features
- production-scale distributed infra

Phase 1 **will** include:
- anime + manga support
- AniList-based ingestion
- Postgres as primary data store
- a hybrid recommender using LightFM
- a simple item metadata schema
- explicit user feedback capture
- recommendation API
- recommendation explanation templates
- offline evaluation + internal testing

---

# 2. Scope

## In Scope
- User account creation
- Import user anime/manga list from AniList username
- Fetch/store catalog metadata from AniList
- Normalize titles and item IDs
- Store user-item interactions:
  - planned
  - in_progress
  - completed
  - dropped
  - paused
  - rating
  - progress
- Build item feature vectors from metadata
- Train a hybrid recommender using LightFM
- Generate top-N recommendations
- Expose API endpoints for:
  - user import
  - recommendation retrieval
  - feedback submission
- Basic admin scripts for retraining and backfilling
- Basic explanation layer using structured reasons

## Out of Scope
- NovelUpdates integration
- manhwa-specific dedicated ingestion
- graph database
- vector database
- free-text conversational recommendations
- creator/studio graph reasoning
- multi-tenant enterprise infra
- real-time model training
- polished frontend beyond minimal UI

---

# 3. Product Definition

## Target User
Anime/manga users who:
- already track or can import their history
- are frustrated with weak recommendations
- want recommendations based on actual taste, not popularity

## Core MVP User Flow
1. User signs up
2. User enters AniList username
3. System imports anime/manga list
4. System computes user profile and recommendations
5. User views recommendations
6. User gives simple feedback:
   - interested
   - not interested
   - already consumed
   - dislike reason
7. System stores feedback for next retrain

---

# 4. Core Design Choices

## 4.1 Primary Media Source
**Decision:** Use **AniList API** as the only official primary metadata source for Phase 1.

**Why:**
- modern GraphQL API
- good anime/manga coverage
- user list import possible
- simpler than stitching together many sources too early

**Phase 1 rule:** Do not integrate MAL/Jikan until AniList ingestion is stable.

---

## 4.2 Primary Database
**Decision:** Use **PostgreSQL**.

**Why:**
- structured data fits well
- easy to operate
- strong support for joins, indexing, analytics
- enough for Phase 1 without premature complexity

---

## 4.3 Recommender Model
**Decision:** Use **LightFM** as the Phase 1 recommendation model.

**Why:**
- supports hybrid recommendation
- combines collaborative + content features
- excellent for sparse data
- fast enough for MVP
- simpler than overbuilding deep ranking stacks

**Phase 1 model objective:** Generate top-N recommendations for each user based on:
- historical interactions
- item metadata features

---

## 4.4 Backend Framework
**Decision:** Use **Java + Spring Boot** as the only application backend. Keep **Python** isolated to the recommendation engine and offline jobs.

**Why:**
- strong fit for a structured service layer, auth, and API development
- mature ecosystem for REST APIs, persistence, validation, and scheduling
- clear separation between the application backend and the Python recommendation pipeline
- better matches the planned implementation language for backend code

**Boundary rule:** Spring Boot owns all external APIs, auth, business logic, and database-backed application workflows. Python is internal-only and is used for model training, batch scoring, and recommendation artifact generation.

---

## 4.5 Data Pipeline / Jobs
**Decision:** Use simple Python CLI jobs first, not Airflow/Prefect yet.

**Why:**
- LightFM is Python-native, so the training pipeline should stay in Python for Phase 1
- Phase 1 complexity is low
- cron/manual jobs are sufficient
- avoid orchestration overhead early

**Rule:** Introduce Prefect only when >5 recurring jobs exist.
**Integration rule:** Python jobs should write trained artifacts and/or generated recommendations back to Postgres so the Spring Boot API can serve results without depending on Python at request time.

---

## 4.6 Service Boundary
**Decision:** Use a two-part architecture:
- Spring Boot application backend
- Python recommendation engine

**Why:**
- preserves a statically typed backend for day-to-day application development
- keeps the stronger Python recsys/ML ecosystem available where it matters
- reduces operational complexity compared with a real-time cross-language inference path

**Phase 1 recommendation:** Prefer batch scoring plus Postgres handoff over a live Python scoring service.

---

## 4.7 Frontend
**Decision:** Minimal frontend, either:
- Next.js dashboard, or
- even simpler internal admin/static UI

**Recommendation:** If solo/small team, do **not** overbuild frontend. Focus on API + internal testing interface.

---

# 5. High-Level Architecture

## Services
### 1. API Service
- Spring Boot application
- the only public backend service
- handles user auth, import trigger, recommendations, feedback
- reads recommendations and explanations from Postgres

### 2. Ingestion Service
- fetches AniList metadata and user lists
- implemented within Spring Boot application workflows and scheduled/admin-triggered jobs
- upserts items and interactions into Postgres

### 3. Training Job
- Python job
- internal-only subsystem, not exposed to clients
- builds user-item interaction matrix
- builds item feature matrix
- trains LightFM model
- saves model artifacts and metadata

### 4. Recommendation Job / Service
- Python batch scoring job
- loads trained model
- computes candidate rankings
- writes recommendations and explanation inputs into Postgres for the Spring Boot API to serve

---

# 6. Tech Stack

## Application Backend
- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Flyway

## Recommendation Engine / Jobs
- Python 3.11
- pandas
- numpy
- scipy
- scikit-learn
- LightFM

## Database
- PostgreSQL 15+

## HTTP / API integration
- Spring WebClient or RestClient
- httpx (for Python jobs, if needed)

## Dev / Ops
- Docker
- docker-compose
- JUnit 5
- MockMvc / Spring Boot Test
- Gradle or Maven
- Spotless
- pytest
- Ruff

## Optional UI
- Next.js
- Tailwind

---

# 7. Repository Structure

```text
taste-graph/
├─ apps/
│  ├─ api/
│  │  ├─ build.gradle.kts
│  │  └─ src/
│  │     ├─ main/
│  │     │  ├─ java/.../
│  │     │  │  ├─ controller/
│  │     │  │  ├─ service/
│  │     │  │  ├─ domain/
│  │     │  │  ├─ repository/
│  │     │  │  └─ config/
│  │     │  └─ resources/
│  │     │     ├─ application.yml
│  │     │     └─ db/migration/
│  │     └─ test/java/.../
│  └─ recommender/
│     ├─ import_anilist_user.py
│     ├─ sync_catalog.py
│     ├─ train_model.py
│     └─ generate_recommendations.py
├─ shared/
│  ├─ schemas/
│  └─ model-artifacts/
├─ tests/
├─ scripts/
├─ docker-compose.yml
├─ pyproject.toml
└─ README.md
