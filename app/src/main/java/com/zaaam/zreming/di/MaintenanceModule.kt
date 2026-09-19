package com.zaaam.zreming.di

import com.zaaam.zreming.data.remote.MaintenanceApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MaintenanceModule {

    @Provides
    @Singleton
    fun provideMaintenanceApi(
        client: OkHttpClient,
        json: Json,
    ): MaintenanceApi {
        return Retrofit.Builder()
            .baseUrl("https://zarstream-6d343-default-rtdb.firebaseio.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MaintenanceApi::class.java)
    }
}