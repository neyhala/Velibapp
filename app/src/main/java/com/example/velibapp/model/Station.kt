package com.example.velibapp.model

data class Station(
    val stationId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int
)