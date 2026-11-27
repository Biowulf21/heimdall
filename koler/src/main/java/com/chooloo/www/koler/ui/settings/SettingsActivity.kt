package com.chooloo.www.koler.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.common.BaseActivity
import com.chooloo.www.chooloolib.ui.preferences.AppNameChoice
import com.chooloo.www.chooloolib.ui.preferences.PreferencesScreen
import com.chooloo.www.chooloolib.ui.theme.Sizes
import com.chooloo.www.koler.domain.repository.CallRepository
import com.chooloo.www.koler.ui.main.LauncherName
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Settings as its own page, opened from the home drawer. */
@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    @Inject lateinit var callRepository: CallRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setChoolooContent(heimdall = true) {
            var isBrandName by remember { mutableStateOf(LauncherName.isBrand(this)) }
            val callBlocksSwitch =
                stringResource(com.chooloo.www.koler.R.string.app_name_switch_in_call)
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(Sizes.Bar),
                ) {
                    IconButton(onClick = ::finish) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(com.chooloo.www.koler.R.string.contact_back),
                        )
                    }
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                PreferencesScreen(
                    showDialerPreferences = true,
                    appName = AppNameChoice(
                        defaultName = stringResource(
                            com.chooloo.www.koler.R.string.launcher_name,
                        ),
                        brandName = stringResource(com.chooloo.www.koler.R.string.app_name),
                        isBrand = isBrandName,
                        onSelect = { brand ->
                            // Toggling a component during a call risks the system restarting
                            // the dialer.
                            if (callRepository.calls.value.isNotEmpty()) {
                                Toast.makeText(
                                    this@SettingsActivity,
                                    callBlocksSwitch,
                                    Toast.LENGTH_SHORT,
                                )
                                    .show()
                            } else if (brand != isBrandName) {
                                LauncherName.apply(this@SettingsActivity, brand)
                                isBrandName = brand
                            }
                        },
                    ),
                )
            }
        }
    }
}
