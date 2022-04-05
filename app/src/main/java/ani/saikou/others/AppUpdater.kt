package ani.saikou.others

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import ani.saikou.*
import okhttp3.OkHttpClient
import okhttp3.Request

object AppUpdater {
    fun check(activity: Activity){
        try{
            val version =
            if(!buildDebug)
                OkHttpClient().newCall(Request.Builder().url("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/stable.txt").build()).execute().body?.string()?.replace("\n","")?:return
            else {
                OkHttpClient().newCall(
                    Request.Builder()
                        .url("https://raw.githubusercontent.com/saikou-app/saikou/main/app/build.gradle")
                        .build()
                ).execute().body?.string()?.substringAfter("versionName \"")?.substringBefore('"') ?: return
            }
            val dontShow = loadData("dont_ask_for_update_$version")?:false
            if(compareVersion(version) && !dontShow && !activity.isDestroyed) activity.runOnUiThread {
                AlertDialog.Builder(activity, R.style.DialogTheme)
                    .setTitle("A new update is available, do you want to check it out?").apply {
                    setMultiChoiceItems(
                        arrayOf("Don't show again for version $version"),
                        booleanArrayOf(false)
                    ) { _, _, isChecked ->
                        if (isChecked) {
                            saveData("dont_ask_for_update_$version", isChecked)
                        }
                    }
                    setPositiveButton("Let's Go") { _: DialogInterface, _: Int ->
                        openLinkInBrowser(if(!buildDebug) "https://github.com/saikou-app/saikou/releases" else "https://discord.com/channels/902174389351620629/946852010198728704")
                    }
                    setNegativeButton("Cope") { dialogInterface: DialogInterface, _: Int ->
                        dialogInterface.dismiss()
                    }
                }.show()
            }
        }
        catch (e:Exception){
            toastString(e.toString())
        }
    }

    private fun compareVersion(version:String):Boolean{
        return try{ version.replace(".","").toInt() > BuildConfig.VERSION_NAME.replace(".","").toInt() } catch (e:Exception){ false }
    }
}