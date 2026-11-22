package app.secondbrain.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun observeByCategory(categoryId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE categoryId IS NULL AND status != 'PENDING' ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<ItemEntity>>

    @Query("SELECT categoryId, COUNT(*) AS count FROM items WHERE status != 'PENDING' GROUP BY categoryId")
    fun observeCounts(): Flow<List<CategoryCount>>

    @Query("SELECT * FROM items WHERE searchText LIKE '%' || :term || '%' ESCAPE '!' ORDER BY createdAt DESC")
    fun search(term: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE status = 'PENDING' OR status = 'FAILED'")
    suspend fun getUnprocessed(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE categoryLocked = 0 AND status = 'DONE'")
    suspend fun getAutoSorted(): List<ItemEntity>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE items SET categoryId = NULL, categoryLocked = 0 WHERE categoryId = :categoryId")
    suspend fun releaseCategory(categoryId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories")
    suspend fun maxOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE categories SET parentId = :newParent WHERE parentId = :oldParent")
    suspend fun reparentChildren(oldParent: String, newParent: String?)

    @Query("SELECT parentId FROM categories WHERE id = :id")
    suspend fun parentOf(id: String): String?

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)
}
