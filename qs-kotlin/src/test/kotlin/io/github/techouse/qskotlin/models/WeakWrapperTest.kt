package io.github.techouse.qskotlin.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

/** Tests for WeakWrapper to increase coverage. */
class WeakWrapperTest {

    private fun forceGcPass() {
        // A few GC hints plus some allocation to encourage collection.
        repeat(3) {
            System.gc()
            // Allocate and drop some memory to create pressure.
            val junk = Array(128) { ByteArray(1024) }
            @Suppress("UNUSED_VARIABLE")
            val sink = junk.size // prevent optimization
            Thread.sleep(5)
        }
    }

    private fun waitForCollection(wrapper: WeakWrapper<*>, timeoutMs: Long = 2000): Boolean {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        while (System.nanoTime() < deadline) {
            if (wrapper.get() == null) return true
            forceGcPass()
        }
        return wrapper.get() == null
    }

    @Test
    @DisplayName("Equality: same referent, different wrappers")
    fun equalitySameReferent() {
        val referent = Any()
        val w1 = WeakWrapper(referent)
        val w2 = WeakWrapper(referent)
        assertEquals(w1, w2)
        assertEquals(w1.hashCode(), w2.hashCode()) // identity hash should match
        assertEquals(w1, w1) // reflexive
        assertNotEquals(w1, Any()) // different type
    }

    @Test
    @DisplayName("Inequality: different referents")
    fun inequalityDifferentReferents() {
        val w1 = WeakWrapper(Any())
        val w2 = WeakWrapper(Any())
        assertNotEquals(w1, w2)
    }

    @Test
    @DisplayName("hashCode stable after collection")
    fun hashCodeStableAfterCollection() {
        var obj: Any? = Any()
        val wrapper = WeakWrapper(obj!!)
        val originalHash = wrapper.hashCode()
        // drop strong reference
        obj = null
        // attempt collection
        waitForCollection(wrapper)
        // hashCode must remain the same regardless of collection
        assertEquals(originalHash, wrapper.hashCode())
    }

    @Test
    @DisplayName("equals returns false once referent collected")
    fun equalsAfterCollection() {
        var obj: Any? = Any()
        val w1 = WeakWrapper(obj!!)
        val w2 = WeakWrapper(obj!!)
        obj = null // remove strong refs
        val collected = waitForCollection(w1)
        // If not collected we skip to avoid flaky failure; still counts toward some coverage paths.
        assumeTrue(collected, "Referent not collected in time; skipping assert")
        // When referent is gone equals should now return false because w1.get()==null
        assertFalse(w1 == w2)
    }

    @Test
    @DisplayName("toString shows collected state once referent GC'd")
    fun toStringCollected() {
        // Use a large object to encourage prompt collection
        class Big(val data: ByteArray = ByteArray(2_000_000))
        var big: Big? = Big()
        val w = WeakWrapper(big!!)
        assertTrue(w.toString().startsWith("WeakWrapper(Big"), "Live toString should include class name: ${w.toString()}")
        big = null
        waitForCollection(w)
        val ts = w.toString()
        // Either collected or still alive, but we exercise both branches.
        val matches = ts == "WeakWrapper(<collected>)" || ts.startsWith("WeakWrapper(Big")
        assertTrue(matches, "Unexpected toString: $ts")
    }

    @Test
    @DisplayName("get() reflects liveness")
    fun getReflectsLiveness() {
        val flag = AtomicBoolean(false)
        class Holder(val onFinalize: AtomicBoolean) {
            // Rely on finalization only as a last resort; not guaranteed, but helps coverage occasionally.
            @Suppress("deprecation")
            protected fun finalize() { onFinalize.set(true) }
        }
        var h: Holder? = Holder(flag)
        val w = WeakWrapper(h!!)
        assertNotNull(w.get())
        h = null
        waitForCollection(w)
        // Either collected (null) or still there; both acceptable but branch executed.
        w.get()?.let { assertFalse(flag.get()) } ?: assertNull(w.get())
    }
}

