package com.lyulf.mind.data.remote

import com.squareup.moshi.Json

data class PairCreateBody(
    val partnerPronoun: String = "ta",
)

data class PairJoinBody(
    val code: String,
    val partnerPronoun: String = "ta",
)

data class PairRecoverBody(
    val recoveryCode: String,
    val side: String,
    val partnerPronoun: String? = null,
)

data class MeSettingsBody(
    val partnerPronoun: String? = null,
)

data class UpdateModuleBody(
    val dailyTarget: Int,
)

data class PairSessionResponse(
    val token: String? = null,
    val user: UserDto,
    val relationshipId: Long,
    val paired: Boolean,
    val waitingForPartner: Boolean = false,
    val inviteCode: String? = null,
    val recoveryCode: String? = null,
    val partner: UserDto? = null,
)

data class UserDto(
    val id: Long,
    val side: String,
    val partnerPronoun: String,
    val partnerPronounLabel: String? = null,
    val createdAt: String? = null,
)

data class CreateReminderBody(
    val type: String,
    val content: String? = null,
    val title: String? = null,
    val clientNonce: String? = null,
)

data class RespondBody(
    val amountMl: Int? = null,
    val count: Int? = null,
    val reply: String? = null,
    val hasImage: Boolean? = null,
    val imageUrl: String? = null,
)

data class UploadResponse(
    val url: String,
)

data class ReminderListResponse(
    val items: List<ReminderDto>,
    val nextCursor: String? = null,
)

data class TodayResponse(
    val fromPartner: List<ReminderDto> = emptyList(),
    val toPartner: List<ReminderDto> = emptyList(),
    val careCount: Int = 0,
    val pokeCount: Int = 0,
)

data class ReminderDto(
    val id: String,
    val relationshipId: Long,
    val senderId: Long,
    val receiverId: Long,
    val type: String,
    val dir: String,
    val title: String? = null,
    val content: String? = null,
    val state: String,
    val read: Boolean,
    val silenced: Boolean,
    val reply: String? = null,
    val hasImage: Boolean = false,
    val imageUrl: String? = null,
    val amountMl: Int? = null,
    val count: Int? = null,
    val createdAt: String,
    val respondedAt: String? = null,
    val timeLabel: String,
    val isToday: Boolean,
    val listStatus: String? = null,
    val detailStatus: String? = null,
    val foldCorner: Boolean = false,
)

data class ErrorBody(
    @Json(name = "error") val error: String? = null,
)

data class OkResponse(val ok: Boolean = true)

data class DailyLogTime(
    val at: String,
    val value: Int,
    val unit: String? = null,
    val index: Int? = null,
)

data class HabitModuleDto(
    val id: Long,
    val moduleType: String,
    val title: String,
    val dailyTarget: Int,
    val sortOrder: Int = 0,
    val enabled: Boolean = true,
    val todayValue: Int = 0,
    val todayTimes: List<DailyLogTime> = emptyList(),
)

data class DailyOverviewResponse(
    val date: String,
    val modules: List<HabitModuleDto>,
)

data class DrinkLogBody(val amountMl: Int)

data class ActivityLogBody(
    val activityType: String? = null,
    val moduleId: Long? = null,
    val count: Int = 1,
)

data class AddModuleBody(
    val title: String,
    val dailyTarget: Int = 3,
)

data class TrendPoint(val date: String, val value: Int)

data class TrendExtreme(val date: String?, val value: Int)

data class TrendResponse(
    val module: HabitModuleDto,
    val days: Int,
    val series: List<TrendPoint>,
    val average: Double,
    val hitDays: Int,
    val max: TrendExtreme,
    val min: TrendExtreme,
    val todayEntries: List<DailyLogTime>,
    val todayTotal: Int,
    val unit: String,
)
