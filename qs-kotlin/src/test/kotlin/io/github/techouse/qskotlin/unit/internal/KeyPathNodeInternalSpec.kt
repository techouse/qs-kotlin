package io.github.techouse.qskotlin.unit.internal

import io.github.techouse.qskotlin.internal.KeyPathNode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class KeyPathNodeInternalSpec :
    DescribeSpec({
        describe("KeyPathNode internals") {
            it("returns same node when appending an empty segment") {
                val root = KeyPathNode.fromMaterialized("a")

                root.append("") shouldBeSameInstanceAs root
            }

            it("caches dot-encoded representation across calls") {
                val node = KeyPathNode.fromMaterialized("a").append(".b")

                val first = node.asDotEncoded()
                val second = node.asDotEncoded()

                second shouldBeSameInstanceAs first
                first.materialize() shouldBe "a%2Eb"
            }

            it("keeps node identity when dot encoding makes no parent or segment changes") {
                val node = KeyPathNode.fromMaterialized("a").append("[b]")

                node.asDotEncoded() shouldBeSameInstanceAs node
            }

            it("rebuilds child path when only the parent needs dot encoding") {
                val node = KeyPathNode.fromMaterialized("a.b").append("[c]")
                val encoded = node.asDotEncoded()

                encoded.materialize() shouldBe "a%2Eb[c]"
            }
        }
    })
