package com.callguard.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import com.callguard.app.CallGuardApp
import com.callguard.app.data.local.CallLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallGuardScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart
        val presentation = callDetails.handlePresentation

        val isHidden = presentation == TelecomManager.PRESENTATION_RESTRICTED ||
                presentation == TelecomManager.PRESENTATION_UNKNOWN ||
                rawNumber.isNullOrBlank()

        val app = application as CallGuardApp
        val engine = app.evaluationEngine
        val dao = app.database.callGuardDao()
        val settingsRepo = app.settingsRepository

        serviceScope.launch {
            val currentMode = settingsRepo.getCurrentSettings().activeMode
            val result = engine.evaluateIncomingCall(rawNumber, isHidden)

            val responseBuilder = CallResponse.Builder()

            if (result.shouldBlock) {
                responseBuilder
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false)
            } else {
                responseBuilder
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipNotification(false)
            }

            respondToCall(callDetails, responseBuilder.build())

            dao.insertCallLog(
                CallLogEntity(
                    phoneNumber = if (isHidden) "Numéro Masqué" else (rawNumber ?: "Inconnu"),
                    contactName = result.resolvedContactName,
                    wasBlocked = result.shouldBlock,
                    reason = result.reason,
                    activeMode = currentMode
                )
            )
        }
    }
}
