package ani.saikou.others

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ani.saikou.currActivity
import ani.saikou.tryWith

class AutoStartServiceOnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> tryWith(true) {
                SubscriptionWorker.enqueue(context?: currActivity() ?: throw Exception("Failed to start Saikou Source Subscription"))
            }
        }
    }
}