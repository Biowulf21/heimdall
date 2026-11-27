package com.chooloo.www.chooloolib.ui.preferences

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.domain.enum.DesignStyle
import com.chooloo.www.chooloolib.domain.enum.Page
import com.chooloo.www.chooloolib.domain.enum.ThemeMode
import com.chooloo.www.chooloolib.ui.common.Labels
import com.chooloo.www.chooloolib.ui.components.list.ItemList

private enum class PreferenceSection(@StringRes val titleRes: Int) {
    APPEARANCE(R.string.pref_section_appearance),
    DIALER(R.string.pref_section_dialer),
    BLOCKING(R.string.pref_section_blocking),
    ABOUT(R.string.pref_section_about),
}

private enum class PreferenceItem(
    val section: PreferenceSection,
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int? = null,
    val isDialerOnly: Boolean = false,
    // The dialer reaches these from its home drawer, so its Settings page leaves them out.
    val isInDialerDrawer: Boolean = false,
) {
    THEME_MODE(PreferenceSection.APPEARANCE, Icons.Rounded.Palette, R.string.pref_title_theme_mode),
    DESIGN_STYLE(
        PreferenceSection.APPEARANCE,
        Icons.Rounded.AutoAwesome,
        R.string.pref_title_design_style,
    ),
    APP_NAME(
        PreferenceSection.APPEARANCE,
        Icons.Rounded.TextFields,
        R.string.pref_title_app_name,
        R.string.pref_app_name_description,
    ),
    THEME_COLOR(
        PreferenceSection.APPEARANCE,
        Icons.Rounded.Palette,
        R.string.pref_title_theme_color,
        isDialerOnly = true,
    ),
    LEFT_HANDED(
        PreferenceSection.APPEARANCE,
        Icons.Rounded.SwapHoriz,
        R.string.pref_left_handed,
        isDialerOnly = true,
    ),
    CIRCULAR_LETTER_PREVIEW(
        PreferenceSection.APPEARANCE,
        Icons.Rounded.Circle,
        R.string.pref_circular_letter_preview,
        R.string.pref_circular_letter_preview_description,
        isDialerOnly = true,
    ),
    PHONE_SETUP(
        PreferenceSection.DIALER,
        Icons.Rounded.Phone,
        R.string.pref_phone_setup,
        isDialerOnly = true,
    ),
    DEFAULT_PAGE(
        PreferenceSection.DIALER,
        Icons.Rounded.Home,
        R.string.pref_title_default_page,
        isDialerOnly = true,
    ),
    GROUP_RECENTS_ENABLED(
        PreferenceSection.DIALER,
        Icons.AutoMirrored.Rounded.ViewList,
        R.string.pref_title_group_recents,
        isDialerOnly = true,
    ),
    DIALPAD_TONES_ENABLED(
        PreferenceSection.DIALER,
        Icons.Rounded.MusicNote,
        R.string.pref_title_dialpad_tones,
        isDialerOnly = true,
    ),
    DIALPAD_VIBRATE_ENABLED(
        PreferenceSection.DIALER,
        Icons.Rounded.Vibration,
        R.string.pref_title_dialpad_vibrate,
        isDialerOnly = true,
    ),
    CLEAR_RECENTS(
        PreferenceSection.DIALER,
        Icons.Rounded.DeleteSweep,
        R.string.action_clear_recents,
        R.string.explain_clear_recents,
        isDialerOnly = true,
    ),
    MANAGE_BLOCKED(
        PreferenceSection.BLOCKING,
        Icons.Rounded.Block,
        R.string.pref_title_manage_blocked,
        isDialerOnly = true,
        isInDialerDrawer = true,
    ),
    RATE(PreferenceSection.ABOUT, Icons.Rounded.Star, R.string.pref_title_rate),
    EMAIL(PreferenceSection.ABOUT, Icons.Rounded.Email, R.string.pref_title_email),
    REPORT_BUGS(
        PreferenceSection.ABOUT,
        Icons.Rounded.BugReport,
        R.string.pref_title_report_bugs,
        isInDialerDrawer = true,
    ),
    DONATE(
        PreferenceSection.ABOUT,
        Icons.Rounded.Favorite,
        R.string.pref_title_donate,
        isInDialerDrawer = true,
    ),
}

/** Launcher-name choice; only the app that owns the launcher aliases passes one. */
class AppNameChoice(
    val defaultName: String,
    val brandName: String,
    val isBrand: Boolean,
    val onSelect: (isBrand: Boolean) -> Unit,
)

