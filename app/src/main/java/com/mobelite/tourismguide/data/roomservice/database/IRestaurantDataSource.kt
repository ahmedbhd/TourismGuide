package com.mobelite.tourismguide.data.roomservice.database

import com.mobelite.tourismguide.data.roomservice.model.Restaurant
import io.reactivex.Flowable

interface IRestaurantDataSource {

    val allRestaurants: Flowable<List<Restaurant>>
    fun getRestaurantById(id: Int): Flowable<Restaurant>
    fun insertRestaurant(vararg restaurant: Restaurant)
    fun getMyRestaurants(id: String): Flowable<List<Restaurant>>
    fun getFavourites(): Flowable<List<Restaurant>>

    //    fun updateRestaurant(vararg restaurant: Restaurant)
//    fun deleteRestaurant(restaurant: Restaurant)
    fun deleteAll()
}