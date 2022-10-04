package ru.tim.photogallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import ru.tim.photogallery.api.FlickrApi

class PhotoGalleryViewModel : ViewModel() {

    private val flickrFetcher = FlickrFetcher(FlickrApi.flickrApi)

    val galleryItemFlow = Pager(PagingConfig(pageSize = 21)) {
        PhotoGalleryPagingSource(flickrFetcher)
    }.flow
        .cachedIn(viewModelScope)//???

    //val galleryItemLiveData: LiveData<List<GalleryItem>> = flickrFetcher.fetchPhotos()

    override fun onCleared() {
        super.onCleared()
        flickrFetcher.cancelRequestInFlight()
    }

}