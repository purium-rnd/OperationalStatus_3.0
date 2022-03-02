package com.example.test.serverApi

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ForecastInfoService {

    @GET("/data/2.5/forecast/")
    fun getForecastInfo(
        @Query("lat") lat:String,
        @Query("lon") lon:String,
        @Query("appid") appid:String
    ): Call<ForecastData>
}