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

## Compatibility checks
Runtime validates:
- `formatVersion` matches `DictionaryFormat.FORMAT_VERSION`
- `normalisation` matches `DictionaryFormat.NORMALISATION`

Mismatches fail fast on load.

## Benchmark
Run JMH benchmarks for lookup throughput:
```
./gradlew :packages:dictionary-runtime:jmh
```

To benchmark against a custom wordlist or prebuilt FST:
```
./gradlew :packages:dictionary-runtime:jmh -PjmhWordlistPath=osps.txt
./gradlew :packages:dictionary-runtime:jmh -PjmhFstPath=artifacts/osps.fst
```

Quick run (shorter warmup/measurement):
```
./gradlew :packages:dictionary-runtime:jmh -PjmhWordlistPath=osps.txt -PjmhQuick=true
```
