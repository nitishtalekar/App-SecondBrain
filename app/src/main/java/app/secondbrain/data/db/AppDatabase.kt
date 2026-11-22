package app.secondbrain.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ItemEntity::class, CategoryEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
}
