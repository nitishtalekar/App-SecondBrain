package app.secondbrain.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Folders to tags. Every item keeps its content, and the names of the folder it sat in (and that
 * folder's parents) plus its old tags are parked in `legacyTags` to become tags on next launch.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `items_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`type` TEXT NOT NULL, `title` TEXT, `body` TEXT NOT NULL, `url` TEXT, `imagePath` TEXT, " +
                "`ocrText` TEXT, `imageLabels` TEXT, `linkTitle` TEXT, `linkDescription` TEXT, `linkSiteName` TEXT, " +
                "`linkKeywords` TEXT, `linkImageUrl` TEXT, `tagsJson` TEXT NOT NULL DEFAULT '[]', " +
                "`removedTags` TEXT NOT NULL DEFAULT '', `legacyTags` TEXT NOT NULL DEFAULT '', `action` TEXT, " +
                "`completedAt` INTEGER, `groupId` TEXT, `needsAnalysis` INTEGER NOT NULL DEFAULT 0, " +
                "`scanned` INTEGER NOT NULL DEFAULT 0, `taggerVersion` INTEGER NOT NULL DEFAULT 0, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
        )
        val shared = "id, type, title, body, url, imagePath, ocrText, imageLabels, linkTitle, linkDescription, " +
            "linkSiteName, linkKeywords, linkImageUrl, createdAt, updatedAt"
        db.execSQL(
            "INSERT INTO items_new ($shared, needsAnalysis) " +
                "SELECT $shared, CASE WHEN status != 'DONE' AND type != 'TEXT' THEN 1 ELSE 0 END FROM items",
        )

        val folders = HashMap<String, Pair<String, String?>>()
        db.query("SELECT id, name, parentId FROM categories").use { c ->
            while (c.moveToNext()) folders[c.getString(0)] = c.getString(1) to (if (c.isNull(2)) null else c.getString(2))
        }
        db.query("SELECT id, categoryId, tags FROM items").use { c ->
            while (c.moveToNext()) {
                val names = LinkedHashSet<String>()
                c.getString(2).split(',').filter { it.isNotBlank() }.forEach { names += it.trim() }
                var folder = if (c.isNull(1)) null else c.getString(1)
                val seen = HashSet<String>()
                while (folder != null && seen.add(folder)) {
                    val (name, parent) = folders[folder] ?: break
                    names += name
                    folder = parent
                }
                if (names.isNotEmpty()) {
                    db.execSQL(
                        "UPDATE items_new SET legacyTags = ? WHERE id = ?",
                        arrayOf<Any>(names.joinToString("\n"), c.getLong(0)),
                    )
                }
            }
        }

        db.execSQL("DROP TABLE items")
        db.execSQL("DROP TABLE categories")
        db.execSQL("ALTER TABLE items_new RENAME TO items")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_createdAt` ON `items` (`createdAt`)")
    }
}
