package com.lyulf.mind.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MindApi {
    @POST("pair/create")
    suspend fun createPair(@Body body: PairCreateBody): PairSessionResponse

    @POST("pair/join")
    suspend fun joinPair(@Body body: PairJoinBody): PairSessionResponse

    @POST("pair/recover")
    suspend fun recoverPair(@Body body: PairRecoverBody): PairSessionResponse

    @POST("pair/unbind")
    suspend fun unbind(@Header("Authorization") bearer: String): OkResponse

    @GET("me")
    suspend fun me(@Header("Authorization") bearer: String): PairSessionResponse

    @PATCH("me/settings")
    suspend fun updateSettings(
        @Header("Authorization") bearer: String,
        @Body body: MeSettingsBody,
    ): PairSessionResponse

    @GET("reminders")
    suspend fun listReminders(
        @Header("Authorization") bearer: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): ReminderListResponse

    @GET("reminders/today")
    suspend fun today(@Header("Authorization") bearer: String): TodayResponse

    @POST("reminders")
    suspend fun createReminder(
        @Header("Authorization") bearer: String,
        @Body body: CreateReminderBody,
    ): ReminderDto

    @Multipart
    @POST("uploads")
    suspend fun upload(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part,
    ): UploadResponse

    @POST("reminders/{id}/respond")
    suspend fun respond(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: RespondBody,
    ): ReminderDto

    @POST("reminders/{id}/ack")
    suspend fun ack(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
    ): ReminderDto

    @POST("reminders/{id}/read")
    suspend fun markRead(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
    ): ReminderDto

    @GET("daily")
    suspend fun daily(@Header("Authorization") bearer: String): DailyOverviewResponse

    @POST("logs/drink")
    suspend fun logDrink(
        @Header("Authorization") bearer: String,
        @Body body: DrinkLogBody,
    ): OkResponse

    @POST("logs/activity")
    suspend fun logActivity(
        @Header("Authorization") bearer: String,
        @Body body: ActivityLogBody,
    ): OkResponse

    @POST("modules")
    suspend fun addModule(
        @Header("Authorization") bearer: String,
        @Body body: AddModuleBody,
    ): HabitModuleDto

    @DELETE("modules/{id}")
    suspend fun deleteModule(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long,
    ): OkResponse

    @PATCH("modules/{id}")
    suspend fun updateModule(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long,
        @Body body: UpdateModuleBody,
    ): HabitModuleDto

    @GET("trends")
    suspend fun trends(
        @Header("Authorization") bearer: String,
        @Query("module") moduleId: Long,
        @Query("days") days: Int = 7,
    ): TrendResponse
}
