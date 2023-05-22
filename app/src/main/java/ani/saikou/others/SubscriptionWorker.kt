package ani.saikou.others

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import ani.saikou.others.Notifications.Companion.getIntent
import ani.saikou.others.Notifications.Group.ANIME_GROUP
import ani.saikou.others.Notifications.Group.MANGA_GROUP
import ani.saikou.others.SubscriptionHelper.Companion.getSubscriptions
import ani.saikou.parsers.Episode
import ani.saikou.parsers.MangaChapter
import java.util.concurrent.*

class SubscriptionWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {

        getSubscriptions().forEach {
            val media = it.value
            val text = if (media.isAnime) {
                val ep: Episode? = SubscriptionHelper.getEpisode(media.isAdult, media.id)
                if (ep != null) "Episode ${ep.number}${
                    if (ep.title != null) " : ${ep.title}" else ""
                } ${
                    if (ep.isFiller) " [Filler]" else ""
                } - just got released!"
                else return@forEach
            } else {
                val ep: MangaChapter? = SubscriptionHelper.getChapter(media.isAdult, media.id)
                if (ep != null) "Chapter ${ep.number}${
                    if (ep.title != null) " : ${ep.title}" else ""
                } - just got released!"
                else return@forEach
            }
            createNotification(applicationContext,media,text)
        }

        return Result.success()
    }

    companion object {
        private const val SUBSCRIPTION_WORK_NAME = "work_subscription"
        private var alreadyCreated = false
        fun enqueue(context: Context) {
            if (!alreadyCreated) {
                val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

                val periodicSyncDataWork = PeriodicWorkRequest.Builder(
                    SubscriptionWorker::class.java, 30, TimeUnit.MINUTES
                ).apply {
                    addTag(SUBSCRIPTION_WORK_NAME)
                    setConstraints(constraints)
                }.build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    SUBSCRIPTION_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, periodicSyncDataWork
                )

                // Testing
                val oneTimeSyncDataWork =
                    OneTimeWorkRequest.Builder(SubscriptionWorker::class.java).addTag(SUBSCRIPTION_WORK_NAME)
                        .setConstraints(constraints).build()

                WorkManager.getInstance(context).enqueue(oneTimeSyncDataWork)

                alreadyCreated = true
            }
        }

        fun getChannelId(isAnime:Boolean,mediaId: Int) = "${if (isAnime) "anime" else "manga"}_${mediaId}"

        suspend fun createNotification(context: Context, media: SubscriptionHelper.Companion.SubscribeMedia, text: String) {
            val notificationManager = NotificationManagerCompat.from(context)

            val notification = Notifications.getNotification(
                context,
                if (media.isAnime) ANIME_GROUP else MANGA_GROUP,
                getChannelId(media.isAnime,media.id),
                media.name,
                text,
                media.image
            ).setContentIntent(getIntent(context, media.id)).build()

            //+100 to have extra ids for other notifications?
            notificationManager.notify(100 + media.id, notification)
        }
    }
}