package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.models.FunctionFilter
import io.github.techouse.qskotlin.models.IterableFilter
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FilterSpec {
    @Test
    fun `FunctionFilter primary constructor executes lambda`() {
        val captured = mutableListOf<Pair<String, Any?>>()
        val filter = FunctionFilter { k, v ->
            captured += k to v
            v?.toString()?.reversed() ?: "<none>"
        }
        filter.function("abc", 123) shouldBe "321"
        filter.function("def", null) shouldBe "<none>"
        captured.shouldContainExactly(listOf("abc" to 123, "def" to null))
    }

    @Test
    fun `IterableFilter primary constructor stores custom iterable`() {
        val backing = listOf(1, 2, 3)
        val customIterable =
            object : Iterable<Int> {
                override fun iterator(): Iterator<Int> = backing.iterator()
            }
        val filter = IterableFilter(customIterable)
        filter.iterable.toList() shouldBe backing
    }
}
