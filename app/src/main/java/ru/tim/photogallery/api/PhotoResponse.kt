package ru.tim.photogallery.api

import com.google.gson.annotations.SerializedName
import ru.tim.photogallery.GalleryItem
import kotlin.properties.Delegates

class PhotoResponse {

    var page: Int = 1
    var pages: Int = 1

    @SerializedName("photo")
    lateinit var galleryItems: List<GalleryItem>
}