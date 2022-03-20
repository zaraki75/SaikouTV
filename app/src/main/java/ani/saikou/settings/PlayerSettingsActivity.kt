package ani.saikou.settings

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.saikou.databinding.ActivityPlayerSettingsBinding
import ani.saikou.initActivity
import ani.saikou.navBarHeight
import ani.saikou.statusBarHeight

class PlayerSettingsActivity : AppCompatActivity() {
    lateinit var binding : ActivityPlayerSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        binding.playerSettingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
    }
}