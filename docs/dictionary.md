# Dictionary

## Normalization
- NFC Unicode normalization
- Trim whitespace
- Uppercase using Polish locale

Normalization policy is recorded in metadata as `NFC_UPPERCASE_PL_TRIM`.

## Artifacts
- `artifacts/osps.fst`
- `artifacts/osps.fst.meta.json`

Metadata fields:
- `formatVersion`
- `normalisation`
- `wordCount`
- `sourceSha256`
- `createdAt`

## Build
Test build:
```
./gradlew :packages:dictionary-tools:compileOsps \
  -PospsInput=osps_shortened.txt \
  -PospsOutput=artifacts/osps.fst
```

Full build:
```
./gradlew :packages:dictionary-tools:compileOsps \
  -PospsInput=osps.txt \
  -PospsOutput=artifacts/osps.fst
```
