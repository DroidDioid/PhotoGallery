package ru.tim.photogallery.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import ru.tim.photogallery.PhotoDeserializer

interface FlickrApi {

    @GET("services/rest/?method=flickr.interestingness.getList")
    fun fetchPhotos(@Query("page") page: Int): Call<PhotoResponse>

    @GET("services/rest/?method=flickr.photos.search")
    fun searchPhotos(@Query("text") query: String, @Query("page") page: Int): Call<PhotoResponse>

    companion object Factory {

        private const val BASE_URL = "https://www.flickr.com/"

        private val gson =
            GsonBuilder().registerTypeAdapter(PhotoResponse::class.java, PhotoDeserializer())
                .create()

        private val client = OkHttpClient.Builder()
            .addInterceptor(PhotoInterceptor())
            .build()

        private val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()

        val flickrApi: FlickrApi = retrofit.create(FlickrApi::class.java)
    }
}