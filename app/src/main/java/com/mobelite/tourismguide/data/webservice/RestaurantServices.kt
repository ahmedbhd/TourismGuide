package com.mobelite.tourismguide.data.webservice

import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface RestaurantServices {


    companion object {
        fun create(): RestaurantServices {

            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(
                            RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
//                    .addConverterFactory(ScalarsConverterFactory.create())
                    .baseUrl("http://192.168.1.4:3000/")
                    .build()

            return retrofit.create(RestaurantServices::class.java)
        }
    }

    //========================= Restaurant WS ====================================
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

    @GET("isfav/{user}/{idres}")
    fun ifFavourite(@Path("user") user: String, @Path("idres") idres: String): Observable<String>

    //==================================== user ws ====================================
    @POST("insertuser")
    fun insertUser(@Body User: Model.User): Observable<String>


    //==================================== comments ws ====================================
    @GET("allCmnts")
    fun allCmnts(@Query("rest") id: String): Observable<List<Model.Review>>

    @GET("myCmnts")
    fun myCmnts(@Query("user") user: String, @Query("rest") rest: String): Observable<List<Model.Review>>

    @DELETE("deleteCmnt")
    fun deleteCmnt(@Query("id") id: String): Observable<String>

    @POST("insertCmnt")
    fun insertCmnt(@Body review: Model.Review): Observable<String>

    @PUT("updateCmnt")
    fun updateCmnt(@Body review: Model.Review): Observable<String>

    @GET("cmntCount/{rest}")
    fun cmntCount(@Path("rest") id: Int): Observable<String>


    //==================================== rating ws ====================================

    @GET("allRatings")
    fun allRatings(@Query("rest") id: String): Observable<List<Model.Rating>>

    @POST("insertRate")
    fun insertRate(@Body rating: Model.Rating): Observable<String>
}