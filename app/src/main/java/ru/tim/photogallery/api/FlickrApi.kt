package ru.tim.photogallery.api

import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import ru.tim.photogallery.PhotoDeserializer

interface FlickrApi {

    @GET(
        "services/rest/?method=flickr.interestingness.getList" +
                "&api_key=1d28696a42f622145ec2d65d23c1efda" +
                "&extras=url_s" +
                "&format=json" +
                "&nojsoncallback=1"
    )
    fun fetchPhotos(): Call<PhotoResponse>

    @GET(
        "services/rest/?method=flickr.interestingness.getList" +
                "&api_key=1d28696a42f622145ec2d65d23c1efda" +
                "&extras=url_s" +
                "&format=json" +
                "&nojsoncallback=1" +
                "&per_page=30"
    )
    fun fetchPhotos(@Query("page") page: Int): Call<PhotoResponse>

    companion object Factory {

        private const val BASE_URL = "https://www.flickr.com/"

        private val gson =
            GsonBuilder().registerTypeAdapter(PhotoResponse::class.java, PhotoDeserializer())
                .create()

        private val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val flickrApi: FlickrApi = retrofit.create(FlickrApi::class.java)
    }
}