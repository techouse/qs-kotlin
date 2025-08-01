package io.github.techouse.qskotlin.models

import java.lang.ref.WeakReference

/**
 * Identity-based weak key wrapper.
 * - equals: true only if both referents are alive and identical (===)
 * - hashCode: stable, captured from referent's identity hash at construction
 */
internal class WeakWrapper<T : Any>(value: T) {
    /**
     * A weak reference to the value of type [T]. This allows the referent to be garbage collected
     * while still providing access through the `get()` method.
     */
    private val weakRef = WeakReference(value)

    /**
     * The original hash code of the referent at the time of construction. This is used to provide a
     * stable hash code that does not change even if the referent is collected.
     */
    private val originalHashCode = System.identityHashCode(value)

    /**
     * Returns the referent if it is still alive, or null if it has been collected. This is a weak
     * reference, so the referent may be garbage collected at any time.
     */
    fun get(): T? = weakRef.get()

    /**
     * Returns true if the referent is still alive and identical to the given object. This is an
     * identity check (===) rather than an equality check (==).
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeakWrapper<*>) return false
        val a = this.get() ?: return false
        val b = other.get() ?: return false
        return a === b
    }

    /**
     * Returns the original hash code of the referent at the time of construction. This is stable
     * and does not change even if the referent is collected.
     */
    override fun hashCode(): Int = originalHashCode

    /**
     * Returns a string representation of the WeakWrapper. If the referent is still alive, it shows
     * its class name and identity hash code. If the referent has been collected, it shows
     * "<collected>".
     */
    override fun toString(): String {
        val v = get()
        return if (v != null) "WeakWrapper(${v::class.simpleName}@${System.identityHashCode(v)})"
        else "WeakWrapper(<collected>)"
    }
}
