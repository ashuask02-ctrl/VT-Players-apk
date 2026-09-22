package com.example.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages phone call state monitoring (incoming calls, dialing outgoing calls, and active calls).
 * Triggers pause when a call begins or rings, and resumes playback when the call ends.
 */
class VTCallManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onCallStarted: () -> Unit,
    private val onCallEnded: () -> Unit
) {
    private val tag = "VTCallManager"
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var telephonyCallback: Any? = null
    private var legacyListener: PhoneStateListener? = null
    private var receiver: BroadcastReceiver? = null

    @Volatile
    var isInCall: Boolean = false
        private set

    var isEnabled: Boolean = true

    init {
        registerListeners()
    }

    fun registerListeners() {
        registerTelephonyStateListener()
        registerBroadcastReceiver()
    }

    fun checkAndRecheckPermission() {
        if (telephonyCallback == null && legacyListener == null) {
            registerTelephonyStateListener()
        }
    }

    private fun registerTelephonyStateListener() {
        val tm = telephonyManager ?: return
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.d(tag, "READ_PHONE_STATE not granted; falling back to BroadcastReceiver and AudioFocus")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        Log.d(tag, "TelephonyCallback state changed: $state")
                        handleCallState(state)
                    }
                }
                tm.registerTelephonyCallback(context.mainExecutor, callback)
                telephonyCallback = callback
                Log.d(tag, "Registered TelephonyCallback for Android 12+")
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        Log.d(tag, "PhoneStateListener state changed: $state")
                        handleCallState(state)
                    }
                }
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyListener = listener
                Log.d(tag, "Registered legacy PhoneStateListener")
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not register telephony listener: ${e.message}")
        }
    }

    private fun registerBroadcastReceiver() {
        if (receiver != null) return
        val br = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                if (TelephonyManager.ACTION_PHONE_STATE_CHANGED == intent.action) {
                    val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    Log.d(tag, "Received PHONE_STATE broadcast: $stateStr")
                    when (stateStr) {
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            handleCallState(TelephonyManager.CALL_STATE_RINGING)
                        }
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            handleCallState(TelephonyManager.CALL_STATE_OFFHOOK)
                        }
                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            handleCallState(TelephonyManager.CALL_STATE_IDLE)
                        }
                    }
                }
            }
        }
        try {
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            ContextCompat.registerReceiver(
                context,
                br,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            receiver = br
            Log.d(tag, "Registered PHONE_STATE BroadcastReceiver")
        } catch (e: Exception) {
            Log.w(tag, "Could not register PHONE_STATE receiver: ${e.message}")
        }
    }

    fun handleCallState(state: Int) {
        if (!isEnabled) return
        coroutineScope.launch(Dispatchers.Main) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    // Receiving an incoming call
                    Log.d(tag, "Call State: RINGING (incoming call alert)")
                    isInCall = true
                    onCallStarted()
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Outgoing call placed, answered, or currently active
                    Log.d(tag, "Call State: OFFHOOK (dialing or active call)")
                    isInCall = true
                    onCallStarted()
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Call ended, missed, or rejected
                    Log.d(tag, "Call State: IDLE (call finished)")
                    if (isInCall) {
                        isInCall = false
                        onCallEnded()
                    }
                }
            }
        }
    }

    /**
     * Checks if a phone call is active or ringing via TelephonyManager and AudioManager.
     */
    fun isPhoneCallActive(): Boolean {
        val tmState = try {
            telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE
        } catch (_: Exception) {
            TelephonyManager.CALL_STATE_IDLE
        }
        if (tmState != TelephonyManager.CALL_STATE_IDLE) return true

        val mode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        return mode == AudioManager.MODE_RINGTONE ||
                mode == AudioManager.MODE_IN_CALL ||
                mode == AudioManager.MODE_IN_COMMUNICATION ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mode == AudioManager.MODE_CALL_SCREENING)
    }

    fun release() {
        try {
            receiver?.let {
                context.unregisterReceiver(it)
                receiver = null
            }
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let {
                    telephonyManager?.unregisterTelephonyCallback(it)
                    telephonyCallback = null
                }
            } else {
                legacyListener?.let {
                    @Suppress("DEPRECATION")
                    telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                    legacyListener = null
                }
            }
        } catch (_: Exception) {}
    }
}
