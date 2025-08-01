---
name: Bug report
about: The library crashes, produces incorrect encoding/decoding, or behaves unexpectedly.
title: ''
labels: bug
assignees: techouse
---

<!--
  Since this is a port of `qs`, please check the original repo for related issues:
  https://github.com/ljharb/qs/issues
  If you find a relevant issue or spec note, please link it here.
-->

## Summary

<!-- A clear and concise description of what the bug is. -->

## Steps to Reproduce

<!-- Include full steps so we can reproduce the problem. Prefer a minimal repro. -->

1. ...
2. ...
3. ...

**Expected result**  
<!-- What did you expect to happen? -->

**Actual result**  
<!-- What actually happened? Include exact output / string values where relevant. -->

## Minimal Reproduction

> The simplest way is a **single unit test** that fails.  
> Create a minimal Gradle project or use your existing one and add a failing test demonstrating the issue.

<details>
<summary>Failing Kotlin test</summary>

```kotlin
import io.github.techouse.qskotlin.QS
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReproSpec : StringSpec({
    "repro" {
        // Replace with the minimal input that fails:
        QS.decode("a[b]=1") shouldBe mapOf("a" to mapOf("b" to "1"))
    }
})
```
</details>

If the issue only appears when **encoding**, add the minimal input + options used:
```kotlin
import io.github.techouse.qskotlin.QS
import io.github.techouse.qskotlin.models.EncodeOptions

val out = QS.encode(mapOf("a" to listOf("x", "y")), EncodeOptions(encode = false))
println(out) // <-- paste the actual output and the expected output in the issue
```

If the issue only appears on **Android**, please provide an Android-focused repro (see “Android details” below).

## Logs

Please include relevant logs:

- Gradle + tests (JVM):
  ```bash
  ./gradlew :qs-kotlin:clean :qs-kotlin:test --info --stacktrace
  ```

- If you created a small demo project, include the **full console output** from the failing run.

- If a specific input string causes the issue, paste that exact string together with the **actual** and **expected** decoded/encoded structures.

<details>
<summary>Console output</summary>

```
# paste here
```
</details>

## Environment

- **OS**: <!-- e.g., macOS 14.5 / Ubuntu 22.04 / Windows 11 -->
- **JDK**: output of `java -version`
- **Gradle**: output of `./gradlew -v`
- **Kotlin**: <!-- e.g., 2.0.20 -->
- **qs-kotlin** version: <!-- e.g., 0.1.0 -->
- **Charset** in use (if relevant): <!-- UTF-8 / ISO-8859-1 -->
- **Android?**: <!-- yes/no -->

### Dependency snippet (from your Gradle file)

```kotlin
dependencies {
    implementation("io.github.techouse:qs-kotlin:<version>")
    testImplementation(platform("io.kotest:kotest-bom:<version>"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
}
```

> If you use additional encoders/decoders, please mention and show their configuration.

## Android details (if applicable)

- **Android Gradle Plugin** version:
- **compileSdk** / **minSdk**:
- Device/Emulator + Android version:
- Repro steps (module and task):
  ```bash
  ./gradlew :qs-kotlin-android:assembleRelease --stacktrace
  # or if you have Android unit tests
  ./gradlew :qs-kotlin-android:testDebugUnitTest --stacktrace
  ```

- Minimal Android sample (preferred): a tiny app/module with one unit test or instrumentation test that fails.

## Is this a regression?

- Did this work in a previous version of qs-kotlin? If so, which version?

## Additional context

- Links to any related `qs` JavaScript issues/spec notes:
- Any other libraries involved (HTTP clients, frameworks, etc.) and versions:
- Edge cases (e.g., very deep nesting, extremely large strings, ISO-8859-1 with numeric entities, RFC1738 vs RFC3986 spaces, Instant/LocalDateTime serialization, comma list format, etc.):
