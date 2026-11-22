package app.secondbrain.data

import android.content.Context
import app.secondbrain.classify.CategoryConfig
import app.secondbrain.classify.CategoryDef
import app.secondbrain.classify.Classifier
import app.secondbrain.classify.Rule
import app.secondbrain.data.db.CategoryDao
import app.secondbrain.data.db.CategoryEntity
import app.secondbrain.data.db.ItemDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val rulesSerializer = ListSerializer(Rule.serializer())

@Singleton
class CategoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: CategoryDao,
    private val itemDao: ItemDao,
    private val settings: SettingsRepository,
) {
    val categories: Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() > 0) return@withContext
        val text = context.assets.open(DEFAULTS_ASSET).bufferedReader().use { it.readText() }
        applyConfig(CategoryConfig.parse(text))
    }

    /** Upserts by id; categories missing from [config] are left alone. */
    suspend fun applyConfig(config: CategoryConfig) = withContext(Dispatchers.IO) {
        val start = dao.maxOrder() + 1
        val existing = dao.getAll().associateBy { it.id }
        dao.upsertAll(config.categories.mapIndexed { index, def ->
            CategoryEntity(
                id = def.id,
                name = def.name,
                color = def.color,
                sortOrder = existing[def.id]?.sortOrder ?: (start + index),
                parentId = def.parent,
                minScore = def.minScore,
                rulesJson = CategoryConfig.json.encodeToString(rulesSerializer, def.rules),
            )
        })
        settings.setThresholds(config.minScore, config.confidenceThreshold)
    }

    suspend fun exportConfig(): CategoryConfig = withContext(Dispatchers.IO) {
        val (minScore, threshold) = settings.thresholds() ?: (2.0 to 0.3)
        CategoryConfig(minScore = minScore, confidenceThreshold = threshold, categories = dao.getAll().map { it.toDef() })
    }

    suspend fun classifier(): Classifier = Classifier(exportConfig())

    suspend fun create(name: String, parentId: String? = null): String = withContext(Dispatchers.IO) {
        val existing = dao.getAll().map { it.id }.toSet()
        val base = name.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "category" }
        var id = base
        var n = 2
        while (id in existing) id = "$base-${n++}"
        val color = parentId?.let { pid -> dao.getAll().firstOrNull { it.id == pid }?.color }
            ?: PALETTE[existing.size % PALETTE.size]
        dao.upsert(CategoryEntity(id, name.trim(), color, dao.maxOrder() + 1, parentId))
        id
    }

    suspend fun rename(id: String, name: String) = withContext(Dispatchers.IO) { dao.rename(id, name.trim()) }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        // Subfolders move up a level; items filed directly here go back to be sorted.
        dao.reparentChildren(id, dao.parentOf(id))
        itemDao.releaseCategory(id)
        dao.delete(id)
    }

    companion object {
        const val DEFAULTS_ASSET = "categories.json"
        private val PALETTE = listOf("#C2693F", "#6F8F5E", "#4F86A0", "#B08A3E", "#8A6BA8", "#B85C7A", "#4E9A8B", "#8C8272")
    }
}

internal fun CategoryEntity.toDef() = CategoryDef(
    id = id,
    name = name,
    color = color,
    parent = parentId,
    minScore = minScore,
    rules = runCatching { CategoryConfig.json.decodeFromString(rulesSerializer, rulesJson) }.getOrDefault(emptyList()),
)
