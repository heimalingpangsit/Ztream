package com.zaaam.zreming.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET

interface MaintenanceApi {
    @GET("maintenance.json")
    suspend fun getMaintenance(): JsonElement
}