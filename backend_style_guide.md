# Backend Style Guide

This guide is based on the existing Java backend in this repository. It is meant to capture the style that is already present in the codebase so future AI-generated code feels native to the project instead of generic.

The goal is not to force textbook Java patterns. The goal is to match the author's actual habits: pragmatic Spring Boot code, explicit control flow, clear naming, light abstraction, and straightforward request handling.

## 1. Overall Style

- Prefer simple, direct backend code over clever abstractions.
- Keep the architecture layered and recognizable: controller -> service -> repository, with DTO and mapper layers in between where needed.
- Write code that is easy to trace linearly from top to bottom.
- Favor imperative logic and guard clauses over deeply nested abstractions or heavy functional composition.
- Optimize for readable business flow first. Do not introduce extra interfaces, helper classes, or patterns unless the codebase already needs them.

This project is more pragmatic than dogmatic. Strict purity between layers is not the main goal. If a controller needs a mapper and a service and the combination is clear, that is acceptable.

## 2. Package And Project Structure

Follow the existing Spring package layout:

- `config`
- `controllers`
- `domain.entities`
- `domain.dto`
- `domain.dto.auth`
- `domain.dto.payment`
- `mappers`
- `mappers.impl`
- `repositories`
- `security`
- `services`
- `services.impl`

Naming is conventional and explicit:

- Controllers end in `Controller`
- Service interfaces end in `Service`
- Implementations end in `ServiceImpl`
- Repositories end in `Repository`
- DTOs end in `Dto`
- Mappers end in `Mapper` and `MapperImpl`
- Security classes use descriptive names like `ShopUserDetails` and `JwtAuthenticationFilter`
- Config classes end in `Config` or describe their startup responsibility, such as `RoleSeeder`

Do not invent shortened or cute class names. Use literal names that describe the class responsibility.

## 3. Formatting And Code Shape

Formatting is standard, not ornamental:

- Use 4-space indentation in Java classes.
- Put opening braces on the same line as the declaration.
- Stack annotations vertically, one per line.
- Leave blank lines between logical sections of a class or method.
- Use multiline builder chains and stream chains when they are clearer than one long line.

The codebase is not hyper-strict about micro-formatting. Match the surrounding file when touching an existing class. For new classes, keep formatting clean and conventional rather than over-engineered.

## 4. Class Design

Classes are usually small and centered on a single responsibility.

- Controllers own HTTP concerns and request/response branching.
- Services handle persistence orchestration and backend business rules.
- Repositories stay thin and mostly rely on Spring Data query methods.
- DTOs are simple transport shapes.
- Entities are JPA-first domain objects, not rich domain models.
- Mappers are thin wrappers around `ModelMapper`.

Do not add utility-heavy base classes, custom response wrappers, or generic service hierarchies unless there is already a strong project-level need.

## 5. Dependency Injection Style

The dominant pattern is constructor injection.

- Prefer explicit constructors in controllers, services, and mapper implementations.
- Use `private final` dependencies when practical.
- `@RequiredArgsConstructor` is used in some smaller config/security classes and is acceptable.
- Avoid setter injection.

There are a few classes using `@Autowired` field injection. Treat that as existing inconsistency, not the preferred default for new code. For AI-generated code, prefer constructor injection unless you are editing a class that already clearly uses field injection and should stay locally consistent.

## 6. Controllers

Controllers are one of the clearest expressions of the code style in this project.

### Routing

- Most endpoints live under `/api`.
- Request mappings are explicit and literal.
- Methods are named after the action they perform: `getUser`, `createMusicItem`, `removeFromCart`, `verifyStripeSessionID`.

### Request Handling

- Use `ResponseEntity` directly.
- Return specific HTTP statuses explicitly.
- Use `ResponseEntity.ok(...)` for straightforward 200 responses.
- Use `new ResponseEntity<>(..., HttpStatus.X)` when the method branches across multiple statuses.

### Control Flow

Prefer step-by-step request handling:

1. Validate authentication or request preconditions.
2. Load the current user or target entity.
3. Return early on missing/invalid cases.
4. Perform the mutation or lookup.
5. Map the result to a DTO if needed.
6. Return the response with an explicit status.

