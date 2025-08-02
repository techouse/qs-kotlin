package io.github.techouse.qskotlin.enums

/** An enum of all available duplicate key handling strategies. */
enum class Duplicates {
    /** Combine duplicate keys into a single key with an array of values. */
    COMBINE,

    /** Use the first value for duplicate keys. */
    FIRST,

    /** Use the last value for duplicate keys. */
    LAST;
}
