package ani.saikou.subcriptions

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ani.saikou.*
import ani.saikou.parsers.Episode
import ani.saikou.parsers.MangaChapter

class Subscription {
    companion object {
        const val defaultTime = 8
        val timeMinutes = arrayOf(0L, 5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, 1440)

        private var alreadyStarted = false
        fun Context.startSubscription(force: Boolean = false) {
            if (!alreadyStarted || force) {
                alreadyStarted = true
                SubscriptionWorker.enqueue(this)
                AlarmReceiver.alarm(this)
            } else logger("Already Subscribed")
        }

        private var currentlyPerforming = false
        suspend fun perform(context: Context) {
            if (!currentlyPerforming) tryWithSuspend {
                currentlyPerforming = true
                App.context = context

                val subscriptions = SubscriptionHelper.getSubscriptions(context)
                var i = 0
                val index = subscriptions.map { i++; it.key to i }.toMap()
                val notificationManager = NotificationManagerCompat.from(context)

                val progressEnabled = loadData("subscription_checking_notifications", context) ?: true
                val progressNotification = if (progressEnabled) getProgressNotification(
                    context,
                    subscriptions.size
                ) else null
                if (progressNotification != null) {
                    notificationManager.notify(progressNotificationId, progressNotification.build())
                }

                fun progress(progress: Int, parser: String, media: String) {
                    if (progressNotification != null)
                        notificationManager.notify(
                            progressNotificationId,
                            progressNotification
                                .setProgress(subscriptions.size, progress, false)
                                .setContentText("$media on $parser")
                                .build()
                        )
                }

                subscriptions.toList().map {
                    val media = it.second.printIt("sub : ")
                    val text = if (media.isAnime) {
                        val parser = SubscriptionHelper.getAnimeParser(context, media.isAdult, media.id)
                        progress(index[it.first]!!, parser.name, media.name)
                        val ep: Episode? = SubscriptionHelper.getEpisode(context, parser, media.id, media.isAdult)
                        if (ep != null) "Episode ${ep.number}${
                            if (ep.title != null) " : ${ep.title}" else ""
                        }${
                            if (ep.isFiller) " [Filler]" else ""
                        } - just got released!" to ep.thumbnail
                        else null
                    } else {
                        val parser = SubscriptionHelper.getMangaParser(context, media.isAdult, media.id)
                        progress(index[it.first]!!, parser.name, media.name)
                        val ep: MangaChapter? =
                            SubscriptionHelper.getChapter(context, parser, media.id, media.isAdult)
                        if (ep != null) "Chapter ${ep.number}${
                            if (ep.title != null) " : ${ep.title}" else ""
                        } - just got released!" to null
                        else null
                    } ?: return@map
                    createNotification(context.applicationContext, media, text.first, text.second)
                }

                if (progressNotification != null) notificationManager.cancel(progressNotificationId)
                currentlyPerforming = false
            }
        }

        fun getChannelId(isAnime: Boolean, mediaId: Int) = "${if (isAnime) "anime" else "manga"}_${mediaId}"

        private suspend fun createNotification(
            context: Context,
            media: SubscriptionHelper.Companion.SubscribeMedia,
            text: String,
            thumbnail: FileUrl?
        ) {
            val notificationManager = NotificationManagerCompat.from(context)

            val notification = Notifications.getNotification(
                context,
                if (media.isAnime) Notifications.Group.ANIME_GROUP else Notifications.Group.MANGA_GROUP,
                getChannelId(media.isAnime, media.id),
                media.name,
                text,
                media.image,
                false,
                thumbnail
            ).setContentIntent(Notifications.getIntent(context, media.id)).build()

            notification.flags = Notification.FLAG_AUTO_CANCEL
            //+100 to have extra ids for other notifications?
            notificationManager.notify(100 + media.id, notification)
        }

        private const val progressNotificationId = 100

        private fun getProgressNotification(context: Context, size: Int): NotificationCompat.Builder {
            return Notifications.getNotification(
                context,
                null,
                "subscription_checking",
                "Checking Subscriptions",
                null,
                true
            ).setOngoing(true).setProgress(size, 0, false).setAutoCancel(false)
        }
    }
}