package ani.saikou.settings

import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import ani.saikou.*
import ani.saikou.anime.source.AnimeSources
import ani.saikou.databinding.ActivitySettingsBinding
import ani.saikou.manga.source.MangaSources

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        binding.settingsBack.setOnClickListener {
            onBackPressed()
        }

        println(AnimeSources.names)
        binding.animeSource.setText(AnimeSources.names[loadData("settings_default_anime_source") ?: 0])
        binding.animeSource.setAdapter(null)
        binding.animeSource.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, AnimeSources.names))
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_default_anime_source",i)
        }

        binding.settingsPlayer.setOnClickListener{
            toastString("Warks in Porgassss",this)
        }

        binding.mangaSource.setText(MangaSources.names[loadData("settings_default_manga_source") ?: 0])
        binding.mangaSource.setAdapter(null)
        binding.mangaSource.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, MangaSources.names))
        binding.mangaSource.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_default_manga_source",i)
        }

        binding.settingsReader.setOnClickListener{
            toastString("Warks in Porgassss",this)
        }

        val uiSettings : UserInterface = loadData("settings_ui")?: UserInterface()
        var previous:View = when(uiSettings.darkMode){
            null->binding.settingsUiAuto
            true->binding.settingsUiDark
            false->binding.settingsUiLight
        }
        previous.alpha = 1f
        fun uiTheme(mode:Boolean?,current: View){
            previous.alpha = 0.33f
            previous = current
            current.alpha = 1f
            uiSettings.darkMode = mode
            saveData("settings_ui",uiSettings)
            initActivity(this)
        }

        binding.settingsUiAuto.setOnClickListener {
            uiTheme(null,it)
        }

        binding.settingsUiLight.setOnClickListener {
            uiTheme(false,it)
        }

        binding.settingsUiDark.setOnClickListener {
            uiTheme(true,it)
        }

        binding.settingsInfo.setOnClickListener {
            if(binding.settingsInfo.maxLines == 3)
                binding.settingsInfo.maxLines = 100
            else
                binding.settingsInfo.maxLines = 3
        }

        binding.loginDiscord.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.discord))))
        }
        binding.loginTelegram.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.telegram))))
        }
        binding.loginGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github))))
        }

        binding.settingsUi.setOnClickListener{
            toastString("Warks in Porgassss",this)
        }

        (binding.settingsLogo.drawable as Animatable).start()
        val array = resources.getStringArray(R.array.tips)

        binding.settingsLogo.setSafeOnClickListener {
            (binding.settingsLogo.drawable as Animatable).start()
            if(Math.random()*69<42.0)
               toastString(array[(Math.random()*array.size).toInt()],this)
        }

        binding.settingsDev.setOnClickListener{
            toastString("Warks in Porgassss",this)
        }
        binding.settingsDisclaimer.setOnClickListener {
            AlertDialog.Builder(this,R.style.DialogTheme).apply {
                setTitle(R.string.disclaimer)
                setView(ScrollView(context).apply {
                    isVerticalScrollBarEnabled = true
                    addView(TextView(context).apply {
                        setPadding(32f.px)
                        setText(R.string.full_disclaimer)
                    })
                })
                show()
            }
        }
    }
}