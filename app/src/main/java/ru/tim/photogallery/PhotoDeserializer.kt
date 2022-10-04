package ru.tim.photogallery

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import ru.tim.photogallery.api.PhotoResponse
import java.lang.reflect.Type

class PhotoDeserializer : JsonDeserializer<PhotoResponse> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PhotoResponse? {

        return Gson().fromJson(
            json?.asJsonObject?.get("photos"),
            PhotoResponse::class.java
        )
    }
}