package com.vibecode.minitodo.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibecode.minitodo.domain.SortMode
import com.vibecode.minitodo.domain.ThemeMode
import com.vibecode.minitodo.domain.ThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val sortMode: SortMode = SortMode.EARLIEST_START,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themePalette: ThemePalette = ThemePalette.DYNAMIC,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SORT_MODE = stringPreferencesKey("sort_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val LEGACY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
    }

    private val data = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }

    val notificationPermissionAsked: Flow<Boolean> = data
        .map { preferences -> preferences[Keys.NOTIFICATION_PERMISSION_ASKED] ?: false }

    val settings: Flow<AppSettings> = data
        .map { preferences ->
            AppSettings(
                sortMode = preferences[Keys.SORT_MODE]
                    ?.let { name -> SortMode.entries.firstOrNull { it.name == name } }
                    ?: SortMode.EARLIEST_START,
                themeMode = preferences[Keys.THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                themePalette = preferences[Keys.THEME_PALETTE]
                    ?.let { name -> ThemePalette.entries.firstOrNull { it.name == name } }
                    ?: when (preferences[Keys.LEGACY_DYNAMIC_COLOR]) {
                        false -> ThemePalette.BLUE
                        else -> ThemePalette.DYNAMIC
                    },
            )
        }

    suspend fun setSortMode(mode: SortMode) {
        context.settingsDataStore.edit { it[Keys.SORT_MODE] = mode.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setThemePalette(palette: ThemePalette) {
        context.settingsDataStore.edit { it[Keys.THEME_PALETTE] = palette.name }
    }

    suspend fun markNotificationPermissionAsked() {
        context.settingsDataStore.edit { it[Keys.NOTIFICATION_PERMISSION_ASKED] = true }
    }
}
