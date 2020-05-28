package com.rafalk.syncfiles.autosync

import android.app.IntentService
import android.app.NotificationChannel
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.rafalk.syncfiles.R
import com.rafalk.syncfiles.SyncDirs
import com.rafalk.syncfiles.database.AppDatabase
import timber.log.Timber

private const val ACTION_SYNC = "com.rafalk.syncfiles.autosync.action.SYNC"


/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
class AutoSyncIntentService : IntentService("AutoSyncIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SYNC -> {
                Timber.d("Handling action sync in service")
                handleActionSync()
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionSync() {
        val db = AppDatabase.getDatabase(applicationContext)
        val dirs = db.dirsPairDao().getAll()
        for (pair in dirs) {
            SyncDirs(pair.remoteDirId, pair.localDir, getGoogleDriveService())
        }
        Timber.d("Finished syncing")
        showNotification()
    }

    private fun getGoogleDriveService(): Drive {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(applicationContext)

        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE)
        )
        credential.selectedAccount = googleAccount!!.account
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(getString(R.string.app_name))
            .build()
    }

    private fun showNotification() {
        Timber.d("Attempting to show notification")
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(
            applicationContext,
            NotificationChannel.DEFAULT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.icon_sync)
            .setContentTitle("SyncFiles")
            .setContentText("Successfully synced directories.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setSound(alarmSound)
        val notificationManager =
            NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(0, mBuilder.build())
    }

    companion object {
        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun startActionSync(context: Context) {
            val intent = Intent(context, AutoSyncIntentService::class.java).apply {
                action = ACTION_SYNC
            }
            context.startService(intent)
        }
    }
}
