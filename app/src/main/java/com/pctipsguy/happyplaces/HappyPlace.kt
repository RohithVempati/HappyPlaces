package com.pctipsguy.happyplaces

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "HappyPlacesTable")
data class HappyPlace(
    @PrimaryKey(autoGenerate=true)
    var id:Int=0,
    val title:String,
    val description:String,
    val date:String,
    val location:String,
    val imgUri: String,
    val longitude: Double,
    val latitude:Double
):Serializable