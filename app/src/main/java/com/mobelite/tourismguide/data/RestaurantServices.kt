package com.mobelite.tourismguide.data

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

interface RestaurantServices {


    companion object {
        fun create(): RestaurantServices {

            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(
                            RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .baseUrl("http://192.168.1.10:3000/")
                    .build()

            return retrofit.create(RestaurantServices::class.java)
        }
    }


    @GET("selectfav")
    fun selectfav(@Query("id") id: String): Observable<List<Model.ResultRestaurant>>



    @GET("selectAll")
        fun selectAll(): Observable<List<Model.ResultRestaurant>>


    @GET("selectmy")
    fun selectmy(@Query("id") id: String): Observable<List<Model.ResultRestaurant>>


    @DELETE("deletefav")
    fun deletefav(@Query("user") user: String,
                      @Query("res") res: String): Observable<String>


    @DELETE("deleterest")
    fun deleterest(@Query("user") user: String,
                  @Query("res") res: String): Observable<String>

    @POST("insert")
    fun insert(@Body resultRestaurant: Model.ResultRestaurant): Observable<String>


    @POST("insertfav")
    fun insertfav(@Body favRestaurant: Model.FavRestaurant): Observable<String>


    @PUT("updaterest")
    fun updaterest(@Body resultRestaurant: Model.ResultRestaurant): Observable<String>





}