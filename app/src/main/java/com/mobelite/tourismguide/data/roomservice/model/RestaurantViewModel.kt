package com.mobelite.tourismguide.data.roomservice.model

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.mobelite.tourismguide.data.roomservice.database.RestaurantRepository
import io.reactivex.Flowable

class RestaurantViewModel(application: Application, restaurantRepository: RestaurantRepository) : AndroidViewModel(application) {

    private val mRepository: RestaurantRepository = RestaurantRepository(restaurantRepository)

    private val allRes: Flowable<List<Restaurant>>

    init {
        allRes = mRepository.allRestaurants
    }

    fun insert(restaurant: Restaurant) {
        mRepository.insertRestaurant(restaurant)
    }
}