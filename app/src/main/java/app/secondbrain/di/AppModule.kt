package app.secondbrain.di

import android.content.Context
import androidx.room.Room
import app.secondbrain.data.db.AppDatabase
import app.secondbrain.data.db.CategoryDao
import app.secondbrain.data.db.ItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "secondbrain.db").build()

    @Provides fun itemDao(db: AppDatabase): ItemDao = db.itemDao()
    @Provides fun categoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
}
