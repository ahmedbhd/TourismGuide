package com.mobelite.tourismguide.data.webservice

import java.util.*

object Model {
    data class ResultRestaurant(val id: Int, val name: String, val phone : String, val description : String, val lat : String, val lng : String
                                , var image : String, val userid: String)

    data class FavRestaurant(val id: Int , val rest: Int,val user: String)


    data class User(val id: String ,val email: String)



    data class Review(val id: Int , val comment: String, val iduser: String , val idres:Int , val date: Date)

}