This backend favors visible branching over abstraction. It is normal to see several explicit null checks in a controller method.

### Auth Checks

- `@AuthenticationPrincipal UserDetails userDetails` is the normal way to access the current user.
- Methods often guard `userDetails == null` explicitly.
- Admin-only flows may use both `@PreAuthorize("hasRole('ADMIN')")` and an in-method authority check.

That double-checking style is part of the current codebase. Do not replace it with something more abstract unless the whole surrounding file already follows a different pattern.

## 7. Services

Service interfaces are straightforward and close to CRUD.

- Keep interface methods short and literal: `findAll`, `find`, `save`, `delete`, `exists`, `findByEmail`, `findByFileName`.
- Service implementations are usually thin orchestration layers over repositories.
- Business logic is still kept readable and imperative rather than extracted into many tiny helpers.

When writing a service method:

- Prefer explicit loops over clever stream-heavy mutation.
- Keep the method body linear and easy to follow.
- Use domain objects directly when the layer is internal.
- Avoid speculative abstractions.

The codebase does not chase perfect domain modeling. Services are allowed to be pragmatic transaction coordinators.

## 8. Repositories

Repositories are minimal Spring Data JPA interfaces.

- Extend `JpaRepository`.
- Add only obvious query methods such as `findByEmail`, `findByName`, and `findByFileName`.
- Return `Optional<T>` from repository lookup methods.
- Do not add custom query logic unless a simple derived query name is not enough.

Repository names and method names should be literal and business-readable.

## 9. Entities

Entity style is very consistent:

- Use JPA annotations directly on fields.
- Use Lombok heavily, especially `@Data`, `@Builder`, `@AllArgsConstructor`, and `@NoArgsConstructor`.
- Use `@Builder.Default` to initialize collection fields.
- Favor `Set` for relationship collections.
- Use eager loading where the existing entity model already does so.

Entities in this codebase are not minimalist records. They often contain:

- generated IDs
- basic field annotations
- collection relationships
- builder support
- hand-written `toString()` methods for debugging

When creating a new entity, follow the same Lombok-plus-JPA approach instead of switching to records, immutable constructors, or manually written boilerplate.

## 10. DTOs

DTOs are simple data carriers.

- Use Lombok `@Data`.
- Add `@Builder` where construction convenience is helpful.
- Keep fields public through generated accessors; do not create custom accessor logic without a reason.
- It is acceptable for DTOs to contain small convenience methods, such as `priceTimes100()`.

Do not over-validate DTOs with a large annotation layer unless validation is already being introduced for the relevant feature. The current code leans toward simple request objects and explicit controller logic.

## 11. Mapping

Mapping is intentionally lightweight.

- Use small mapper interfaces plus `...MapperImpl` classes.
- Back mapper implementations with `ModelMapper`.
- Keep mapping methods literal: `fromUserDto`, `toUserDto`, `fromMusicItemDto`, `toMusicItemDto`.

Do not hand-write large transformation layers unless the mapping becomes genuinely nontrivial.

## 12. Null And Optional Handling

This codebase has a very recognizable pattern here:

- Repository and service lookups often return `Optional<T>`.
- At the call site, `Optional` is frequently converted with `orElse(null)`.
- The code then branches with explicit `if (x == null)` checks.

This is an important style marker. In this backend, `Optional` is often used as a repository/service boundary type, not as a full fluent programming style.

For AI-generated code:

- Return `Optional` from repository/service lookup APIs when it fits the existing pattern.
- In controllers and service methods, it is acceptable to unwrap to `null` and branch explicitly if that keeps the method clearer.
- Avoid deeply chaining `map`, `flatMap`, and `ifPresentOrElse` unless the local file already uses that style.

## 13. Collections And Streams

Use collections pragmatically:

- `Set` for owned items, roles, carts, and relationship-backed collections.
- `List` for ordered responses and request payloads.
- Streams are mainly used for mapping one collection to another or performing a simple aggregate.
- `toList()` is preferred over collecting with older verbose syntax.

Examples of acceptable stream usage in this style:

- entity list -> DTO list
- authority stream -> `anyMatch(...)`
- summing item prices

Avoid turning entire methods into dense stream pipelines when a loop would read more naturally.

