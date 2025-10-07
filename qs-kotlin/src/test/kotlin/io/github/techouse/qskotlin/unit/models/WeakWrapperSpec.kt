package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.models.WeakWrapper
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [WeakWrapper].
 *
 * These tests focus on equality, hashCode, toString, and referent liveness behavior.
 *
 * Note: We avoid using finalizers (deprecated) and instead rely on `WeakReference` visibility plus
 * GC nudges to minimize flakiness across different JVMs/CI runners.
 */
class WeakWrapperSpec :
    DescribeSpec({
        describe("equality & hashCode") {
            it("equals true for same referent, false for merely-equal objects") {
                val o = Any()
                val a = WeakWrapper(o)
                val b = WeakWrapper(o)
                val c = WeakWrapper(Any())
                a shouldBe b
                a shouldNotBe c
                a.hashCode() shouldBe System.identityHashCode(o)
            }

            it("toString contains class name when not collected") {
                val o = Any()
                val w = WeakWrapper(o)
                w.toString() shouldContain "WeakWrapper("
            }

            it("toString shows <collected> when referent is garbage collected") {
                var obj: Any? = Any()
                val wrapper = WeakWrapper(obj!!)

                // Clear the reference and force garbage collection
                obj = null
                val collected = waitForCollection(wrapper)
                assumeOrSkip(collected) { "GC did not collect in time; skipping" }
                eventually(duration = kotlin.time.Duration.parse("250ms")) {
                    wrapper.toString() shouldContain "<collected>"
                }
            }

            it("Equality: same referent, different wrappers") {
                val referent = Any()
                val w1 = WeakWrapper(referent)
                val w2 = WeakWrapper(referent)
                w1 shouldBe w2
                w1.hashCode() shouldBe w2.hashCode()
                w1 shouldBe w1 // reflexive
                w1 shouldNotBe Any()
            }

            it("equals short-circuits when other referent already cleared") {
                val referent = Any()
                val w1 = WeakWrapper(referent)
                val w2 = WeakWrapper(referent)
                val field =
                    WeakWrapper::class.java.getDeclaredField("weakRef").apply {
                        isAccessible = true
                    }
                field.set(w2, WeakReference<Any?>(null))

                (w1 == w2) shouldBe false
            }

            it("Inequality: different referents") {
                val w1 = WeakWrapper(Any())
                val w2 = WeakWrapper(Any())
                w1 shouldNotBe w2
            }

            it("hashCode stable after collection") {
                var obj: Any? = Any()
                val wrapper = WeakWrapper(obj!!)
                val originalHash = wrapper.hashCode()
                obj = null
                val collected = waitForCollection(wrapper)
                assumeOrSkip(collected) { "GC did not collect in time; skipping" }
                wrapper.hashCode() shouldBe originalHash
            }

            it("equals returns false once referent collected") {
                var obj: Any? = Any()
                val w1 = WeakWrapper(obj!!)
                val w2 = WeakWrapper(obj!!)
                obj = null
                val collected = waitForCollection(w1)
                assumeOrSkip(collected) { "GC did not collect in time; skipping" }
                w1 shouldNotBe w2
            }

            it("toString shows collected state once referent GC'd") {
                class Big(val data: ByteArray = ByteArray(256_000))
                var big: Big? = Big()
                val w = WeakWrapper(big!!)
                w.toString() shouldContain "WeakWrapper("
                w.toString() shouldContain "Big"
                big = null
                val collected = waitForCollection(w)
                assumeOrSkip(collected) { "GC did not collect in time; skipping" }
                w.toString() shouldContain "<collected>"
            }

            it("get() reflects liveness without finalize (WeakReference)") {
                // Use a plain WeakReference (no ReferenceQueue) to assert clearing without relying
                // on
                // enqueue timing, which varies across JVMs.
                var h: Any? = Any()
                @Suppress("UNUSED_VARIABLE") val weak = WeakReference(h)
                val w = WeakWrapper(h!!)
                w.get().shouldNotBeNull()
                h = null
                val collected = waitForCollection(w)
                assumeOrSkip(collected) { "GC did not collect in time; skipping" }
                // Ensure the referent is actually cleared from a separate WeakReference as well
                waitForCleared(weak).shouldBeTrue()
                w.get().shouldBeNull()
            }
        }
    })

/** Aborts the current test (skips) when [condition] is false. */
private inline fun assumeOrSkip(condition: Boolean, lazyMessage: () -> String) {
    if (!condition) throw org.opentest4j.TestAbortedException(lazyMessage())
}

/**
 * GC/collection helpers used by these tests.
 *
 * We deliberately avoid finalizers (deprecated and may be disabled) and instead rely on
 * `WeakReference` visibility plus a few GC/Finalization "nudges". This minimizes flakiness across
 * different JVMs/CI runners. Timeouts are increased automatically when running on CI.
 */
private fun gcTimeoutMs(): Long =
    System.getenv("QS_GC_TIMEOUT_MS")?.toLongOrNull()
        ?: if (System.getenv("CI") != null) 6000 else 2000

private fun forceGcPass() {
    val passes = System.getenv("QS_GC_PASSES")?.toIntOrNull() ?: 3
    val chunks = System.getenv("QS_GC_CHUNKS")?.toIntOrNull() ?: 256
    val chunkSize = System.getenv("QS_GC_CHUNK_SIZE")?.toIntOrNull() ?: 4096
    repeat(passes) {
        System.gc()
        if (System.getenv("QS_GC_FINALIZE") == "1") System.runFinalization()
        val junk = Array(chunks) { ByteArray(chunkSize) } // ~chunks*chunkSize bytes per pass
        @Suppress("UNUSED_VARIABLE") val sink = junk.size
        Thread.sleep(5)
    }
}

private fun waitForCollection(
    wrapper: WeakWrapper<*>,
    timeoutMillis: Long = gcTimeoutMs(),
): Boolean {
    if (wrapper.get() == null) return true
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
    while (System.nanoTime() < deadline) {
        if (wrapper.get() == null) return true
        forceGcPass()
        runCatching { Thread.onSpinWait() }
    }
    return wrapper.get() == null
}

private fun waitForCleared(
    ref: WeakReference<out Any?>,
    timeoutMillis: Long = gcTimeoutMs(),
): Boolean {
    if (ref.get() == null) return true
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
    while (System.nanoTime() < deadline) {
        if (ref.get() == null) return true
        forceGcPass()
    }
    return ref.get() == null
}
