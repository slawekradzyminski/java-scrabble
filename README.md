# Java Scrabble (Polish)

## Modules
- `apps/backend` - Spring Boot WebFlux service
- `packages/game-engine` - domain engine (board, tiles, scoring scaffolding)
- `packages/dictionary-runtime` - FST loader + lookup API
- `packages/dictionary-tools` - CLI compiler (word list → FST + metadata)

## Dictionary pipeline
Compile the test dictionary from `osps_shortened.txt`:

```bash
./gradlew :packages:dictionary-tools:compileOsps \
  -PospsInput=osps_shortened.txt \
  -PospsOutput=artifacts/osps.fst
```

Metadata is generated alongside the FST as `artifacts/osps.fst.meta.json`.

## Tests

```bash
./gradlew test
```

## Checks and coverage

```bash
./gradlew check
```

To generate coverage reports:

```bash
./gradlew jacocoTestReport
```

Coverage report locations:
- `apps/backend/build/reports/jacoco/test/html/index.html`
- `packages/dictionary-runtime/build/reports/jacoco/test/html/index.html`
- `packages/game-engine/build/reports/jacoco/test/html/index.html`

## Run backend

```bash
./gradlew :apps:backend:bootRun
```

Example dictionary call:

```bash
curl "http://localhost:8080/api/dictionary/contains?word=zajawiałeś"
```

Health endpoint:

```bash
curl "http://localhost:8080/actuator/health"
```
