package ru.tim.photogallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import ru.tim.photogallery.api.FlickrApi

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {

    private val flickrFetcher = FlickrFetcher(FlickrApi.flickrApi)
    private val queryFlow = MutableStateFlow("")
    val searchTerm: String
        get() = queryFlow.value

    init {
        queryFlow.value = QueryPreferences.getStoredQuery(app)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val galleryItemFlow = queryFlow.flatMapLatest { query ->
        Pager(PagingConfig(pageSize = 100)) {
            PhotoGalleryPagingSource(flickrFetcher, query)
        }.flow.cachedIn(viewModelScope)
    }

    fun searchPhotos(query: String) {
        QueryPreferences.setStoredQuery(app, query)
        queryFlow.value = query
    }

    override fun onCleared() {
        super.onCleared()
        flickrFetcher.cancelRequestInFlight()
    }

}