package com.example.velibapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.velibapp.model.FavoriteStation

@Database(
    entities = [FavoriteStation::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
}