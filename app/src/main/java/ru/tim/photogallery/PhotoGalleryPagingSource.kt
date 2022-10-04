package ru.tim.photogallery

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.tim.photogallery.api.FlickrApi
import ru.tim.photogallery.api.PhotoResponse

class PhotoGalleryPagingSource(private val flickrFetcher: FlickrFetcher) :
    PagingSource<Int, GalleryItem>() {

    override fun getRefreshKey(state: PagingState<Int, GalleryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryItem> {
        return try {
            val nextPageNumber = params.key ?: 1
            val response = flickrFetcher.fetchPhotos(nextPageNumber)
            LoadResult.Page(
                data = response.galleryItems,
                prevKey = null,
                nextKey = if (response.page < response.pages) response.page + 1 else null
            )
        } catch (e: Exception) {
            Log.e("PG", "exep", e)
            LoadResult.Error(e)
        }
    }
}