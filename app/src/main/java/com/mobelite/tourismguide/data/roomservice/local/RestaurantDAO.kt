package com.mobelite.tourismguide.data.roomservice.local

import android.arch.persistence.room.*
import com.mobelite.tourismguide.data.roomservice.model.Restaurant
import io.reactivex.Flowable

@Dao
interface RestaurantDAO {

    @get:Query("SELECT * FROM restaurant")
    val allRestaurants: Flowable<List<Restaurant>>

    @Query("SELECT * FROM restaurant WHERE id=:ID")
    fun getRestaurantById(ID: Int): Flowable<Restaurant>

    @Insert
    fun insertRestaurant(vararg restaurant: Restaurant)

    @Query("SELECT * FROM restaurant WHERE Fav==1")
    fun getFavourites(): Flowable<List<Restaurant>>

    @Query("SELECT * FROM restaurant WHERE UserID=:ID")
    fun getMyRestaurants(ID:String): Flowable<List<Restaurant>>

//    @Update
//    fun updateRestaurant(vararg restaurant: Restaurant)
//
//    @Delete
//    fun deleteRestaurant(restaurant: Restaurant)

    @Query("DELETE  FROM restaurant")
    fun deleteAll()
}