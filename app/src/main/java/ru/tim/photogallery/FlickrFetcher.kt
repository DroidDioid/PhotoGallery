package ru.tim.photogallery

import android.util.Log
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

    fun fetchPhotosRequest(page: Int): Call<PhotoResponse> {
        return flickrApi.fetchPhotos(page)
    }

    suspend fun fetchPhotos(page: Int): PhotoResponse {
        return fetchPhotosMetaData(fetchPhotosRequest(page))
    }

    fun searchPhotosRequest(text: String, page: Int): Call<PhotoResponse> {
        return flickrApi.searchPhotos(text, page)
    }

    suspend fun searchPhotos(text: String, page: Int): PhotoResponse {
        return fetchPhotosMetaData(searchPhotosRequest(text, page))
    }

    private suspend fun fetchPhotosMetaData(flickrRequest: Call<PhotoResponse>): PhotoResponse {
        this.flickrRequest = flickrRequest
        return suspendCoroutine { continuation ->
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