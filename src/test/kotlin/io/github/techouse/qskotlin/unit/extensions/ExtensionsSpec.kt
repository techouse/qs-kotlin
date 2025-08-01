package io.github.techouse.qskotlin.unit.extensions

import io.github.techouse.qskotlin.extensions.slice
import io.github.techouse.qskotlin.extensions.whereNotType
import io.github.techouse.qskotlin.models.Undefined
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ExtensionsSpec :
    FunSpec({
        context("IterableExtension") {
            test("whereNotUndefined") {
                val iterable: Iterable<Any> = listOf(1, 2, Undefined(), 4, 5)
                val result: Iterable<Any> = iterable.whereNotType<Any, Undefined>()
                result.shouldBeInstanceOf<Iterable<Any>>()
                result.toList() shouldBe listOf(1, 2, 4, 5)
            }
        }

        context("ListExtension") {
            test("whereNotUndefined") {
                val list: List<Any> = listOf(1, 2, Undefined(), 4, 5)
                val result: List<Any> = list.whereNotType<Any, Undefined>().toList()
                result.shouldBeInstanceOf<List<Any>>()
                result shouldBe listOf(1, 2, 4, 5)
            }

            test("slice") {
                val animals = listOf("ant", "bison", "camel", "duck", "elephant")

                animals.slice(2) shouldBe listOf("camel", "duck", "elephant")
                animals.slice(2, 4) shouldBe listOf("camel", "duck")
                animals.slice(1, 5) shouldBe listOf("bison", "camel", "duck", "elephant")
                animals.slice(-2) shouldBe listOf("duck", "elephant")
                animals.slice(2, -1) shouldBe listOf("camel", "duck")
                animals.slice() shouldBe listOf("ant", "bison", "camel", "duck", "elephant")
            }
        }

        context("StringExtensions") {
            test("slice") {
                val str = "The quick brown fox jumps over the lazy dog."

                str.slice(31) shouldBe "the lazy dog."
                str.slice(31, 1999) shouldBe "the lazy dog."
                str.slice(4, 19) shouldBe "quick brown fox"
                str.slice(-4) shouldBe "dog."
                str.slice(-9, -5) shouldBe "lazy"
            }
        }
    })
