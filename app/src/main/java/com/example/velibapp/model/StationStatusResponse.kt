package com.example.velibapp.model

import com.google.gson.annotations.SerializedName

data class StationStatusResponse(
    val data: StationStatusData
)

data class StationStatusData(
    val stations: List<StationStatus>
)

data class StationStatus(
    @SerializedName("station_id")
    val stationId: String,

    @SerializedName("num_bikes_available")
    val bikesAvailable: Int,

    @SerializedName("num_docks_available")
    val docksAvailable: Int
)