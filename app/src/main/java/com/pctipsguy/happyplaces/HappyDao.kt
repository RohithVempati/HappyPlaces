package com.pctipsguy.happyplaces

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HappyDao {

    @Insert
    suspend fun insert(happyPlace: HappyPlace)

    @Update
    suspend fun update(happyPlace: HappyPlace)

    @Delete
    fun delete(happyPlace: HappyPlace)

    @Query("SELECT * FROM HAPPYPLACESTABLE")
    fun fetchAll(): Flow<List<HappyPlace>>

    @Query("SELECT * FROM HAPPYPLACESTABLE WHERE id=:id")
    fun fetchById(id:Int):HappyPlace

    @Query("SELECT imgUri FROM HAPPYPLACESTABLE")
    fun fetchAllUri():List<String>
}