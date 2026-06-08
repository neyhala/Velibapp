package com.example.velibapp.repository

import com.example.velibapp.api.RetrofitInstance
import com.example.velibapp.model.StationDetail

class VelibRepository {

    suspend fun getStations(): List<StationDetail> {

        val information =
            RetrofitInstance.api.getStationInformation()

        val status =
            RetrofitInstance.api.getStationStatus()

        val statusMap =
            status.data.stations.associateBy { it.stationId }

        return information.data.stations.mapNotNull { station ->

            val stationStatus =
                statusMap[station.stationId]

            if (stationStatus != null) {

                StationDetail(
                    stationId = station.stationId,
                    name = station.name,
                    lat = station.lat,
                    lon = station.lon,
                    capacity = station.capacity,
                    bikesAvailable = stationStatus.bikesAvailable,
                    docksAvailable = stationStatus.docksAvailable
                )

            } else {
                null
            }
        }
    }
}