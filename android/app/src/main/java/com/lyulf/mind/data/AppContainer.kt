package com.lyulf.mind.data

import android.content.Context
import com.lyulf.mind.BuildConfig
import com.lyulf.mind.data.remote.MindApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val sessionStore = SessionStore(context.applicationContext)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // WS 长连接
        .pingInterval(20, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(
            // HTTP 仍用有限读超时
            client.newBuilder().readTimeout(20, TimeUnit.SECONDS).build(),
        )
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val seenReminders = SeenReminderStore(context.applicationContext)
    val api: MindApi = retrofit.create(MindApi::class.java)
    val repository = MindRepository(api, sessionStore, seenReminders)
    val realtime = MindRealtime(client)
}
