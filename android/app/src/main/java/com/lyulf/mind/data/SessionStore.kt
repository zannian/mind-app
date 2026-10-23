package com.lyulf.mind.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("mind_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val userIdKey = longPreferencesKey("user_id")
    private val sideKey = stringPreferencesKey("side")
    private val pronounKey = stringPreferencesKey("partner_pronoun")
    private val relKey = longPreferencesKey("relationship_id")
    private val pairedKey = booleanPreferencesKey("paired")
    private val inviteKey = stringPreferencesKey("invite_code")
    private val recoveryKey = stringPreferencesKey("recovery_code")

    data class Session(
        val token: String?,
        val userId: Long?,
        val side: String?,
        val partnerPronoun: String?,
        val relationshipId: Long?,
        val paired: Boolean,
        val inviteCode: String?,
        val recoveryCode: String?,
    ) {
        val hasDevice get() = !token.isNullOrBlank()
        val partnerPronounLabel: String
            get() = when (partnerPronoun) {
                "she" -> "她"
                "he" -> "他"
                else -> "TA"
            }

        fun bearer() = "Bearer $token"
    }

    val session: Flow<Session> = context.dataStore.data.map { p ->
        Session(
            token = p[tokenKey],
            userId = p[userIdKey],
            side = p[sideKey],
            partnerPronoun = p[pronounKey],
            relationshipId = p[relKey],
            paired = p[pairedKey] == true,
            inviteCode = p[inviteKey],
            recoveryCode = p[recoveryKey],
        )
    }

    suspend fun saveSession(
        token: String,
        userId: Long,
        side: String,
        partnerPronoun: String,
        relationshipId: Long,
        paired: Boolean,
        inviteCode: String? = null,
        recoveryCode: String? = null,
        keepRecoveryIfNull: Boolean = true,
    ) {
        context.dataStore.edit { p ->
            p[tokenKey] = token
            p[userIdKey] = userId
            p[sideKey] = side
            p[pronounKey] = partnerPronoun
            p[relKey] = relationshipId
            p[pairedKey] = paired
            if (inviteCode != null) p[inviteKey] = inviteCode
            else if (paired) p.remove(inviteKey)
            if (recoveryCode != null) p[recoveryKey] = recoveryCode
            else if (!keepRecoveryIfNull) p.remove(recoveryKey)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
