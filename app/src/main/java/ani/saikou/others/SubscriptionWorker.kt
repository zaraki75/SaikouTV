package ani.saikou.others

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import ani.saikou.FileUrl
import ani.saikou.loadData
import ani.saikou.others.Notifications.Companion.getIntent
import ani.saikou.others.Notifications.Group.ANIME_GROUP
import ani.saikou.others.Notifications.Group.MANGA_GROUP
import ani.saikou.others.SubscriptionHelper.Companion.SubscribeMedia
import ani.saikou.others.SubscriptionHelper.Companion.getAnimeParser
import ani.saikou.others.SubscriptionHelper.Companion.getMangaParser
import ani.saikou.others.SubscriptionHelper.Companion.getSubscriptions
import ani.saikou.parsers.Episode
import ani.saikou.parsers.MangaChapter
import java.util.concurrent.*

class SubscriptionWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        val subscriptions = getSubscriptions()
        var i = 0
        val index = subscriptions.map { i++; it.key to i }.toMap()
        val notificationManager = NotificationManagerCompat.from(context)

        val progressNotificationId = 100

        val progressEnabled = loadData("subscription_checking_notifications") ?: false
        val progressNotification = if (progressEnabled) {
            Notifications.getNotification(
                applicationContext,
                null,
                "subscription_checking",
                "Checking Subscriptions",
                null,
                true
            ).setOngoing(true).setProgress(subscriptions.size, 0, false)
        } else null

        if (progressNotification != null) notificationManager.notify(progressNotificationId, progressNotification.build())

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

        subscriptions.forEach {
            val media = it.value
            val text = if (media.isAnime) {
                val parser = getAnimeParser(media.isAdult, media.id)
                progress(index[it.key]!!, parser.name, media.name)
                val ep: Episode? = SubscriptionHelper.getEpisode(parser, media.id, media.isAdult)
                if (ep != null) "Episode ${ep.number}${
                    if (ep.title != null) " : ${ep.title}" else ""
                }${
                    if (ep.isFiller) " [Filler]" else ""
                } - just got released!" to ep.thumbnail
                else return@forEach
            } else {
                val parser = getMangaParser(media.isAdult, media.id)
                progress(index[it.key]!!, parser.name, media.name)
                val ep: MangaChapter? = SubscriptionHelper.getChapter(parser, media.id, media.isAdult)
                if (ep != null) "Chapter ${ep.number}${
                    if (ep.title != null) " : ${ep.title}" else ""
                } - just got released!" to null
                else return@forEach
            }
            createNotification(applicationContext, media, text.first, text.second)
        }

        if (progressNotification != null) notificationManager.cancel(progressNotificationId)

        return Result.success()
    }

    companion object {

        const val defaultTime = 5
        val timeMinutes = arrayOf(15L, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, 1440)

        private const val SUBSCRIPTION_WORK_NAME = "work_subscription"
        private var alreadyCreated = false
        fun enqueue(context: Context, force: Boolean = false) {
            if (!alreadyCreated || force) {
                alreadyCreated = true
                val curTime = loadData<Int>("subscriptions_time") ?: defaultTime
                val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

                val periodicSyncDataWork = PeriodicWorkRequest.Builder(
                    SubscriptionWorker::class.java, timeMinutes[curTime], TimeUnit.MINUTES
                ).apply {
                    addTag(SUBSCRIPTION_WORK_NAME)
                    setConstraints(constraints)
                }.build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    SUBSCRIPTION_WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, periodicSyncDataWork
                )
            }
        }

//        fun workOnceTest(context: Context) {
//            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
//            val oneTimeSyncDataWork =
//                OneTimeWorkRequest
//                    .Builder(SubscriptionWorker::class.java)
//                    .addTag("TEST")
//                    .setConstraints(constraints).build()
//
//            WorkManager.getInstance(context).enqueue(oneTimeSyncDataWork)
//        }

        fun getChannelId(isAnime: Boolean, mediaId: Int) = "${if (isAnime) "anime" else "manga"}_${mediaId}"

        suspend fun createNotification(context: Context, media: SubscribeMedia, text: String, thumbnail: FileUrl?) {
            val notificationManager = NotificationManagerCompat.from(context)

            val notification = Notifications.getNotification(
                context,
                if (media.isAnime) ANIME_GROUP else MANGA_GROUP,
                getChannelId(media.isAnime, media.id),
                media.name,
                text,
                media.image,
                false,
                thumbnail
            ).setContentIntent(getIntent(context, media.id)).build()

            notification.flags = Notification.FLAG_AUTO_CANCE
            //+100 to have extra ids for other notifications?
            notificationManager.notify(100 + media.id, notification)
        }
    }
}