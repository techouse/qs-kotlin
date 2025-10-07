# Copilot Project Instructions

Purpose: Enable AI agents to confidently modify and extend qs-kotlin while preserving behavior parity with the upstream JavaScript `qs` and existing JVM/Android consumers.

## Architecture & Modules
- Core library: `qs-kotlin/` (package `io.github.techouse.qskotlin`). All logic lives here; prefer adding functionality only in this module.
- Android wrapper: `qs-kotlin-android/` – thin AAR re-export (`api(project(":qs-kotlin"))`); DO NOT fork behavior here.
- Parity / regression harness: `comparison/` – Kotlin runner + Node fixture (`comparison/js/qs.js`). Used to diff outputs vs upstream `qs`.
- Shared build + dependency versions: root `build.gradle.kts` + `gradle/libs.versions.toml`. Bump deps only via the version catalog.

## Primary APIs & Options
- Static facade: `QS.encode(map: Map<*, *>, options = EncodeOptions(...))` and `QS.decode(qs: String, options = DecodeOptions(...))`.
- Key option patterns to preserve: depth limit (default 5), `parameterLimit`, list handling (indices/brackets/repeat/comma), `duplicates` (COMBINE/FIRST/LAST), `allowDots` + `encodeDotInKeys` / `decodeDotInKeys`, charset + sentinel, `Undefined` sentinel value to omit keys, filters (`FunctionFilter`, `IterableFilter`), custom `encoder`, `dateSerializer`.
- Mixed list/map or large numeric indices intentionally coerce to maps – keep this logic consistent.

## Performance & Style Conventions
- Favor allocation-light, pure helpers; avoid creating intermediate collections in hot encode/decode paths.
- Keep public API names stable; internal helpers can be refactored but retain behavioral tests.
- Kotlin formatting enforced by `ktfmt`; Java (rare) via Spotless. Run `./gradlew ktfmtFormat` (or `ktfmtCheck` in CI) before committing.

## Testing & Validation Workflow
1. Unit tests: `./gradlew :qs-kotlin:test` (Kotest + JUnit5). New tests in `qs-kotlin/src/test/kotlin/.../*Spec.kt` (e.g., `DecodeSpec`, `EncodeSpec`).
2. Coverage (when touching core): `./gradlew :qs-kotlin:jacocoTestReport` -> see `qs-kotlin/build/reports/jacoco/jacocoJvmReport/html/index.html`.
3. Parity diff (required if encode/decode semantics change):
   - `./gradlew -q :comparison:run > kotlin.out`
   - `node comparison/js/qs.js > node.out`
   - `diff -u node.out kotlin.out` (script shortcut: `comparison/compare_outputs.sh`).
4. Android wrapper sanity: `./gradlew :qs-kotlin-android:assembleRelease :qs-kotlin-android:testReleaseUnitTest`.

## When Changing Behavior
- Update/extend focused specs replicating the upstream `qs` case causing the change.
- Re-run parity diff; ensure only intentional differences (document them in the PR).
- Update README sections if option semantics or defaults shift (also Dokka via `./gradlew docs`).
- Maintain `Undefined` omission semantics: only omit keys whose value is exactly `Undefined()` / `Undefined.INSTANCE`.

## Adding Dependencies / Build Logic
- Add versions to `gradle/libs.versions.toml`; reference via `libs.*` in module `build.gradle.kts`.
- Keep core free of Android-only dependencies; Android module may add tooling (e.g., desugaring) but no divergent logic.

## Commit / PR Conventions
- Emoji + present-tense summary (e.g., `:sparkles: add comma list format` or `:bug: fix depth cutoff`).
- Provide test output snippet and (if changed) parity diff notes.

## Typical Safe Task Examples
- Add new decode option flag: define data class field (default preserving existing behavior), implement in parser branch, add targeted spec, run steps above.
- Optimize a hot loop: micro-benchmark locally (if added), ensure no parity diff, keep allocations flat.

## Pitfalls / Edge Cases To Re-Test
- Deep nesting > depth limit (remaining brackets literalized).
- Mixed list indices + named keys morphing into map.
- Large sparse indices collapsing to map.
- Charset sentinel toggling between UTF-8 / ISO-8859-1.
- Duplicate keys under each `Duplicates` mode.

## What NOT To Do
- Don’t change defaults silently (depth=5, parameterLimit, listFormat, duplicates behavior).
- Don’t add Android-specific code to core or fork logic in wrapper.
- Don’t remove or rename public entry points (`QS`, `EncodeOptions`, `DecodeOptions`) without deprecation path.

Use this file as your operational brief; if a change touches encode/decode semantics, tests + comparison diff are mandatory. Ask maintainers if an intended divergence from upstream `qs` is significant or user-facing.
