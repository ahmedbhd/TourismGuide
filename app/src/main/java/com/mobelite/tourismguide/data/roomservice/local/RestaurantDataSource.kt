package com.mobelite.tourismguide.data.roomservice.local

import com.mobelite.tourismguide.data.roomservice.database.IRestaurantDataSource
import com.mobelite.tourismguide.data.roomservice.model.Restaurant
import io.reactivex.Flowable

class RestaurantDataSource(private val restaurantDAO: RestaurantDAO) : IRestaurantDataSource {
    override val allRestaurants: Flowable<List<Restaurant>>
        get() = restaurantDAO.allRestaurants

    override fun getRestaurantById(id: Int): Flowable<Restaurant> {
        return restaurantDAO.getRestaurantById(id)
    }

    override fun insertRestaurant(vararg restaurant: Restaurant) {
        restaurantDAO.insertRestaurant(*restaurant)
    }

    override fun updateRestaurant(vararg restaurant: Restaurant) {
        restaurantDAO.updateRestaurant(* restaurant)
    }

    override fun deleteRestaurant(restaurant: Restaurant) {
        restaurantDAO.deleteRestaurant(restaurant)
    }

    override fun deleteAll() {
        restaurantDAO.deleteAll()
    }

    companion object {
        private var mInstance: RestaurantDataSource? = null
        fun getInstance(restaurantDAO: RestaurantDAO): RestaurantDataSource {
            if (mInstance==null)
                mInstance = RestaurantDataSource(restaurantDAO)
            return mInstance!!
        }
    }
}