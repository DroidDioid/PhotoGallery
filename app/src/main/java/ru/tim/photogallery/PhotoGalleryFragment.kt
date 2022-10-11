package ru.tim.photogallery

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PhotoGalleryFragment : VisibleFragment() {

    private val photoGalleryViewModel by lazy {
        ViewModelProvider(this).get(PhotoGalleryViewModel::class.java)
    }
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)

        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        progressBar = view.findViewById(R.id.progressBar)
        retryButton = view.findViewById(R.id.retryButton)

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

        retryButton.setOnClickListener {
            photoPagingAdapter.retry()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            photoGalleryViewModel.galleryItemFlow.collectLatest { pagingData ->
                photoPagingAdapter.submitData(pagingData)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            photoPagingAdapter.loadStateFlow.collectLatest { loadStates ->
                Log.i("EEEE", "loadStates $loadStates")
                progressBar.isVisible = loadStates.refresh is LoadState.Loading

                if (loadStates.refresh is LoadState.Loading || loadStates.refresh is LoadState.Error)
                    photoRecyclerView.adapter = null
                else if (photoRecyclerView.adapter == null) {
                    photoRecyclerView.adapter = photoPagingAdapter
                }

                retryButton.isVisible =
                    loadStates.refresh is LoadState.Error || loadStates.append is LoadState.Error

                if (loadStates.refresh is LoadState.Error) {
                    Toast.makeText(context, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    photoGalleryViewModel.searchPhotos(query)

                    searchView.setQuery("", false)
                    searchView.isIconified = true

                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })

        searchView.setOnSearchClickListener {
            searchView.setQuery(photoGalleryViewModel.searchTerm, false)
        }

        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        val toggleItemTitle = if (isPolling) {
            R.string.stop_polling
        } else {
            R.string.start_polling
        }
        toggleItem.setTitle(toggleItemTitle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_clear -> {
                photoGalleryViewModel.searchPhotos("")
                true
            }
            R.id.menu_item_toggle_polling -> {
                val isPolling = QueryPreferences.isPolling(requireContext())
                if (isPolling) {
                    WorkManager.getInstance(requireContext()).cancelUniqueWork(POLL_WORK)
                    QueryPreferences.setPolling(requireContext(), false)
                } else {
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        //.setRequiresCharging(true)
                        .build()
                    val periodicRequest =
                        PeriodicWorkRequestBuilder<PollWorker>(15, TimeUnit.MINUTES)
                            .setConstraints(constraints)
                            .build()
                    WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                        POLL_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicRequest
                    )
                    QueryPreferences.setPolling(requireContext(), true)
                }
                activity?.invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class PhotoHolder(private val itemImageView: ImageView) :
        RecyclerView.ViewHolder(itemImageView), OnClickListener {

        private lateinit var galleryItem: GalleryItem

        init {
            itemImageView.setOnClickListener(this)
        }

        fun bindGalleryItem(item: GalleryItem) {
            galleryItem = item

            Glide.with(this@PhotoGalleryFragment)
                .load(galleryItem.url)
                .placeholder(R.drawable.bill_up_close)
                .into(itemImageView)
        }

        override fun onClick(v: View?) {
            val intent = PhotoPageActivity.newIntent(requireContext(), galleryItem.photoPageUri)
            startActivity(intent)

            /*CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.purple_200
                            )
                        )
                        .build()
                )
                .setShowTitle(true)
                .build()
                .launchUrl(requireContext(), galleryItem.photoPageUri)*/
        }
    }

    private inner class PhotoAdapter(diffCallback: DiffUtil.ItemCallback<GalleryItem>) :
        PagingDataAdapter<GalleryItem, PhotoHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val imageView = layoutInflater.inflate(
                R.layout.list_item_gallery,
                parent,
                false
            ) as ImageView
            return PhotoHolder(imageView)
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = getItem(position)

            galleryItem?.let {
                holder.bindGalleryItem(it)
            }
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
        private const val POLL_WORK = "POLL_WORK"
        fun newInstance() = PhotoGalleryFragment()
    }
}