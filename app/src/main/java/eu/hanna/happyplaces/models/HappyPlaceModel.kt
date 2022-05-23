package eu.hanna.happyplaces.models

import java.io.Serializable

data class HappyPlaceModel(
    val id:Int,
    val title:String,
    val image: String,
    val description: String,
    val date: String,
    val location: String,
    val latitude: Double,
    val longitutde: Double
) : Serializable

// It cannot sent the data class directly through intent so make it serializable