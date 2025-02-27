package com.machadothi.templateapp.di

import android.os.Build
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.machadothi.templateapp.BuildConfig
import com.machadothi.templateapp.data.network.DataService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun providesRetrofit(
        json: Json
    ): Retrofit {
        print(BuildConfig.SERVER_URL)
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_URL)
            .client(
                OkHttpClient.Builder()
                    .callTimeout(5L, TimeUnit.SECONDS)
                    .addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY),
                    ).build(),
            )
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }


    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        return logging
    }

    @Provides
    fun providesService(retrofit: Retrofit): DataService {
        return retrofit.create(DataService::class.java)
    }

    @Provides
    fun providesJson() = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
}