# Contributing to `qs-kotlin`

Thanks for your interest in improving **qs-kotlin**! This project welcomes PRs, issues, and discussion.  
Please read this guide before contributing.

> A friendly reminder: this project follows a Code of Conduct. See `CODE-OF-CONDUCT.md`.

---

## Supported toolchain

- **Kotlin:** 2.2+
- **JVM target:** 17 (project compiles and runs tests on JDK 17; please avoid using preview APIs)
- **Gradle:** 8.x
- **IDE:** IntelliJ IDEA (Community or Ultimate)

If you find breakage on newer Kotlin/JDK versions, open an issue with repro details.

---

## Getting started

```bash
# Clone
git clone https://github.com/techouse/qs-kotlin.git
cd qs-kotlin

# Run the full test suite
./gradlew clean test

# Run a single spec or test
./gradlew test --tests "io.github.techouse.qskotlin.unit.EncodeSpec"
./gradlew test --tests "*EncodeSpec.can use custom encoder"

# Build (without running tests)
./gradlew assemble
```

---

## Code style & formatting (ktfmt)

This repo uses **ktfmt** (Kotlinlang style) via the IntelliJ plugin.

**IDE setup**

1. Install **ktfmt** plugin in IntelliJ (Settings → Plugins → Marketplace → _ktfmt_).
2. Enable it and pick **Kotlinlang** style. The repo includes the IDE state:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="KtfmtSettings">
    <option name="enableKtfmt" value="Enabled" />
    <option name="uiFormatterStyle" value="Kotlinlang" />
  </component>
</project>
```

3. (Recommended) Turn on **Reformat code on Save** (Settings → Tools → Actions on Save → _Reformat code_).

**CI/CLI options (choose one and propose in your PR if adding):**

- **Spotless + ktfmt**

  ```kotlin
  plugins {
      id("com.diffplug.spotless") version "<latest>"
  }

  spotless {
      kotlin {
          // Keep aligned with the IDE: Kotlinlang style
          ktfmt("<latest>").kotlinlangStyle()
          target("src/**/*.kt")
      }
  }

  // ./gradlew spotlessApply to format, ./gradlew spotlessCheck to verify
  ```

- **ktfmt Gradle plugin**

  ```kotlin
  plugins {
      id("com.ncorti.ktfmt.gradle") version "<latest>"
  }

  ktfmt {
      kotlinLangStyle() // match IDE style
  }

  // ./gradlew ktfmtFormat to format, ./gradlew ktfmtCheck to verify
  ```

> If you add either plugin, keep versions up to date and align the style with the IDE (Kotlinlang).

**General style**

- 4‑space indentation, meaningful names, small/pure functions where reasonable.
- Keep hot-path methods allocation-light; use iterative approaches over deep recursion in parser/encoder internals.

---

## Tests

We use **Kotest** (on **JUnit 5**) for unit tests. When you change code paths that touch parsing or encoding, add or update tests.

- Run all tests: `./gradlew test`
- Fail fast: `./gradlew test --fail-fast`
- With stacktraces: `./gradlew test --stacktrace`
- HTML test report: `build/reports/tests/test/index.html`

### Coverage (JaCoCo)

```bash
./gradlew jacocoTestReport
# Open the HTML report
open build/reports/jacoco/test/html/index.html  # (macOS)
```

---

## Project layout (high level)

```
src/
  main/kotlin/io/github/techouse/qskotlin/
    QS.kt               # Public API (decode/encode)
    Utils.kt            # Core helpers, escaping/unescaping, merge, etc.
    Decoder.kt          # Internal decoding helpers
    Encoder.kt          # Internal encoding helpers
    enums/, models/     # Options, enums, small value types
  test/kotlin/io/github/techouse/qskotlin/unit/
    *Spec.kt            # Kotest specs mirroring qs behavior
```

---

## Compatibility with JS `qs`

This port aims to mirror the semantics of [`qs`](https://github.com/ljharb/qs) (including edge cases).  
If you notice divergent behavior, please:

1. Add a failing test that demonstrates the difference.
2. Reference the `qs` test or behavior you expect.
3. Propose a fix, or open a focused issue.

---

## Performance notes

- Hot paths (splitting parameters, bracket scanning, entity interpretation) should avoid `Regex` where simple loops suffice.
- Prefer `StringBuilder` and pre-sized collections when possible.
- Avoid creating intermediate maps/lists in tight loops.
- Watch for algorithmic complexity (e.g., nested scans).

If you submit perf changes, include a short note and—if available—a microbenchmark. A future JMH module is welcome.

---

## Submitting a change

1. **Open an issue first** for big changes to align on approach.
2. **Small, focused PRs** are easier to review and land quickly.
3. **Add tests** that cover new behavior and edge cases.
4. **Keep public API stable** unless we agree on a version bump.
5. **Changelog entry** (in the PR description is fine) for user-visible changes.

### Commit/PR style

- Clear, descriptive commits. Conventional Commits welcome but not required.
- Reference issues as needed, e.g., “Fixes #123”.
- Prefer present tense: “Add X”, “Fix Y”.

### Branch naming

Use a short, descriptive branch: `fix/latin1-entities`, `feat/weakmap-sidechannel`, etc.

---

## Releasing (maintainers)

1. Update version in `gradle.properties`.
2. Ensure `./gradlew clean test jacocoTestReport` passes.
3. Tag and publish to Maven Central (Sonatype). Include release notes:
   - Added/Changed/Fixed
   - Behavior differences vs previous release (if any).
4. After publishing, update README with new coordinates if needed.

---

## Security

If you believe you’ve found a vulnerability, please **do not** open a public issue.  
Email the maintainer instead (see GitHub profile). We’ll coordinate a fix and disclosure timeline.

---

## Questions?

Open a discussion or issue with as much detail as possible (input, expected vs actual output, environment).  
Thanks again for helping make `qs-kotlin` solid and fast!
