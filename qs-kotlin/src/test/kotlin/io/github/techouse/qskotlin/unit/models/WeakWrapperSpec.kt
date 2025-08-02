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
        }
    })
