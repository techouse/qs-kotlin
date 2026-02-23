package io.github.techouse.qskotlin.internal

/**
 * Linked-node representation of an encoder key path.
 *
 * Public behavior is logically immutable (append/create operations return nodes rather than mutate
 * path structure), but this type uses unsynchronized internal memoization (`dotEncoded`,
 * `materialized`) for speed. It is intended for single-threaded encoder traversal and should not be
 * shared across threads without external synchronization.
 */
internal class KeyPathNode
private constructor(private val parent: KeyPathNode?, private val segment: String) {
    private var dotEncoded: KeyPathNode? = null
    private var materialized: String? = null

    private val depth: Int = (parent?.depth ?: 0) + 1
    private val totalLength: Int = (parent?.totalLength ?: 0) + segment.length

    fun append(value: String): KeyPathNode {
        return if (value.isEmpty()) this else KeyPathNode(this, value)
    }

    fun asDotEncoded(): KeyPathNode {
        dotEncoded?.let {
            return it
        }

        val encodedSegment = replaceDots(segment)
        val encoded =
            if (parent == null) {
                if (encodedSegment === segment) {
                    this
                } else {
                    KeyPathNode(null, encodedSegment)
                }
            } else {
                val encodedParent = parent.asDotEncoded()
                if (encodedParent === parent && encodedSegment === segment) {
                    this
                } else {
                    KeyPathNode(encodedParent, encodedSegment)
                }
            }

        dotEncoded = encoded
        return encoded
    }

    fun materialize(): String {
        materialized?.let {
            return it
        }

        val out =
            if (parent == null) {
                segment
            } else if (depth == 2) {
                parent.segment + segment
            } else {
                val parts = arrayOfNulls<String>(depth)
                var index = depth - 1
                var node: KeyPathNode? = this
                while (node != null) {
                    parts[index--] = node.segment
                    node = node.parent
                }

                buildString(totalLength) {
                    for (part in parts) {
                        append(part)
                    }
                }
            }

        materialized = out
        return out
    }

    companion object {
        fun fromMaterialized(value: String): KeyPathNode = KeyPathNode(null, value)

        private fun replaceDots(value: String): String {
            return if (value.indexOf('.') >= 0) value.replace(".", "%2E") else value
        }
    }
}
