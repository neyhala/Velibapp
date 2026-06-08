package com.example.velibapp.model

data class StationDetail(
    val stationId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int,
    val bikesAvailable: Int,
    val docksAvailable: Int
)