## 14. Error Handling And Logging

Error handling is explicit and local.

- Controllers commonly return status codes directly instead of throwing custom exceptions for every case.
- A global error handler exists for broad fallback cases such as unexpected exceptions and bad credentials.
- Logging is pragmatic rather than fully standardized.

Current style includes both `Slf4j` logging and direct `System.out.println(...)` / `System.out.print(...)`, especially in seeders and integration-oriented code. For future AI-generated code:

- Use `log` in request-handling and long-lived application components when the class already uses `@Slf4j`.
- Direct `System.out` is acceptable in startup seeding, quick debugging, or simple integration code if that matches the local file.
- Do not add elaborate structured logging frameworks or wrappers.

## 15. Comments

Comments are used sparingly and usually for one of three reasons:

- clarifying the purpose of a non-obvious block
- documenting a workflow step
- temporarily preserving work-in-progress code

Preferred comment style:

- short block comments before genuinely non-obvious logic
- short inline comments when the intent is not obvious from the code alone

Avoid excessive narration. Most methods should remain readable without comment-heavy explanation.

The codebase does contain some commented-out blocks from earlier iterations. For new AI-generated code, avoid adding dead commented-out sections unless there is a clear reason to preserve an unfinished branch during active development.

## 16. Testing Style

The testing style is integration-first rather than mock-heavy.

- Prefer `@SpringBootTest` for backend integration coverage.
- Use `@AutoConfigureMockMvc` for controller integration tests.
- Use `@DirtiesContext`, `@Transactional`, and `@Rollback` in the existing pattern where test isolation matters.
- Use helper factories like `TestDataUtil` to create entities.
- Use AssertJ `assertThat(...)`.
- Use MockMvc JSON assertions for controller responses.

Test names are descriptive and behavior-oriented:

- `testThatUsersCanBeCreated`
- `testThatFindOneMusicItemReturnsHttp200`
- `testThatListAllMusicItemsReturnsAccurateJson`

That naming style is verbose, literal, and acceptable for this codebase.

## 17. What AI Should Do By Default

When generating new backend code for this project, default to the following:

- Use the existing package layout and class suffix naming.
- Prefer Spring Boot conventions over custom framework layers.
- Use constructor injection.
- Keep controller methods explicit and status-code-driven.
- Use `ResponseEntity` directly.
- Use Lombok on DTOs and entities.
- Keep repositories thin.
- Keep service methods straightforward and imperative.
- Use `Optional` at repository/service boundaries, but unwrap to `null` and branch explicitly when that makes controller/service code clearer.
- Use streams mainly for mapping and simple aggregates.
- Match local file style when editing existing code.

## 18. What AI Should Avoid

- Do not introduce generic enterprise abstractions that the codebase does not already use.
- Do not replace direct branching with heavy functional or reactive patterns.
- Do not switch the project to records, MapStruct, custom response envelopes, hexagonal architecture, or exception-driven control flow unless explicitly asked.
- Do not add unnecessary helper layers just to look clean on paper.
- Do not “improve” the code into a style that no longer sounds like this project.

## 19. Resolving Inconsistencies

This codebase has a few real inconsistencies, which is normal for a personal project. When AI has to choose, use these tie-breakers:

- Prefer constructor injection over field injection.
- Prefer the interface type for dependencies unless the file already intentionally depends on an implementation.
- Prefer `ResponseEntity` with explicit statuses over custom wrappers.
- Prefer simple controller-level guards over new exception hierarchies.
- Prefer `log` inside classes that already use Lombok logging, but do not rewrite surrounding `System.out` usage unless there is a reason.
- Prefer clean new code over copying incidental unused imports or dead commented code.

## 20. Short Reference Example

If AI adds a new endpoint, it should usually look like this stylistically:

```java
@GetMapping(path = "/users/example/{id}")
public ResponseEntity<ExampleDto> getExample(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
) {
    if (userDetails == null) {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    Example example = exampleService.find(id).orElse(null);
    if (example == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    return new ResponseEntity<>(exampleMapper.toExampleDto(example), HttpStatus.OK);
}
```

That example captures the project's main backend voice: explicit, readable, Spring-native, and pragmatic.
