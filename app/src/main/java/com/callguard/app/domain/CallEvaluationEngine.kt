package com.callguard.app.domain

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import com.callguard.app.data.local.CallGuardDao
import com.callguard.app.data.local.FilterMode
import com.callguard.app.data.local.ListType
import com.callguard.app.data.preferences.SettingsRepository

data class EvaluationResult(
    val shouldBlock: Boolean,
    val reason: String,
    val resolvedContactName: String?
)

class CallEvaluationEngine(
    private val context: Context,
    private val dao: CallGuardDao,
    private val settingsRepo: SettingsRepository
) {

    suspend fun evaluateIncomingCall(
        rawNumber: String?,
        isHiddenOrPrivate: Boolean
    ): EvaluationResult {
        val settings = settingsRepo.getCurrentSettings()

        if (settings.activeMode == FilterMode.DISABLED) {
            val name = rawNumber?.let { resolveContactInfo(it).first }
            return EvaluationResult(false, "Filtrage désactivé", name)
        }

        if (isHiddenOrPrivate || rawNumber.isNullOrBlank()) {
            return if (settings.blockHiddenNumbers) {
                EvaluationResult(true, "Numéro masqué bloqué", "Numéro Masqué")
            } else {
                EvaluationResult(false, "Numéro masqué autorisé", "Numéro Masqué")
            }
        }

        val (contactName, isContact, isFavorite) = resolveContactInfo(rawNumber)

        if (settings.allowRepeatedCalls) {
            val windowMillis = settings.repeatedCallWindowMinutes * 60 * 1000L
            val recentCount = dao.countRecentCallsFromNumber(
                rawNumber,
                System.currentTimeMillis() - windowMillis
            )
            if (recentCount >= 1) {
                return EvaluationResult(false, "Exception : Appel répété", contactName)
            }
        }

        return when (settings.activeMode) {
            FilterMode.BLACKLIST -> {
                val blacklist = dao.getRulesByTypeSync(ListType.BLACKLIST)
                val isBlacklisted = blacklist.any { numbersMatch(it.phoneNumber, rawNumber) }
                if (isBlacklisted) {
                    EvaluationResult(true, "Présent dans la liste noire", contactName)
                } else {
                    EvaluationResult(false, "Autorisé (Hors liste noire)", contactName)
                }
            }

            FilterMode.WHITELIST -> {
                if (settings.allowFavoriteContacts && isFavorite) {
                    return EvaluationResult(false, "Exception : Contact favori", contactName)
                }
                if (settings.allowAllContactsInWhitelist && isContact) {
                    return EvaluationResult(false, "Exception : Répertoire", contactName)
                }

                val whitelist = dao.getRulesByTypeSync(ListType.WHITELIST)
                val isWhitelisted = whitelist.any { numbersMatch(it.phoneNumber, rawNumber) }
                if (isWhitelisted) {
                    EvaluationResult(false, "Présent dans la liste blanche", contactName)
                } else {
                    EvaluationResult(true, "Bloqué (Abs. liste blanche)", contactName)
                }
            }

            FilterMode.DISABLED -> EvaluationResult(false, "Désactivé", contactName)
        }
    }

    @Suppress("DEPRECATION")
    private fun numbersMatch(num1: String, num2: String): Boolean {
        return PhoneNumberUtils.compare(context, num1, num2)
    }

    private fun resolveContactInfo(phoneNumber: String): Triple<String?, Boolean, Boolean> {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.STARRED
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val starIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val isStarred = if (starIdx >= 0) cursor.getInt(starIdx) == 1 else false
                    return Triple(name, true, isStarred)
                }
            }
            Triple(null, false, false)
        } catch (e: SecurityException) {
            Triple(null, false, false)
        }
    }
}
