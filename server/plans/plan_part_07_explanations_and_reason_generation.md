# Part 7: Explanations and Reason Generation

## 1) Purpose
Define explanation generation that is interpretable, consistent, and faithful to recommendation evidence while avoiding opaque model internals in user-facing messaging.

---

## 2) Principles
- **Truthfulness**: reasons must reflect real evidence from metadata/features.
- **Interpretability**: never expose raw embedding dimensions or unreadable internals.
- **Stability**: same recommendation context yields same primary reason category.
- **Conciseness**: short, user-friendly reason text.
- **Safety**: avoid speculative or misleading claims.

---

## 3) Reason Taxonomy

Primary reason codes:
- `shared_genres`
- `shared_tags`
- `similar_creator_or_studio`
- `similar_theme_textual`
- `behavioral_affinity` (co-consumption style)
- `cold_start_content_match`
- `fallback_popularity_context` (last-resort)

Each reason code maps to:
- template id
- required evidence fields
- confidence/priority weight

---

## 4) Evidence Inputs

Evidence sources:
- interaction profile aggregates
- `item_metadata` overlap signals
- `explanation_inputs` produced at scoring time
- optional catalog priors for fallback

Minimum required evidence by reason:
- `shared_genres`: at least 1 overlapping genre
- `shared_tags`: overlap threshold policy
- `similar_creator_or_studio`: shared creator/studio
- `similar_theme_textual`: semantic similarity crossing threshold with textual descriptor extraction

---

## 5) Reason Selection and Fallback Order

Selection order (default):
1. `shared_genres`
2. `shared_tags`
3. `similar_creator_or_studio`
4. `similar_theme_textual`
5. `behavioral_affinity`
6. `cold_start_content_match`
7. `fallback_popularity_context`

Rule:
- pick highest-priority reason that satisfies evidence constraints.
- optionally include up to 2 secondary reasons if confidence threshold passes.

---

## 6) Template System

Template examples:
- `shared_genres`:
  - "Because you often enjoy {genre_list} titles."
- `similar_creator_or_studio`:
  - "Because it shares creators/studios with works you rated highly."
- `similar_theme_textual`:
  - "Because it has themes similar to titles you liked ({theme_keywords})."

Template constraints:
- no raw numeric score exposure by default.
- max length cap per message.
- avoid repetitive boilerplate across list entries by variant rotation rules.

---

## 7) Explanation Payload Contract

Stored and served payload shape:
```json
{
  "primaryReason": {
    "code": "shared_genres",
    "message": "Because you often enjoy dark fantasy titles.",
    "confidence": 0.82
  },
  "secondaryReasons": [],
  "evidence": {
    "genres": ["Fantasy", "Psychological"],
    "tags": ["Dark", "Mind Game"]
  },
  "version": "explanations_v1"
}
```

Contract stability:
- `code`, `message`, and `version` required.
- `confidence` optional but recommended.
- evidence keys documented and backward-compatible.

---

## 8) Quality Checks and Guardrails

Hard checks:
- no empty explanation payload for active recommendations.
- no template rendering with missing required placeholders.
- reason code must exist in taxonomy.

Guardrails:
- suppress generic reasons if specific evidence exists.
- block reasons with contradictory evidence.
- avoid overclaim language ("guaranteed you will like").

---

## 9) Localization and Readability
- English-first in Phase 1 with template localization-ready keying.
- Keep language plain and concise.
- Avoid jargon terms like "embedding similarity" in user text.

---

## 10) Testing Strategy (Explanation-specific)
- deterministic fixtures for reason selection order.
- template rendering tests for placeholder coverage.
- payload schema validation tests.
- anti-pattern tests (no empty/generic fallback when better reason available).

---

## 11) Implementation Sequence
1. Define reason taxonomy and evidence requirements.
2. Implement evidence extraction layer.
3. Implement reason selector and fallback engine.
4. Implement template renderer with validation.
5. Integrate payload generation into scoring output.
6. Add deterministic explanation tests.

---

## 12) Exit Criteria
- Every recommendation has valid explanation payload or explicit documented fallback.
- Explanation content is interpretable and evidence-backed.
- Payload contract is stable and API-consumable.
- Reason logic is deterministic under fixed inputs.
