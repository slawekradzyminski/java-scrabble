# Quality

## Required checks
- `./gradlew test check`
- `./gradlew jacocoTestReport`

## Coverage targets
- Minimum 75% line coverage per Java module.
- Coverage is enforced via `jacocoTestCoverageVerification` (wired into `check`).

## Reports
Each Java module produces a coverage report:
- `apps/backend/build/reports/jacoco/test/html/index.html`
- `packages/dictionary-runtime/build/reports/jacoco/test/html/index.html`
- `packages/game-engine/build/reports/jacoco/test/html/index.html`
