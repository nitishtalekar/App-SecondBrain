package app.secondbrain.search

/**
 * A shuffled order that holds still: the same seed always places the same ids the same way, so the
 * feed doesn't jump when an item changes, and new items slot in without reshuffling the rest.
 */
object FeedOrder {
    fun <T> shuffle(list: List<T>, seed: Long, id: (T) -> Long): List<T> = list.sortedBy { mix(seed + id(it)) }

    /** SplitMix64: scatters neighbouring numbers far apart. */
    private fun mix(value: Long): Long {
        var z = value - 0x61C8864680B583EBL
        z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
        z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
        return z xor (z ushr 31)
    }
}
