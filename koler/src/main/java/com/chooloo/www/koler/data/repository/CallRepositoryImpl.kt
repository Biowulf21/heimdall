package com.chooloo.www.koler.data.repository

import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.Call as TelecomCall
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.chooloo.www.koler.R
import com.chooloo.www.koler.data.mapper.CallMapper
import com.chooloo.www.koler.domain.enum.AudioRoute
import com.chooloo.www.koler.domain.model.Call
import com.chooloo.www.koler.domain.model.CallAudio
import com.chooloo.www.koler.domain.repository.CallRepository
import java.util.IdentityHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CallRepositoryImpl @Inject constructor(private val callMapper: CallMapper) :
    CallRepository {
    private var service: InCallService? = null
    private var endpoints = emptyList<CallEndpoint>()
    private var endpointReceived = false
    private var nextCallId = 0
    private val telecomCalls = LinkedHashMap<String, TelecomCall>()
    private val callIds = IdentityHashMap<TelecomCall, String>()
    private val callCallback = object : TelecomCall.Callback() {
        override fun onStateChanged(call: TelecomCall, state: Int) = publishCalls()
        override fun onParentChanged(call: TelecomCall, parent: TelecomCall?) = publishCalls()
        override fun onChildrenChanged(call: TelecomCall, children: List<TelecomCall>) =
            publishCalls()
        override fun onDetailsChanged(call: TelecomCall, details: TelecomCall.Details) =
            publishCalls()
        override fun onConferenceableCallsChanged(call: TelecomCall, calls: List<TelecomCall>) =
            publishCalls()
    }

    private val _calls = MutableStateFlow<List<Call>>(emptyList())
    private val _audio = MutableStateFlow(CallAudio())
    private val _canAddCall = MutableStateFlow(false)
    private val _hasPendingOutgoing = MutableStateFlow(false)

    override val calls: StateFlow<List<Call>> = _calls.asStateFlow()
    override val audio: StateFlow<CallAudio> = _audio.asStateFlow()
    override val canAddCall: StateFlow<Boolean> = _canAddCall.asStateFlow()
    override val hasPendingOutgoing: StateFlow<Boolean> = _hasPendingOutgoing.asStateFlow()

    override fun answer(callId: String) = withCall(callId) {
        answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
    }

    override fun decline(callId: String) = withCall(callId) {
        when {
            callMapper.telecomState(this) != TelecomCall.STATE_RINGING -> disconnect()

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> reject(
                TelecomCall.REJECT_REASON_DECLINED,
            )

            else -> reject(false, null)
        }
    }

    override fun hold(callId: String) = withCall(callId) { hold() }

    override fun unhold(callId: String) = withCall(callId) { unhold() }

    override fun swap(callId: String) = withCall(callId) { swapConference() }

    override fun merge(callId: String) = withCall(callId) {
        conferenceableCalls.firstOrNull()?.conference(this) ?: mergeConference()
    }

    override fun separate(callId: String) = withCall(callId) { splitFromConference() }

    override fun playDtmf(callId: String, key: Char) = withCall(callId) {
        playDtmfTone(key)
        stopDtmfTone()
    }

    override fun selectPhoneAccount(callId: String, accountId: String) = withCall(callId) {
        val handle = callMapper.availablePhoneAccounts(this)
            .firstOrNull { callMapper.accountId(it) == accountId }
        if (handle == null) {
            Log.e(TAG, "Phone account $accountId is not available for call $callId")
            return@withCall
        }
        phoneAccountSelected(handle, false)
    }

    override fun setMuted(isMuted: Boolean) = withService { setMuted(isMuted) }

    override fun setAudioRoute(route: AudioRoute) = withService {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val endpoint = if (route == AudioRoute.WIRED_OR_EARPIECE) {
                endpoints.firstOrNull { endpointRoute(it.endpointType) == AudioRoute.WIRED_HEADSET }
                    ?: endpoints.firstOrNull {
                        endpointRoute(it.endpointType) == AudioRoute.EARPIECE
                    }
            } else {
                endpoints.firstOrNull { endpointRoute(it.endpointType) == route }
            }
            if (endpoint == null) {
                Toast.makeText(
                    this,
                    R.string.call_audio_route_unavailable,
                    Toast.LENGTH_SHORT,
                ).show()
                return@withService
            }
            requestCallEndpointChange(
                endpoint,
                mainExecutor,
                object : OutcomeReceiver<Void, CallEndpointException> {
                    override fun onResult(result: Void?) = Unit
                    override fun onError(error: CallEndpointException) {
                        Toast.makeText(
                            this@withService,
                            R.string.call_audio_route_unavailable,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )
        } else {
            setAudioRoute(callMapper.toTelecomRoute(route))
        }
    }

    fun onServiceCreated(service: InCallService) {
        this.service = service
    }

    fun onServiceDestroyed(service: InCallService) {
        if (this.service !== service) return
        this.service = null
        endpoints = emptyList()
        endpointReceived = false
        callIds.keys.forEach { it.unregisterCallback(callCallback) }
        telecomCalls.clear()
        callIds.clear()
        _calls.value = emptyList()
        _audio.value = CallAudio()
        _canAddCall.value = false
        _hasPendingOutgoing.value = false
    }

    fun onCallAdded(call: TelecomCall): Call {
        val id = "call-${nextCallId++}"
        callIds[call] = id
        telecomCalls[id] = call
        call.registerCallback(callCallback)
        _hasPendingOutgoing.value = false
        publishCalls()
        return _calls.value.first { it.id == id }
    }

    fun onCallRemoved(call: TelecomCall) {
        call.unregisterCallback(callCallback)
        callIds.remove(call)?.let(telecomCalls::remove)
        publishCalls()
    }

    fun onPendingOutgoing() {
        _hasPendingOutgoing.value = true
    }

    fun onCanAddCallChanged(canAddCall: Boolean) {
        _canAddCall.value = canAddCall
    }

    fun onAudioStateChanged(audioState: CallAudioState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            _audio.value = callMapper.toCallAudio(audioState)
        } else {
            _audio.value = _audio.value.copy(
                isMuted = audioState.isMuted,
                route = if (endpointReceived) {
                    _audio.value.route
                } else {
                    callMapper.toCallAudio(
                        audioState,
                    ).route
                },
            )
        }
    }

    @RequiresApi(34)
    fun onEndpointChanged(endpoint: CallEndpoint) {
        endpointReceived = true
        _audio.value = _audio.value.copy(route = endpointRoute(endpoint.endpointType))
    }

    @RequiresApi(34)
    fun onEndpointsChanged(available: List<CallEndpoint>) {
        endpoints = available.toList()
        _audio.value = _audio.value.copy(
            supportedRoutes = available.mapNotNull { endpointRoute(it.endpointType) }.distinct(),
        )
    }

    fun onMuteStateChanged(isMuted: Boolean) {
        _audio.value = _audio.value.copy(isMuted = isMuted)
    }

    private fun publishCalls() {
        _calls.value = telecomCalls.map { (id, call) -> callMapper.toCall(call, id) }
    }

    private inline fun withService(action: InCallService.() -> Unit) {
        val service = service
        if (service == null) {
            Log.e(TAG, "Ignoring action, no in-call service is bound")
            return
        }
        service.action()
    }

    private inline fun withCall(callId: String, action: TelecomCall.() -> Unit) {
        val call = telecomCalls[callId]
        if (call == null) {
            Log.w(TAG, "Ignoring action on removed call $callId")
            return
        }
        call.action()
    }

    private companion object {
        const val TAG = "CallRepository"
    }
}

internal fun endpointRoute(type: Int): AudioRoute? = when (type) {
    CallEndpoint.TYPE_EARPIECE -> AudioRoute.EARPIECE
    CallEndpoint.TYPE_SPEAKER -> AudioRoute.SPEAKER
    CallEndpoint.TYPE_BLUETOOTH -> AudioRoute.BLUETOOTH
    CallEndpoint.TYPE_WIRED_HEADSET -> AudioRoute.WIRED_HEADSET
    else -> null
}
