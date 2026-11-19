package com.chooloo.www.chooloolib.domain.repository

import com.chooloo.www.chooloolib.domain.enum.Page
import com.chooloo.www.chooloolib.domain.enum.ThemeMode
import com.chooloo.www.chooloolib.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    val settings: Flow<Settings>

    suspend fun setDefaultPage(page: Page)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setLeftHanded(enabled: Boolean)
    suspend fun completePhoneSetup()
    suspend fun setAccentRgb(rgb: Int)
    suspend fun setIsGroupRecentsEnabled(isEnabled: Boolean)
    suspend fun setIsDialpadTonesEnabled(isEnabled: Boolean)
    suspend fun setIsDialpadVibrateEnabled(isEnabled: Boolean)
}
