package com.zaaam.zreming.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class MaintenanceDto(
    val maintenance: Boolean = false,
)

interface MaintenanceApi {
    @GET("maintenance.json")
    suspend fun getMaintenance(): MaintenanceDto
}