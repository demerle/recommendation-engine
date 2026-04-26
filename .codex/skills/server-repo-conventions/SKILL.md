---
name: server-repo-conventions
description: Use when working in the recommendation-engine repository and you need to follow repo-local coding conventions without loading all style guidance into context up front. This skill keeps context lean by treating `server/.cursor/rules/*.mdc` as the canonical detailed style source, reading `project-style-baseline.mdc` first and then only the additional rule files relevant to the files or tests being changed.
---

# Server Repo Conventions

Use the existing Cursor rules as the canonical detailed style guide for this repository.

## Follow This Loading Order

1. Read `E:\dev_projects\recommendation-engine\server\.cursor\rules\project-style-baseline.mdc` before making substantial edits in `server/`.
2. Read only the additional `.mdc` files relevant to the files being changed.
3. Apply the matched rules while implementing and testing.
4. Do not duplicate or restate the rule contents unless the user asks for a summary.

## Load Rules By Area

- For `src/main/java/**/config/*.java`, read `config-style.mdc`.
- For `src/main/java/**/controllers/*.java`, read `controllers-style.mdc`.
- For `src/main/java/**/domain/dto/**/*.java`, read `dto-style.mdc`.
- For `src/main/java/**/domain/entities/*.java`, read `entities-style.mdc`.
- For `src/main/java/**/mappers/**/*.java`, read `mappers-style.mdc`.
- For `src/main/java/**/repositories/*.java`, read `repositories-style.mdc`.
- For `src/main/java/**/security/*.java`, read `security-style.mdc`.
- For `src/main/java/**/services/*.java`, read `services-interface-style.mdc`.
- For `src/main/java/**/services/impl/*.java`, read `services-impl-style.mdc`.
- For `src/test/java/**/controllers/*.java`, read `controller-integration-tests-style.mdc`.
- For `src/test/java/**/repositories/*.java`, read `repository-integration-tests-style.mdc`.
- For `src/test/java/**/TestDataUtil.java`, read `test-data-util-style.mdc`.

## Working Rules

- Treat `server/.cursor/rules/*.mdc` as the source of truth for style decisions inside `server/`.
- Prefer progressive loading: baseline first, then only the rule files needed for the current task.
- If a task spans multiple areas, load only the relevant rules for those areas.
- If no rule exists for a touched path, follow the baseline rule and existing nearby code patterns.
- When local code conflicts with an older rule, preserve working code unless the user asks for a broader cleanup.
