package io.github.techouse.qskotlin.fixtures

internal enum class DummyEnum {
    LOREM,
    IPSUM,
    DOLOR;

    // Match Dart’s enum.toString() → lowercase name
    override fun toString(): String = name
}
