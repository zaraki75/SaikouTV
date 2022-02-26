package ani.saikou.media

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistSearch
import ani.saikou.anilist.SearchResults
import ani.saikou.databinding.ActivitySearch2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SearchActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySearch2Binding
    private val scope = lifecycleScope
    val model: AnilistSearch by viewModels()

    var type = "ANIME"
    var style : Int=0
    private var screenWidth:Float = 0f

    private lateinit var mediaAdaptor:MediaAdaptor
    private lateinit var progressAdapter:ProgressAdapter
    private lateinit var concatAdapter:ConcatAdapter

    private var clear=true
    var searchText:String = ""
    var genre:String = ""
    var sortBy:String = ""
    var tag:String = ""
    var adult = false
    var listOnly = false

    private lateinit var searchResults:SearchResults

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearch2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)
        screenWidth = resources.displayMetrics.run { widthPixels / density }

        binding.searchRecyclerView.updatePaddingRelative(top= statusBarHeight,bottom = navBarHeight +80f.px)

        type = intent.getStringExtra("type")?:type
        genre = intent.getStringExtra("genre")?:""
        sortBy = intent.getStringExtra("sortBy")?:""
        style = loadData<Int>("searchStyle") ?:0
        adult = if(Anilist.adult) intent.getBooleanExtra("hentai", false) else false

        searchResults = SearchResults(type,false, results = arrayListOf(), hasNextPage = false)
        val headerAdaptor=SearchAdapter(this)
        progressAdapter = ProgressAdapter()
        mediaAdaptor = MediaAdaptor(style,searchResults.results,this,matchParent = true)

        val gridSize=(screenWidth / 124f).toInt()
        val gridLayoutManager = GridLayoutManager(this,gridSize)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (position) {
                    0 -> gridSize
                    concatAdapter.itemCount-1 -> gridSize
                    else -> when (style) {
                        0 -> 1
                        else -> gridSize
                    }
                }
            }
        }

        concatAdapter = ConcatAdapter(headerAdaptor,mediaAdaptor,progressAdapter)

        binding.searchRecyclerView.layoutManager = gridLayoutManager
        binding.searchRecyclerView.adapter = concatAdapter

        model.getSearch().observe(this){
            if(it!=null){
                searchResults.apply {
                    isAdult = it.isAdult
                    perPage = it.perPage
                    search = it.search
                    sort = it.sort
                    genres = it.genres
                    tags = it.tags
                    format = it.format
                    page = it.page
                    hasNextPage = it.hasNextPage
                }

                val prev = searchResults.results.size
                searchResults.results.addAll(it.results)
                mediaAdaptor.notifyItemRangeInserted(prev,it.results.size)

                if(it.hasNextPage)
                    progressAdapter.bar.visibility = View.VISIBLE
                else
                    progressAdapter.bar.visibility = View.GONE
            }
        }
    }
    var searchTimer = Timer()

    fun search(clearSearch:Boolean,search:String?=null,genre:String?=null,tag:String?=null,sortBy:String?=null,adult:Boolean=false,listOnly:Boolean=false){
        clear = clearSearch
        if(clear){
            val size = searchResults.results.size
            searchResults.results.clear()
            mediaAdaptor.notifyItemRangeRemoved(0,size)
            progressAdapter.bar.visibility = View.VISIBLE
        }

        this.genre = genre?:this.genre
        this.searchText = search?:this.searchText
        this.adult = adult
        this.tag = tag?:this.tag
        this.listOnly = listOnly

        searchTimer.cancel()
        searchTimer.purge()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                scope.launch(Dispatchers.IO) {
                    model.loadSearch(type,search, if(genre!=null) arrayListOf(genre) else null,if(tag!=null) arrayListOf(tag) else null,sortBy,adult,listOnly)
                }
            }
        }
        searchTimer = Timer()
        searchTimer.schedule(timerTask, 500)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun recycler(){
        mediaAdaptor.type = style
        mediaAdaptor.notifyDataSetChanged()
    }
}