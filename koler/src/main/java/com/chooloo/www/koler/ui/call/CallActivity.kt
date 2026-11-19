package com.chooloo.www.koler.ui.call

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.chooloo.www.chooloolib.ui.common.BaseActivity
import com.chooloo.www.koler.data.repository.CallRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CallActivity : BaseActivity() {
    @Inject lateinit var callRepository: CallRepositoryImpl

    private val callViewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLocked()
        if (savedInstanceState == null) showKeypadIfRequested(intent)
        finishWhenCallsEnd()
        setChoolooContent(heimdall = true) {
            val videoCalls by callRepository.videoCalls.collectAsStateWithLifecycle()
            CallScreen(callViewModel = callViewModel, videoCalls = videoCalls)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showKeypadIfRequested(intent)
    }

    override fun finish() {
        finishAndRemoveTask()
    }

    private fun finishWhenCallsEnd() {
        lifecycleScope.launch {
            callViewModel.awaitCallsEnded()
            finish()
        }
    }

    private fun showWhenLocked() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
    }

    private fun showKeypadIfRequested(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_SHOW_DIALPAD, false)) callViewModel.onShowKeypad()
    }

    companion object {
        const val EXTRA_SHOW_DIALPAD = "show_dialpad"
    }
}
