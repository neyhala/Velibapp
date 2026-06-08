package com.example.velibapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteStation(

    @PrimaryKey
    val stationId: String,

    val name: String,

    val lat: Double,

    val lon: Double,

    val capacity: Int,

    val bikesAvailable: Int,

    val docksAvailable: Int
)