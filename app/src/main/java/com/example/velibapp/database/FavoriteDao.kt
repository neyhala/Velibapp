package com.example.velibapp.database

import androidx.room.*
import com.example.velibapp.model.FavoriteStation

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteStation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(station: FavoriteStation)

    @Delete
    suspend fun deleteFavorite(station: FavoriteStation)

    @Query("DELETE FROM favorites WHERE stationId = :id")
    suspend fun deleteById(id: String)
}