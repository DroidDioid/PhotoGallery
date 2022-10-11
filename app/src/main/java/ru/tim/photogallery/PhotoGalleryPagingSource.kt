package ru.tim.photogallery

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState

class PhotoGalleryPagingSource(
    private val flickrFetcher: FlickrFetcher,
    private val query: String
) :
    PagingSource<Int, GalleryItem>() {

    override fun getRefreshKey(state: PagingState<Int, GalleryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            Log.i("RRRRr", "getRefreshKey")
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryItem> {
        return try {
            val nextPageNumber = params.key ?: 1
            Log.i("RRRRr", "page $nextPageNumber")

            val response = if (query.isBlank()) {
                flickrFetcher.fetchPhotos(nextPageNumber)
            } else {
                flickrFetcher.searchPhotos(query, nextPageNumber)
            }

            LoadResult.Page(
                data = response.galleryItems,
                prevKey = null,
                nextKey = if (response.page < response.pages) response.page + 1 else null
            )
        } catch (e: Exception) {
            Log.e("EEEE", "exep", e)
            LoadResult.Error(e)
        }
    }
}