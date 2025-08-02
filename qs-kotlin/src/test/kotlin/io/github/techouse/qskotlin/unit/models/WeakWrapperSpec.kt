package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.models.WeakWrapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assertions.assertNotEquals

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
        }
    })
