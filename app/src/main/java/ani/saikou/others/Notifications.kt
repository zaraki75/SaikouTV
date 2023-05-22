package ani.saikou.others

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ani.saikou.FileUrl
import ani.saikou.R
import ani.saikou.anilist.UrlMedia
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("MemberVisibilityCanBePrivate", "unused")
class Notifications {
    enum class Group(val title: String, val icon: Int) {
        ANIME_GROUP("New Episodes", R.drawable.ic_round_movie_filter_24),
        MANGA_GROUP("New Chapters", R.drawable.ic_round_menu_book_24)
    }

    companion object {

        fun getIntent(context: Context, mediaId: Int): PendingIntent {
            val notifyIntent = Intent(context, UrlMedia::class.java)
                .putExtra("media", mediaId)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            return PendingIntent.getActivity(
                context, 0, notifyIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
        }

        private fun createChannel(context: Context, group: Group, id: String, name: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val mChannel = NotificationChannel(id, name, importance)

                val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                notificationManager.createNotificationChannelGroup(NotificationChannelGroup(group.name, group.title))
                mChannel.group = group.name

                notificationManager.createNotificationChannel(mChannel)
            }
        }

        fun deleteChannel(context: Context, id: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.deleteNotificationChannel(id)
            }
        }

        fun getNotification(
            context: Context,
            group: Group,
            channelId: String,
            title: String,
            text: String
        ): NotificationCompat.Builder {
            createChannel(context, group, channelId, title)
            return NotificationCompat.Builder(context, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(group.icon)
                .setContentTitle(title)
                .setContentText(text)
        }

        suspend fun getNotification(
            context: Context,
            group: Group,
            channelId: String,
            title: String,
            text: String,
            img: FileUrl?
        ): NotificationCompat.Builder {
            val builder = getNotification(context, group, channelId, title, text)
            return if(img!=null) {
                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(GlideUrl(img.url) { img.headers })
                        .submit()
                        .get()
                }
                builder.setLargeIcon(bitmap)
            } else builder
        }

        suspend fun getNotification(
            context: Context,
            group: Group,
            channelId: String,
            title: String,
            text: String,
            img: String? = null
        ): NotificationCompat.Builder {
            return getNotification(context, group, channelId, title, text, if(img!=null) FileUrl(img) else null)
        }
    }
}