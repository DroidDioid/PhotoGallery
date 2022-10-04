package ru.tim.photogallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import ru.tim.photogallery.api.FlickrApi

class PhotoGalleryFragment : Fragment() {

    private val photoGalleryViewModel by lazy {
        ViewModelProvider(this).get(PhotoGalleryViewModel::class.java)
    }
    private lateinit var photoRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)

        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                photoRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val spanCount =
                    (photoRecyclerView.width.toDp(requireContext()) / SPAN_WIDTH_DP).toInt()
                photoRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
            }
        }

        photoRecyclerView.viewTreeObserver.addOnGlobalLayoutListener(listener)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoPagingAdapter = PhotoAdapter(PhotoComparator)
        photoRecyclerView.adapter = photoPagingAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            photoGalleryViewModel.galleryItemFlow.collectLatest { pagingData ->
                photoPagingAdapter.submitData(pagingData)
            }
        }

    }

    private class PhotoHolder(itemTextView: TextView) : RecyclerView.ViewHolder(itemTextView) {

        val bindTitle: (CharSequence?) -> Unit = itemTextView::setText
    }

    private class PhotoAdapter(diffCallback: DiffUtil.ItemCallback<GalleryItem>) :
        PagingDataAdapter<GalleryItem, PhotoHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val textView = TextView(parent.context)
            return PhotoHolder(textView)
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = getItem(position)
            holder.bindTitle(galleryItem?.title)
        }

    }

    object PhotoComparator : DiffUtil.ItemCallback<GalleryItem>() {
        override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val TAG = "PhotoGalleryFragment"
        private const val SPAN_WIDTH_DP = 300
        fun newInstance() = PhotoGalleryFragment()
    }
}