@Composable
fun Preferences(
    defaultPage: Page,
    themeMode: ThemeMode,
    designStyle: DesignStyle,
    onDesignStyle: (DesignStyle) -> Unit,
    appName: AppNameChoice?,
    isGroupRecentsEnabled: Boolean,
    isDialpadTonesEnabled: Boolean,
    isDialpadVibrateEnabled: Boolean,
    showDialerPreferences: Boolean,
    onDefaultPage: (Page) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    leftHanded: Boolean,
    onLeftHanded: (Boolean) -> Unit,
    circularLetterPreview: Boolean,
    onCircularLetterPreview: (Boolean) -> Unit,
    onPhoneSetup: (() -> Unit)?,
    accentRgb: Int,
    onAccentRgb: (Int) -> Unit,
    onIsGroupRecentsEnabled: (Boolean) -> Unit,
    onIsDialpadTonesEnabled: (Boolean) -> Unit,
    onIsDialpadVibrateEnabled: (Boolean) -> Unit,
    onManageBlockedClick: () -> Unit,
    onClearRecentsClick: () -> Unit,
    onRateClick: () -> Unit,
    onEmailClick: () -> Unit,
    onReportBugsClick: () -> Unit,
    onDonateClick: () -> Unit,
) {
    var isThemeOpen by rememberSaveable { mutableStateOf(false) }
    if (isThemeOpen) {
        ThemeColorDialog(accentRgb, onAccentRgb) { isThemeOpen = false }
    }

    val preferences = PreferenceItem.entries.filter {
        (showDialerPreferences || !it.isDialerOnly) &&
            !(showDialerPreferences && it.isInDialerDrawer) &&
            (it != PreferenceItem.PHONE_SETUP || onPhoneSetup != null) &&
            (it != PreferenceItem.APP_NAME || appName != null)
    }
    val sectionTitles = PreferenceSection.entries.associateWith { stringResource(it.titleRes) }

    ItemList(
        items = preferences,
        key = { it.name },
        header = { sectionTitles.getValue(it.section) },
        item = { preference ->
            val title = stringResource(preference.titleRes)
            val icon = preference.icon
            val description = preference.descriptionRes?.let { stringResource(it) }

            when (preference) {
                PreferenceItem.THEME_MODE -> ChoicePreferenceListItem(
                    icon = icon,
                    title = title,
                    values = ThemeMode.entries,
                    selectedValue = themeMode,
                    label = { stringResource(Labels.titleRes(it)) },
                    onValueSelected = onThemeMode,
                )

                PreferenceItem.DESIGN_STYLE -> ChoicePreferenceListItem(
                    icon = icon,
                    title = title,
                    values = DesignStyle.entries,
                    selectedValue = designStyle,
                    label = { stringResource(Labels.titleRes(it)) },
                    onValueSelected = onDesignStyle,
                )

                PreferenceItem.APP_NAME -> appName?.let { choice ->
                    ChoicePreferenceListItem(
                        icon = icon,
                        title = title,
                        values = listOf(false, true),
                        selectedValue = choice.isBrand,
                        label = { if (it) choice.brandName else choice.defaultName },
                        onValueSelected = choice.onSelect,
                        description = description,
                    )
                }

                PreferenceItem.THEME_COLOR -> ActionPreferenceListItem(
                    icon,
                    title,
                    accentHex(accentRgb),
                ) { isThemeOpen = true }

                PreferenceItem.LEFT_HANDED -> SwitchPreferenceListItem(
                    icon,
                    title,
                    leftHanded,
                    onLeftHanded,
                )

                PreferenceItem.CIRCULAR_LETTER_PREVIEW -> SwitchPreferenceListItem(
                    icon,
                    title,
                    circularLetterPreview,
                    onCircularLetterPreview,
                    subtitle = description,
                )

                PreferenceItem.PHONE_SETUP -> onPhoneSetup?.let {
                    ActionPreferenceListItem(icon, title, null, it)
                }

                PreferenceItem.DEFAULT_PAGE -> ChoicePreferenceListItem(
                    icon = icon,
                    title = title,
                    values = Page.entries,
                    selectedValue = defaultPage,
                    label = { stringResource(Labels.titleRes(it)) },
                    onValueSelected = onDefaultPage,
                )

                PreferenceItem.GROUP_RECENTS_ENABLED ->
                    SwitchPreferenceListItem(
                        icon,
                        title,
                        isGroupRecentsEnabled,
                        onIsGroupRecentsEnabled,
                    )

                PreferenceItem.DIALPAD_TONES_ENABLED ->
                    SwitchPreferenceListItem(
                        icon,
                        title,
                        isDialpadTonesEnabled,
                        onIsDialpadTonesEnabled,
                    )

                PreferenceItem.DIALPAD_VIBRATE_ENABLED ->
                    SwitchPreferenceListItem(
                        icon,
                        title,
                        isDialpadVibrateEnabled,
                        onIsDialpadVibrateEnabled,
                    )

                PreferenceItem.MANAGE_BLOCKED -> ActionPreferenceListItem(
                    icon,
                    title,
                    description,
                    onManageBlockedClick,
                )

                PreferenceItem.CLEAR_RECENTS -> ActionPreferenceListItem(
                    icon,
                    title,
                    description,
                    onClearRecentsClick,
                )

                PreferenceItem.RATE -> ActionPreferenceListItem(
                    icon,
                    title,
                    description,
                    onRateClick,
                )

                PreferenceItem.EMAIL -> ActionPreferenceListItem(
                    icon,
                    title,
                    description,
                    onEmailClick,
                )

                PreferenceItem.REPORT_BUGS -> ActionPreferenceListItem(
                    icon,
                    title,
                    description,
                    onReportBugsClick,
                )

                PreferenceItem.DONATE -> ActionPreferenceListItem(
                    icon,
                    title,
                    description,
                    onDonateClick,
                )
            }
        },
    )
}
