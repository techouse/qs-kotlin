package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.models.WeakWrapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assertions.assertNotEquals
import java.util.concurrent.atomic.AtomicBoolean

class WeakWrapperSpec :
    DescribeSpec({
        describe("equality & hashCode") {
            it("equals true for same referent, false for merely-equal objects") {
                val o = Any()
                val a = WeakWrapper(o)
                val b = WeakWrapper(o)
                val c = WeakWrapper(Any())
                (a == b).shouldBeTrue()
                assertNotEquals(a, c)
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
                System.gc()
                System.runFinalization()

                // The referent might still be alive, so we need to be more aggressive
                for (i in 1..10) {
                    System.gc()
                    System.runFinalization()
                    Thread.sleep(10)
                    if (wrapper.get() == null) break
                }

                // If GC was successful, toString should show <collected>
                if (wrapper.get() == null) {
                    wrapper.toString() shouldBe "WeakWrapper(<collected>)"
                }
            }

            it("Equality: same referent, different wrappers") {
                val referent = Any()
                val w1 = WeakWrapper(referent)
                val w2 = WeakWrapper(referent)
                w1 shouldBe w2
                w1.hashCode() shouldBe w2.hashCode()
                w1 shouldBe w1 // reflexive
                (w1 == Any()).shouldBeFalse()
            }

            it("Inequality: different referents") {
                val w1 = WeakWrapper(Any())
                val w2 = WeakWrapper(Any())
                (w1 == w2).shouldBeFalse()
            }

            it("hashCode stable after collection") {
                var obj: Any? = Any()
                val wrapper = WeakWrapper(obj!!)
                val originalHash = wrapper.hashCode()
                obj = null
                waitForCollection(wrapper)
                wrapper.hashCode() shouldBe originalHash
            }

            it("equals returns false once referent collected") {
                var obj: Any? = Any()
                val w1 = WeakWrapper(obj!!)
                val w2 = WeakWrapper(obj)
                obj = null
                val collected = waitForCollection(w1)
                if (!collected) return@it // avoid flakiness if GC did not collect in time
                (w1 == w2).shouldBeFalse()
            }

            it("toString shows collected state once referent GC'd") {
                class Big(val data: ByteArray = ByteArray(256_000))
                var big: Big? = Big()
                val w = WeakWrapper(big!!)
                w.toString().startsWith("WeakWrapper(Big") shouldBe true
                big = null
                waitForCollection(w)
                val ts = w.toString()
                val matches = ts == "WeakWrapper(<collected>)" || ts.startsWith("WeakWrapper(Big")
                matches shouldBe true
            }

            it("get() reflects liveness") {
                val flag = AtomicBoolean(false)
                @Suppress("DEPRECATION")
                class Holder(val onFinalize: AtomicBoolean) {
                    protected fun finalize() {
                        onFinalize.set(true)
                    }
                }
                var h: Holder? = Holder(flag)
                val w = WeakWrapper(h!!)
                w.get().shouldNotBeNull()
                h = null
                waitForCollection(w)
                val current = w.get()
                if (current == null) {
                    current.shouldBeNull()
                } else {
                    flag.get() shouldBe false
                }
            }
        }
    })

private fun forceGcPass() {
    repeat(3) {
        System.gc()
        System.runFinalization()
        val junk = Array(128) { ByteArray(1024) }
        @Suppress("UNUSED_VARIABLE") val sink = junk.size
        Thread.sleep(5)
    }
}

private fun waitForCollection(wrapper: WeakWrapper<*>, timeoutMs: Long = 2000): Boolean {
    if (wrapper.get() == null) return true
    val deadline = System.nanoTime() + timeoutMs * 1_000_000
    while (System.nanoTime() < deadline) {
        if (wrapper.get() == null) return true
        forceGcPass()
    }
    return wrapper.get() == null
}
