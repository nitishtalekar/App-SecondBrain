package app.secondbrain.brain

import app.secondbrain.data.Entry
import app.secondbrain.tagging.TagKind

data class Cluster(
    /** The tag that names this cluster, or [Clusterer.LOOSE] for things that share nothing yet. */
    val tag: String,
    /** Newest first. */
    val entries: List<Entry>,
    /** Index into the cluster palette. Clusters get different colours while there are enough. */
    val color: Int = 0,
)

/** Two clusters that share tags; [strength] is 0..1. */
data class ClusterLink(val a: Int, val b: Int, val strength: Float)

data class BrainMap(val clusters: List<Cluster> = emptyList(), val links: List<ClusterLink> = emptyList())

/**
 * Groups entries by greedy tag cover: repeatedly take the tag that best explains the entries not yet
 * placed. Deterministic, and every cluster is simply "things tagged X".
 */
object Clusterer {

    const val LOOSE = "loose ends"
    private const val MAX_CLUSTERS = 14
    private const val MAX_LINKS = 22
    const val COLORS = 10

    private val weights = mapOf(
        TagKind.MINE to 1.4,
        TagKind.TOPIC to 1.0,
        TagKind.KEYWORD to 0.55,
        TagKind.SOURCE to 0.4,
    )

    fun build(entries: List<Entry>, maxClusters: Int = MAX_CLUSTERS): BrainMap {
        if (entries.isEmpty()) return BrainMap()
        val newestFirst = entries.sortedByDescending { it.item.createdAt }

        val members = HashMap<String, MutableList<Entry>>()
        val weight = HashMap<String, Double>()
        for (entry in newestFirst) {
            for (tag in entry.tags) {
                val w = weights[tag.kind] ?: continue
                members.getOrPut(tag.name) { mutableListOf() } += entry
                weight[tag.name] = maxOf(weight[tag.name] ?: 0.0, w)
            }
        }

        val unplaced = newestFirst.map { it.item.id }.toMutableSet()
        val chosen = LinkedHashMap<String, MutableList<Entry>>()
        while (chosen.size < maxClusters && unplaced.isNotEmpty()) {
            val best = members.keys.asSequence()
                .filter { it !in chosen }
                .mapNotNull { name ->
                    val fresh = members.getValue(name).count { it.item.id in unplaced }
                    // A word or website shared by only one thing is not a group.
                    val needed = if (weight.getValue(name) >= 1.0) 1 else 2
                    if (fresh < needed) null else name to fresh * weight.getValue(name)
                }
                .sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenBy { it.first })
                .firstOrNull()?.first ?: break
            val taken = members.getValue(best).filter { it.item.id in unplaced }
            chosen[best] = taken.toMutableList()
            taken.forEach { unplaced -= it.item.id }
        }

        val loose = mutableListOf<Entry>()
        for (entry in newestFirst) {
            if (entry.item.id !in unplaced) continue
            val home = entry.tags.firstOrNull { it.name in chosen }?.name
            if (home != null) chosen.getValue(home) += entry else loose += entry
        }

        val grouped = chosen.map { (tag, list) -> Cluster(tag, list.sortedByDescending { it.item.createdAt }) }
            .sortedWith(compareByDescending<Cluster> { it.entries.size }.thenBy { it.tag })
        val clusters = coloured(if (loose.isEmpty()) grouped else grouped + Cluster(LOOSE, loose))

        return BrainMap(clusters, links(clusters))
    }

    /** Each tag prefers its own colour (so it stays put as the brain grows) and steps past taken ones. */
    private fun coloured(clusters: List<Cluster>): List<Cluster> {
        val uses = IntArray(COLORS)
        return clusters.map { cluster ->
            val preferred = Math.floorMod(cluster.tag.hashCode(), COLORS)
            val pick = (0 until COLORS).map { (preferred + it) % COLORS }.minBy { uses[it] }
            uses[pick]++
            cluster.copy(color = pick)
        }
    }

    private fun links(clusters: List<Cluster>): List<ClusterLink> {
        val profiles = clusters.map { cluster ->
            cluster.entries.flatMap { entry -> entry.tags.filter { it.kind in weights }.map { it.name } }.toSet()
        }
        val out = mutableListOf<ClusterLink>()
        for (a in clusters.indices) {
            for (b in a + 1 until clusters.size) {
                val shared = profiles[a].count { it in profiles[b] }
                if (shared == 0) continue
                val smaller = minOf(profiles[a].size, profiles[b].size).coerceAtLeast(1)
                out += ClusterLink(a, b, (shared.toFloat() / smaller).coerceAtMost(1f))
            }
        }
        return out.sortedByDescending { it.strength }.take(MAX_LINKS)
    }
}
