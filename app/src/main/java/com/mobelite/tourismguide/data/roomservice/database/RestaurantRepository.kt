package com.mobelite.tourismguide.data.roomservice.database

import com.mobelite.tourismguide.data.roomservice.model.Restaurant
import io.reactivex.Flowable

class RestaurantRepository(private val mLocationDataSource: IRestaurantDataSource) : IRestaurantDataSource {
    override val allRestaurants: Flowable<List<Restaurant>>
        get() = mLocationDataSource.allRestaurants

    override fun getRestaurantById(id: Int): Flowable<Restaurant> {
        return mLocationDataSource.getRestaurantById(id)
    }

    override fun insertRestaurant(vararg restaurant: Restaurant) {
        mLocationDataSource.insertRestaurant(*restaurant)
    }

    override fun updateRestaurant(vararg restaurant: Restaurant) {
        mLocationDataSource.updateRestaurant(*restaurant)
    }

    override fun deleteRestaurant(restaurant: Restaurant) {
        mLocationDataSource.deleteRestaurant(restaurant)
    }

    override fun deleteAll() {
        mLocationDataSource.deleteAll()
    }


    companion object {
        private var mInstance: RestaurantRepository? = null
        fun getInstance(mLocationDataSource: IRestaurantDataSource): RestaurantRepository {
            if (mInstance==null)
                mInstance = RestaurantRepository(mLocationDataSource)
            return mInstance!!
        }
    }
}