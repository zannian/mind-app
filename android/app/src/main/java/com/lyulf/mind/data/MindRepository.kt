package com.lyulf.mind.data

import com.lyulf.mind.data.remote.ActivityLogBody
import com.lyulf.mind.data.remote.AddModuleBody
import com.lyulf.mind.data.remote.CreateReminderBody
import com.lyulf.mind.data.remote.DrinkLogBody
import com.lyulf.mind.data.remote.MeSettingsBody
import com.lyulf.mind.data.remote.MindApi
import com.lyulf.mind.data.remote.PairCreateBody
import com.lyulf.mind.data.remote.PairJoinBody
import com.lyulf.mind.data.remote.PairRecoverBody
import com.lyulf.mind.data.remote.PairSessionResponse
import com.lyulf.mind.data.remote.RespondBody
import com.lyulf.mind.data.remote.TodayResponse
import com.lyulf.mind.data.remote.TrendResponse
import com.lyulf.mind.data.remote.UpdateModuleBody
import com.lyulf.mind.data.remote.UploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.util.UUID

class MindRepository(
    private val api: MindApi,
    private val sessionStore: SessionStore,
    private val seenReminders: SeenReminderStore? = null,
) {
    suspend fun createPair(partnerPronoun: String): PairSessionResponse {
        val res = api.createPair(PairCreateBody(partnerPronoun))
        persist(res, keepRecoveryIfNull = false)
        return res
    }

    suspend fun joinPair(code: String, partnerPronoun: String): PairSessionResponse {
        val res = api.joinPair(PairJoinBody(code.trim().uppercase(), partnerPronoun))
        persist(res, keepRecoveryIfNull = false)
        return res
    }

    suspend fun recoverPair(recoveryCode: String, side: String, partnerPronoun: String?): PairSessionResponse {
        val res = api.recoverPair(PairRecoverBody(recoveryCode, side, partnerPronoun))
        persist(res, keepRecoveryIfNull = true)
        return res
    }

    suspend fun refreshMe(bearer: String): Boolean {
        val me = api.me(bearer)
        val token = bearer.removePrefix("Bearer ").trim()
        sessionStore.saveSession(
            token = token,
            userId = me.user.id,
            side = me.user.side,
            partnerPronoun = me.user.partnerPronoun,
            relationshipId = me.relationshipId,
            paired = me.paired,
            inviteCode = me.inviteCode,
            recoveryCode = null,
            keepRecoveryIfNull = true,
        )
        return me.paired
    }

    suspend fun unbind(bearer: String) {
        api.unbind(bearer)
        sessionStore.clear()
        seenReminders?.clear()
    }

    /** 前台已读过列表：后台轮询不再对这些弹通知 */
    suspend fun markIncomingSeen(ids: Collection<String>) {
        seenReminders?.markSeen(ids.filter { it.isNotBlank() }, prime = true)
    }

    suspend fun updatePronoun(bearer: String, partnerPronoun: String): PairSessionResponse {
        val res = api.updateSettings(bearer, MeSettingsBody(partnerPronoun = partnerPronoun))
        val token = bearer.removePrefix("Bearer ").trim()
        sessionStore.saveSession(
            token = token,
            userId = res.user.id,
            side = res.user.side,
            partnerPronoun = res.user.partnerPronoun,
            relationshipId = res.relationshipId,
            paired = res.paired,
            inviteCode = res.inviteCode,
            recoveryCode = null,
            keepRecoveryIfNull = true,
        )
        return res
    }

    suspend fun updateModuleTarget(bearer: String, moduleId: Long, dailyTarget: Int) =
        api.updateModule(bearer, moduleId, UpdateModuleBody(dailyTarget))

    private suspend fun persist(res: PairSessionResponse, keepRecoveryIfNull: Boolean) {
        val token = res.token ?: error("服务器未返回设备令牌")
        sessionStore.saveSession(
            token = token,
            userId = res.user.id,
            side = res.user.side,
            partnerPronoun = res.user.partnerPronoun,
            relationshipId = res.relationshipId,
            paired = res.paired,
            inviteCode = res.inviteCode,
            recoveryCode = res.recoveryCode,
            keepRecoveryIfNull = keepRecoveryIfNull,
        )
    }

    suspend fun listReminders(bearer: String, limit: Int = 50) =
        api.listReminders(bearer, limit = limit).items

    suspend fun today(bearer: String): TodayResponse = api.today(bearer)

    suspend fun sendReminder(
        bearer: String,
        type: String,
        content: String? = null,
        title: String? = null,
    ) = api.createReminder(
        bearer,
        CreateReminderBody(
            type = type,
            content = content?.ifBlank { null },
            title = title?.ifBlank { null },
            clientNonce = UUID.randomUUID().toString(),
        ),
    )

    suspend fun poke(bearer: String) = sendReminder(bearer, "poke")

    suspend fun uploadImage(
        bearer: String,
        bytes: ByteArray,
        mime: String = "image/jpeg",
        filename: String = "photo.jpg",
    ): UploadResponse {
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", filename, body)
        return api.upload(bearer, part)
    }

    suspend fun respond(
        bearer: String,
        id: String,
        amountMl: Int? = null,
        count: Int? = null,
        reply: String? = null,
        imageUrl: String? = null,
    ) = api.respond(
        bearer,
        id,
        RespondBody(
            amountMl = amountMl,
            count = count,
            reply = reply,
            hasImage = imageUrl != null,
            imageUrl = imageUrl,
        ),
    )

    suspend fun ack(bearer: String, id: String) = api.ack(bearer, id)

    suspend fun markRead(bearer: String, id: String) = api.markRead(bearer, id)

    suspend fun clearLocal() = sessionStore.clear()

    suspend fun daily(bearer: String) = api.daily(bearer)

    suspend fun logDrink(bearer: String, ml: Int) =
        api.logDrink(bearer, DrinkLogBody(ml))

    suspend fun logActivity(bearer: String, moduleId: Long, activityType: String? = null) =
        api.logActivity(
            bearer,
            ActivityLogBody(activityType = activityType, moduleId = moduleId, count = 1),
        )

    suspend fun addModule(bearer: String, title: String, dailyTarget: Int) =
        api.addModule(bearer, AddModuleBody(title, dailyTarget))

    suspend fun deleteModule(bearer: String, id: Long) = api.deleteModule(bearer, id)

    suspend fun trends(bearer: String, moduleId: Long, days: Int = 7): TrendResponse =
        api.trends(bearer, moduleId, days)
}

fun Throwable.apiMessage(): String {
    if (this is HttpException) {
        return try {
            val raw = response()?.errorBody()?.string()
            Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw.orEmpty())?.groupValues?.get(1)
                ?: message()
                ?: "请求失败"
        } catch (_: Exception) {
            message() ?: "请求失败"
        }
    }
    return message ?: "网络错误"
}
