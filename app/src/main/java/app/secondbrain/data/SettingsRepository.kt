package app.secondbrain.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")

    private val accentKey = stringPreferencesKey("accent")

    private val layoutKey = stringPreferencesKey("library_layout")

    val libraryLayout: Flow<LibraryLayout> = context.dataStore.data.map { prefs ->
        LibraryLayout.entries.firstOrNull { it.name == prefs[layoutKey] } ?: LibraryLayout.GRID
    }

    suspend fun setLibraryLayout(layout: LibraryLayout) {
        context.dataStore.edit { it[layoutKey] = layout.name }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val accent: Flow<Accent> = context.dataStore.data.map { prefs ->
        Accent.entries.firstOrNull { it.name == prefs[accentKey] } ?: Accent.Default
    }

    suspend fun setAccent(accent: Accent) {
        context.dataStore.edit { it[accentKey] = accent.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }
}
