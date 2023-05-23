package ani.saikou.settings

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build.*
import android.os.Build.VERSION.*
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.ActivitySettingsBinding
import ani.saikou.mal.MAL
import ani.saikou.others.AppUpdater
import ani.saikou.others.CustomBottomDialog
import ani.saikou.parsers.AnimeSources
import ani.saikou.parsers.MangaSources
import ani.saikou.subcriptions.Notifications
import ani.saikou.subcriptions.Notifications.Companion.openSettings
import ani.saikou.subcriptions.Subscription.Companion.defaultTime
import ani.saikou.subcriptions.Subscription.Companion.startSubscription
import ani.saikou.subcriptions.Subscription.Companion.timeMinutes
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@SettingsActivity)
    }
    lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.settingsVersion.text = getString(R.string.version_current, BuildConfig.VERSION_NAME)
        binding.settingsVersion.setOnLongClickListener {
            fun getArch(): String {
                SUPPORTED_ABIS.forEach {
                    when (it) {
                        "arm64-v8a"   -> return "aarch64"
                        "armeabi-v7a" -> return "arm"
                        "x86_64"      -> return "x86_64"
                        "x86"         -> return "i686"
                    }
                }
                return System.getProperty("os.arch") ?: System.getProperty("os.product.cpu.abi") ?: "Unknown Architecture"
            }

            val info = """
Saikou Version: ${BuildConfig.VERSION_NAME}
Device: $BRAND $DEVICE
Architecture: ${getArch()}
OS Version: $CODENAME $RELEASE ($SDK_INT)
            """.trimIndent()
            copyToClipboard(info, false)
            toast("Copied device info")
            return@setOnLongClickListener true
        }

        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        onBackPressedDispatcher.addCallback(this, restartMainActivity)

        binding.settingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val animeSource = loadData<Int>("settings_def_anime_source")?.let { if (it >= AnimeSources.names.size) 0 else it } ?: 0
        binding.animeSource.setText(AnimeSources.names[animeSource], false)
        binding.animeSource.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, AnimeSources.names))
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_def_anime_source", i)
            binding.animeSource.clearFocus()
        }

        binding.settingsPlayer.setOnClickListener {
            startActivity(Intent(this, PlayerSettingsActivity::class.java))
        }

        val managers = arrayOf("Default", "1DM", "ADM")
        val downloadManagerDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Download Manager")
        var downloadManager = loadData<Int>("settings_download_manager") ?: 0
        binding.settingsDownloadManager.setOnClickListener {
            downloadManagerDialog.setSingleChoiceItems(managers, downloadManager) { dialog, count ->
                downloadManager = count
                saveData("settings_download_manager", downloadManager)
                dialog.dismiss()
            }.show()
        }

        binding.settingsDownloadInSd.isChecked = loadData("sd_dl") ?: false
        binding.settingsDownloadInSd.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val arrayOfFiles = ContextCompat.getExternalFilesDirs(this, null)
                if (arrayOfFiles.size > 1 && arrayOfFiles[1] != null) {
                    saveData("sd_dl", true)
                } else {
                    binding.settingsDownloadInSd.isChecked = false
                    saveData("sd_dl", false)
                    snackString(getString(R.string.noSdFound))
                }
            } else saveData("sd_dl", false)
        }

        binding.settingsContinueMedia.isChecked = loadData("continue_media") ?: true
        binding.settingsContinueMedia.setOnCheckedChangeListener { _, isChecked ->
            saveData("continue_media", isChecked)
        }

        binding.settingsRecentlyListOnly.isChecked = loadData("recently_list_only") ?: false
        binding.settingsRecentlyListOnly.setOnCheckedChangeListener { _, isChecked ->
            saveData("recently_list_only", isChecked)
        }

        val dns = listOf("None", "Google", "Cloudflare", "AdGuard")
        binding.settingsDns.setText(dns[loadData("settings_dns") ?: 0], false)
        binding.settingsDns.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, dns))
        binding.settingsDns.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_dns", i)
            initializeNetwork(this)
            binding.settingsDns.clearFocus()
        }

        binding.settingsPreferDub.isChecked = loadData("settings_prefer_dub") ?: false
        binding.settingsPreferDub.setOnCheckedChangeListener { _, isChecked ->
            saveData("settings_prefer_dub", isChecked)
        }

        val mangaSource = loadData<Int>("settings_def_manga_source")?.let { if (it >= MangaSources.names.size) 0 else it } ?: 0
        binding.mangaSource.setText(MangaSources.names[mangaSource], false)
        binding.mangaSource.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, MangaSources.names))
        binding.mangaSource.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_def_manga_source", i)
            binding.mangaSource.clearFocus()
        }

        binding.settingsReader.setOnClickListener {
            startActivity(Intent(this, ReaderSettingsActivity::class.java))
        }

        val uiSettings: UserInterfaceSettings =
            loadData("ui_settings", toast = false) ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }
        var previous: View = when (uiSettings.darkMode) {
            null  -> binding.settingsUiAuto
            true  -> binding.settingsUiDark
            false -> binding.settingsUiLight
        }
        previous.alpha = 1f
        fun uiTheme(mode: Boolean?, current: View) {
            previous.alpha = 0.33f
            previous = current
            current.alpha = 1f
            uiSettings.darkMode = mode
            saveData("ui_settings", uiSettings)
            Refresh.all()
            finish()
            startActivity(Intent(this, SettingsActivity::class.java))
            initActivity(this)
        }

        binding.settingsUiAuto.setOnClickListener {
            uiTheme(null, it)
        }

        binding.settingsUiLight.setOnClickListener {
            uiTheme(false, it)
        }

        binding.settingsUiDark.setOnClickListener {
            uiTheme(true, it)
        }

        var previousStart: View = when (uiSettings.defaultStartUpTab) {
            0    -> binding.uiSettingsAnime
            1    -> binding.uiSettingsHome
            2    -> binding.uiSettingsManga
            else -> binding.uiSettingsHome
        }
        previousStart.alpha = 1f
        fun uiTheme(mode: Int, current: View) {
            previousStart.alpha = 0.33f
            previousStart = current
            current.alpha = 1f
            uiSettings.defaultStartUpTab = mode
            saveData("ui_settings", uiSettings)
            initActivity(this)
        }

        binding.uiSettingsAnime.setOnClickListener {
            uiTheme(0, it)
        }

        binding.uiSettingsHome.setOnClickListener {
            uiTheme(1, it)
        }

        binding.uiSettingsManga.setOnClickListener {
            uiTheme(2, it)
        }

        binding.settingsShowYt.isChecked = uiSettings.showYtButton
        binding.settingsShowYt.setOnCheckedChangeListener { _, isChecked ->
            uiSettings.showYtButton = isChecked
            saveData("ui_settings", uiSettings)
        }

        var previousEp: View = when (uiSettings.animeDefaultView) {
            0    -> binding.settingsEpList
            1    -> binding.settingsEpGrid
            2    -> binding.settingsEpCompact
            else -> binding.settingsEpList
        }
        previousEp.alpha = 1f
        fun uiEp(mode: Int, current: View) {
            previousEp.alpha = 0.33f
            previousEp = current
            current.alpha = 1f
            uiSettings.animeDefaultView = mode
            saveData("ui_settings", uiSettings)
        }

        binding.settingsEpList.setOnClickListener {
            uiEp(0, it)
        }

        binding.settingsEpGrid.setOnClickListener {
            uiEp(1, it)
        }

        binding.settingsEpCompact.setOnClickListener {
            uiEp(2, it)
        }

        var previousChp: View = when (uiSettings.mangaDefaultView) {
            0    -> binding.settingsChpList
            1    -> binding.settingsChpCompact
            else -> binding.settingsChpList
        }
        previousChp.alpha = 1f
        fun uiChp(mode: Int, current: View) {
            previousChp.alpha = 0.33f
            previousChp = current
            current.alpha = 1f
            uiSettings.mangaDefaultView = mode
            saveData("ui_settings", uiSettings)
        }

        binding.settingsChpList.setOnClickListener {
            uiChp(0, it)
        }

        binding.settingsChpCompact.setOnClickListener {
            uiChp(1, it)
        }

        binding.settingBuyMeCoffee.setOnClickListener {
            lifecycleScope.launch {
                it.pop()
            }            
            openLinkInBrowser("https://www.buymeacoffee.com/brahmkshatriya")
        }
        lifecycleScope.launch {
            binding.settingBuyMeCoffee.pop()
        }
        binding.settingUPI.visibility = if (checkCountry(this)) View.VISIBLE else View.GONE
        lifecycleScope.launch {
            binding.settingUPI.pop()
        }
        binding.settingUPI.setOnClickListener {
            lifecycleScope.launch {
                it.pop()
            }
            val upi = "upi://pay?pa=brahmkshatriya@apl&pn=Saikou"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(upi)
            }
            startActivity(Intent.createChooser(intent, "Donate with..."))
        }

        binding.loginDiscord.setOnClickListener {
            openLinkInBrowser(getString(R.string.discord))
        }
        binding.loginTelegram.setOnClickListener {
            openLinkInBrowser(getString(R.string.telegram))
        }
        binding.loginGithub.setOnClickListener {
            openLinkInBrowser(getString(R.string.github))
        }

        binding.settingsUi.setOnClickListener {
            startActivity(Intent(this, UserInterfaceSettingsActivity::class.java))
        }

        binding.settingsFAQ.setOnClickListener {
            startActivity(Intent(this, FAQActivity::class.java))
        }

        (binding.settingsLogo.drawable as Animatable).start()
        val array = resources.getStringArray(R.array.tips)

        binding.settingsLogo.setSafeOnClickListener {
            (binding.settingsLogo.drawable as Animatable).start()
            snackString(array[(Math.random() * array.size).toInt()], this)
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, TVConnectionActivity::class.java))
        }

        binding.settingsDev.setOnClickListener{
            DevelopersDialogFragment().show(supportFragmentManager,"dialog")
        }
        binding.settingsForks.setOnClickListener {
            ForksDialogFragment().show(supportFragmentManager, "dialog")
        }
        binding.settingsDisclaimer.setOnClickListener {
            val title = getString(R.string.disclaimer)
            val text = TextView(this)
            text.setText(R.string.full_disclaimer)

            CustomBottomDialog.newInstance().apply {
                setTitleText(title)
                addView(text)
                setNegativeButton("Close") {
                    dismiss()
                }
                show(supportFragmentManager, "dialog")
            }
        }

        var curTime = loadData<Int>("subscriptions_time") ?: defaultTime
        val timeNames = timeMinutes.map {
            val mins = it % 60
            val hours = it / 60
            if(it>0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
            else getString(R.string.do_not_update)
        }.toTypedArray()
        binding.settingsSubscriptionsTime.text = getString(R.string.subscriptions_checking_time_s, timeNames[curTime])
        val speedDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle(R.string.subscriptions_checking_time)
        binding.settingsSubscriptionsTime.setOnClickListener {
            speedDialog.setSingleChoiceItems(timeNames, curTime) { dialog, i ->
                curTime = i
                binding.settingsSubscriptionsTime.text = getString(R.string.subscriptions_checking_time_s, timeNames[i])
                saveData("subscriptions_time", curTime)
                dialog.dismiss()
                startSubscription(true)
            }.show()
        }

        binding.settingsSubscriptionsTime.setOnLongClickListener {
            startSubscription(true)
            true
        }

        binding.settingsNotificationsCheckingSubscriptions.isChecked = loadData("subscription_checking_notifications") ?: true
        binding.settingsNotificationsCheckingSubscriptions.setOnCheckedChangeListener { _, isChecked ->
            saveData("subscription_checking_notifications", isChecked)
            if (isChecked)
                Notifications.createChannel(
                    this,
                    null,
                    "subscription_checking",
                    getString(R.string.checking_subscriptions),
                    false
                )
            else
                Notifications.deleteChannel(this, "subscription_checking")
        }

        binding.settingsNotificationsCheckingSubscriptions.setOnLongClickListener {
            openSettings(this, null)
        }


        binding.settingsCheckUpdate.isChecked = loadData("check_update") ?: true
        binding.settingsCheckUpdate.setOnCheckedChangeListener { _, isChecked ->
            saveData("check_update", isChecked)
            if (!isChecked) {
                snackString("You Long Click the button to check for App Update")
            }
        }

        binding.settingsLogo.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppUpdater.check(this@SettingsActivity, true)
            }
            true
        }

        binding.settingsCheckUpdate.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppUpdater.check(this@SettingsActivity, true)
            }
            true
        }
          
        binding.settingsAccountHelp.setOnClickListener {
            val title = getString(R.string.account_help)
            val full = getString(R.string.full_account_help)
            CustomBottomDialog.newInstance().apply {
                setTitleText(title)
                addView(
                    TextView(it.context).apply {
                        val markWon = Markwon.builder(it.context).usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
                        markWon.setMarkdown(this, full)
                    }
                )
            }.show(supportFragmentManager, "dialog")
        }

        fun reload() {
            if (Anilist.token != null) {
                binding.settingsAnilistLogin.setText(R.string.logout)
                binding.settingsAnilistLogin.setOnClickListener {
                    Anilist.removeSavedToken(it.context)
                    restartMainActivity.isEnabled = true
                    reload()
                }
                binding.settingsAnilistUsername.visibility = View.VISIBLE
                binding.settingsAnilistUsername.text = Anilist.username
                binding.settingsAnilistAvatar.loadImage(Anilist.avatar)

                binding.settingsMALLoginRequired.visibility = View.GONE
                binding.settingsMALLogin.visibility = View.VISIBLE
                binding.settingsMALUsername.visibility = View.VISIBLE

                if (MAL.token != null) {
                    binding.settingsMALLogin.setText(R.string.logout)
                    binding.settingsMALLogin.setOnClickListener {
                        MAL.removeSavedToken(it.context)
                        restartMainActivity.isEnabled = true
                        reload()
                    }
                    binding.settingsMALUsername.visibility = View.VISIBLE
                    binding.settingsMALUsername.text = MAL.username
                    binding.settingsMALAvatar.loadImage(MAL.avatar)
                } else {
binding.settingsMALAvatar.setImageResource(R.drawable.ic_round_person_24)                
                    binding.settingsMALUsername.visibility = View.GONE
                    binding.settingsMALLogin.setText(R.string.login)
                    binding.settingsMALLogin.setOnClickListener {
                        MAL.loginIntent(this)
                    }
                }
            } else {
binding.settingsAnilistAvatar.setImageResource(R.drawable.ic_round_person_24)            
                binding.settingsAnilistUsername.visibility = View.GONE
                binding.settingsAnilistLogin.setText(R.string.login)
                binding.settingsAnilistLogin.setOnClickListener {
                    Anilist.loginIntent(this)
                }
                binding.settingsMALLoginRequired.visibility = View.VISIBLE
                binding.settingsMALLogin.visibility = View.GONE
                binding.settingsMALUsername.visibility = View.GONE
            }
        }
        reload()
    }
}    