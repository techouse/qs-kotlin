package io.github.techouse.qskotlin.models

/** Internal model to distinguish between `null` and not set value (aka `undefined`). */
internal sealed class Undefined {
    data object Instance : Undefined() {
        override fun toString(): String = "Undefined"
    }

    companion object {
        operator fun invoke(): Undefined = Instance
    }
}
