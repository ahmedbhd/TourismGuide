package com.mobelite.tourismguide.data.roomservice.local

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.mobelite.tourismguide.data.roomservice.local.RestaurantDataBase.Companion.DATABASE_VERSION
import com.mobelite.tourismguide.data.roomservice.model.Restaurant

@Database(entities = [(Restaurant::class)], version = DATABASE_VERSION)
abstract class RestaurantDataBase : RoomDatabase() {
    abstract fun restaurantDAO(): RestaurantDAO

    companion object {
        const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "Database-Room"
        private var mInstance: RestaurantDataBase? = null
        fun getInstance(context: Context): RestaurantDataBase {
            if (mInstance==null)
                mInstance = Room.databaseBuilder(context, RestaurantDataBase::class.java, DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                        .build()
            return mInstance!!
        }
    }
}