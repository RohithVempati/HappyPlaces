package com.pctipsguy.happyplaces

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(version = 1,entities = [HappyPlace::class])
abstract class HappyDatabase:RoomDatabase() {
    abstract fun getPlacesDao() : HappyDao

    companion object{
        @Volatile
        var INSTANCE:HappyDatabase? = null
        fun getInstance(context: Context):HappyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext,
                    HappyDatabase::class.java,"HpyDB")
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
