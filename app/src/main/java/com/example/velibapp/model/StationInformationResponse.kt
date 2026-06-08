package com.example.velibapp.model

import com.google.gson.annotations.SerializedName

data class StationInformationResponse(
    val data: StationData
)
data class StationData(
    val stations: List<VelibStation>
)

data class VelibStation(
    @SerializedName("station_id")
    val stationId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int
)