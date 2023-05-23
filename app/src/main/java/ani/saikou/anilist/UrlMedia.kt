package ani.saikou.anilist

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import ani.saikou.loadMedia
import ani.saikou.logError
import ani.saikou.startMainActivity

class UrlMedia : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var id : Int? = intent?.extras?.getInt("media") ?: 0
        var isMAL = false
        if(id==0){
            val data: Uri? = intent?.data
            isMAL = data?.host != "anilist.co"
            try {
                if (data?.pathSegments?.get(1) != null) id = data.pathSegments?.get(1)?.toIntOrNull()
            } catch (e: Exception) {
                logError(e)
            }
        }
        else loadMedia = id
        startMainActivity(this, bundleOf("media" to id,"mal" to isMAL))
    }
}