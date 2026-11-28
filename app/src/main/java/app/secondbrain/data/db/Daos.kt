package app.secondbrain.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): ItemEntity?

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE needsAnalysis = 1")
    suspend fun getNeedingAnalysis(): List<ItemEntity>

    @Query("SELECT id FROM items WHERE taggerVersion < :version")
    suspend fun getTaggedBefore(version: Int): List<Long>

    @Query("SELECT COUNT(*) FROM items WHERE imagePath = :path")
    suspend fun countImageUses(path: String): Int

    @Insert
    suspend fun insert(item: ItemEntity): Long

    /** All posts from one capture land together or not at all. */
    @Transaction
    suspend fun insertAll(items: List<ItemEntity>): List<Long> = items.map { insert(it) }

    @Update
    suspend fun update(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)
}
