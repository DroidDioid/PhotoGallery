package ru.tim.photogallery

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue


fun Number.toDp(context: Context): Float =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_PX,
        this.toFloat(),
        context.resources.displayMetrics
    )