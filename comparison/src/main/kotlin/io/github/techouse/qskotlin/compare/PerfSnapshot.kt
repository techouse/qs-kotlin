package io.github.techouse.qskotlin.compare

import com.sun.management.ThreadMXBean
import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.EncodeOptions
import java.lang.management.ManagementFactory

private data class PerfResult(val msPerOp: Double, val allocBytesPerOp: Long?)

private data class DecodeCase(
    val count: Int,
    val comma: Boolean,
    val utf8: Boolean,
    val valueLen: Int,
    val iterations: Int,
)

private val threadMxBean: ThreadMXBean? =
    (ManagementFactory.getThreadMXBean() as? ThreadMXBean)?.also { bean ->
        if (bean.isThreadAllocatedMemorySupported && !bean.isThreadAllocatedMemoryEnabled) {
            bean.isThreadAllocatedMemoryEnabled = true
        }
    }

@Suppress("ExplicitGarbageCollectionCall")
private fun runGcPause() {
    System.gc()
    Thread.sleep(25L)
}

private fun currentThreadAllocatedBytesOrNull(): Long? {
    val bean = threadMxBean ?: return null
    if (!bean.isThreadAllocatedMemorySupported || !bean.isThreadAllocatedMemoryEnabled) {
        return null
    }
    return bean.getThreadAllocatedBytes(Thread.currentThread().id)
}

private fun median(values: MutableList<Double>): Double {
    values.sort()
    return values[values.size / 2]
}

private fun median(values: MutableList<Long>): Long {
    values.sort()
    return values[values.size / 2]
}

private fun buildNested(depth: Int): Map<String, Any?> {
    var current: Map<String, Any?> = mapOf("leaf" to "x")
    repeat(depth) { current = mapOf("a" to current) }
    return current
}

private fun makeValue(length: Int, seed: Int): String {
    val out = StringBuilder(length)
    var state: UInt = (seed.toUInt() * 2654435761u) + 1013904223u
    repeat(length) {
        state = state xor (state shl 13)
        state = state xor (state shr 17)
        state = state xor (state shl 5)

        val x = (state % 62u).toInt()
        val ch =
            when {
                x < 10 -> ('0'.code + x).toChar()
                x < 36 -> ('A'.code + (x - 10)).toChar()
                else -> ('a'.code + (x - 36)).toChar()
            }
        out.append(ch)
    }
    return out.toString()
}

private fun buildQuery(
    count: Int,
    commaLists: Boolean,
    utf8Sentinel: Boolean,
    valueLen: Int,
): String {
    val pairs = ArrayList<String>(count + if (utf8Sentinel) 1 else 0)
    if (utf8Sentinel) {
        pairs += "utf8=%E2%9C%93"
    }

    repeat(count) { i ->
        val key = "k$i"
        val value = if (commaLists && i % 10 == 0) "a,b,c" else makeValue(valueLen, i)
        pairs += "$key=$value"
    }

    return pairs.joinToString("&")
}

private fun measureEncode(depth: Int, iterations: Int): Pair<PerfResult, Int> {
    val payload = buildNested(depth)
    val options = EncodeOptions(encode = false)

    repeat(5) { encode(payload, options) }

    val times = ArrayList<Double>(7)
    val allocs = ArrayList<Long>(7)
    var outLength = 0

    repeat(7) {
        runGcPause()
        val before = currentThreadAllocatedBytesOrNull()
        val start = System.nanoTime()
        var encoded = ""
        repeat(iterations) { encoded = encode(payload, options) }
        val elapsed = System.nanoTime() - start
        val after = currentThreadAllocatedBytesOrNull()
        outLength = encoded.length

        times += (elapsed / 1_000_000.0) / iterations
        if (before != null && after != null) {
            allocs += (after - before) / iterations
        }
    }

    val result =
        PerfResult(
            msPerOp = median(times),
            allocBytesPerOp = allocs.takeIf { it.isNotEmpty() }?.let { median(it) },
        )

    return result to outLength
}

private fun measureDecode(
    count: Int,
    commaLists: Boolean,
    utf8Sentinel: Boolean,
    valueLen: Int,
    iterations: Int,
): Pair<PerfResult, Int> {
    val query = buildQuery(count, commaLists, utf8Sentinel, valueLen)
    val options =
        DecodeOptions(
            comma = commaLists,
            parseLists = true,
            parameterLimit = Int.MAX_VALUE,
            throwOnLimitExceeded = false,
            interpretNumericEntities = false,
            charsetSentinel = utf8Sentinel,
            ignoreQueryPrefix = false,
        )

    repeat(5) { decode(query, options) }

    val times = ArrayList<Double>(7)
    val allocs = ArrayList<Long>(7)
    var keyCount = 0

    repeat(7) {
        runGcPause()
        val before = currentThreadAllocatedBytesOrNull()
        val start = System.nanoTime()
        var decoded = emptyMap<String, Any?>()
        repeat(iterations) { decoded = decode(query, options) }
        val elapsed = System.nanoTime() - start
        val after = currentThreadAllocatedBytesOrNull()
        keyCount = decoded.size

        times += (elapsed / 1_000_000.0) / iterations
        if (before != null && after != null) {
            allocs += (after - before) / iterations
        }
    }

    val result =
        PerfResult(
            msPerOp = median(times),
            allocBytesPerOp = allocs.takeIf { it.isNotEmpty() }?.let { median(it) },
        )

    return result to keyCount
}

private fun formatAllocMib(bytes: Long?): String {
    if (bytes == null) return "n/a"
    return "%8.2f MiB/op".format(bytes / (1024.0 * 1024.0))
}

private fun formatAllocKib(bytes: Long?): String {
    if (bytes == null) return "n/a"
    return "%8.1f KiB/op".format(bytes / 1024.0)
}

internal fun runPerfSnapshot() {
    println("Kotlin qs-kotlin perf snapshot (median of 7 samples)")
    println("Encode (encode=false, deep nesting):")

    listOf(2000, 5000, 12000).forEach { depth ->
        val iterations = if (depth >= 12000) 8 else 20
        val (result, outLength) = measureEncode(depth, iterations)
        println(
            "  depth=%5d: %8.3f ms/op | %s | len=%d"
                .format(depth, result.msPerOp, formatAllocMib(result.allocBytesPerOp), outLength)
        )
    }

    println("Decode (public API):")
    val cases =
        listOf(
            DecodeCase(count = 100, comma = false, utf8 = false, valueLen = 8, iterations = 120),
            DecodeCase(count = 1000, comma = false, utf8 = false, valueLen = 40, iterations = 16),
            DecodeCase(count = 1000, comma = true, utf8 = true, valueLen = 40, iterations = 16),
        )

    cases.forEach { c ->
        val (result, keyCount) = measureDecode(c.count, c.comma, c.utf8, c.valueLen, c.iterations)
        println(
            "  count=%4d, comma=%-5s, utf8=%-5s, len=%2d: %7.3f ms/op | %s | keys=%d"
                .format(
                    c.count,
                    c.comma,
                    c.utf8,
                    c.valueLen,
                    result.msPerOp,
                    formatAllocKib(result.allocBytesPerOp),
                    keyCount,
                )
        )
    }
}
