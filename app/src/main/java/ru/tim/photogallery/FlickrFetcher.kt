package ru.tim.photogallery

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.tim.photogallery.api.FlickrApi
import ru.tim.photogallery.api.PhotoResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FlickrFetcher(private val flickrApi: FlickrApi) {

    private lateinit var flickrRequest: Call<PhotoResponse>

    fun fetchPhotos(): LiveData<List<GalleryItem>> {
        val responseLiveData = MutableLiveData<List<GalleryItem>>()
        flickrRequest = flickrApi.fetchPhotos()

        flickrRequest.enqueue(object : Callback<PhotoResponse> {

            override fun onFailure(call: Call<PhotoResponse>, t: Throwable) {
                Log.e(TAG, "Failed to fetch photos", t)
            }

            override fun onResponse(call: Call<PhotoResponse>, response: Response<PhotoResponse>) {
                Log.d(TAG, "Response received: ${response.body()}")
                val photoResponse = response.body()
                var galleryItems = photoResponse?.galleryItems ?: mutableListOf()
                galleryItems = galleryItems.filterNot { it.url.isBlank() }

                responseLiveData.value = galleryItems
            }
        })

        return responseLiveData
    }

    suspend fun fetchPhotos(page: Int): PhotoResponse {
        return suspendCoroutine { continuation ->
            flickrRequest = flickrApi.fetchPhotos(page)

            flickrRequest.enqueue(object : Callback<PhotoResponse> {

                override fun onFailure(call: Call<PhotoResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to fetch photos", t)
                    continuation.resumeWithException(t)
                }

                override fun onResponse(
                    call: Call<PhotoResponse>,
                    response: Response<PhotoResponse>
                ) {
                    Log.d(TAG, "Response received: ${response.body()}")
                    val photoResponse =
                        response.body() ?: PhotoResponse().apply { galleryItems = mutableListOf() }
                    var galleryItems = photoResponse.galleryItems
                    galleryItems = galleryItems.filterNot { it.url.isBlank() }

                    photoResponse.galleryItems = galleryItems

                    continuation.resume(photoResponse)
                }
            })
        }
    }

    fun cancelRequestInFlight() {
        if (::flickrRequest.isInitialized) flickrRequest.cancel()
    }

    companion object {
        private const val TAG = "FlickrFetchr"
    }
}