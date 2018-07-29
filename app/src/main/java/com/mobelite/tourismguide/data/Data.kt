package com.mobelite.tourismguide.data

object Model {
    data class ResultRestaurant(val id: Int , val name: String, val phone : String , val description : String , val lat : String , val lng : String
                                    , val image : String , val userid: String)

    data class FavRestaurant(val id: Int , val rest: Int,val user: String)
}