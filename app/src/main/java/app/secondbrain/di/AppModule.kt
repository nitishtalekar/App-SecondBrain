package app.secondbrain.di

import android.content.Context
import androidx.room.Room
import app.secondbrain.data.db.AppDatabase
import app.secondbrain.data.db.ItemDao
import app.secondbrain.data.db.MIGRATION_1_2
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
        Room.databaseBuilder(context, AppDatabase::class.java, "secondbrain.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun itemDao(db: AppDatabase): ItemDao = db.itemDao()
}
