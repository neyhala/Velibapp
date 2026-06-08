package com.example.velibapp.api

import com.example.velibapp.model.StationInformationResponse
import retrofit2.http.GET
import com.example.velibapp.model.StationStatusResponse

interface VelibApiService {
    @GET("station_information.json")
    suspend fun getStationInformation(): StationInformationResponse

    @GET("station_status.json")
    suspend fun getStationStatus(): StationStatusResponse
}