# Agent Guidelines

## Required checks before final response
- Run `./gradlew test check` after changes.
- Run `./gradlew jacocoTestReport` to refresh coverage reports.

## Coverage expectations
- Maintain minimum line coverage of 75% per Java module.
- If coverage drops below 75%, add focused tests to restore it.
- Coverage enforcement is wired via `jacocoTestCoverageVerification` and `check`.
- Frontend coverage is enforced via Vitest thresholds (see `apps/frontend/vitest.config.ts`).

## Documentation hygiene
- Keep `plan.md` updated with progress notes.
- Update `docs/` when new subsystems or workflows are introduced.

## Test structure
- Use `// given`, `// when`, `// then` comment markers in all test methods.
