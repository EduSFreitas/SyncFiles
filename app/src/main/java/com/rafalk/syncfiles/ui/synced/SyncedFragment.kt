package com.rafalk.syncfiles.ui.synced

import android.app.NotificationChannel.DEFAULT_CHANNEL_ID
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.rafalk.syncfiles.R
import com.rafalk.syncfiles.SyncDirs
import com.rafalk.syncfiles.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber


class SyncedFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var listener: SyncedFragmentListener
    private lateinit var syncedViewModel: SyncedViewModel
    private lateinit var mContext: Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        syncedViewModel =
            ViewModelProviders.of(this).get(SyncedViewModel::class.java)

        val db = AppDatabase.getDatabase(mContext)

        val root = inflater.inflate(R.layout.fragment_synced, container, false)

        val syncButton = root.findViewById<Button>(R.id.sync_button)
        syncButton.setOnClickListener { view ->
            view.isEnabled = false
            launch(Dispatchers.Default) {
                val dirs = db.dirsPairDao().getAll()
                for (pair in dirs) {
                    SyncDirs(pair.remoteDirId, pair.localDir, getGoogleDriveService())
                }
            }.invokeOnCompletion {
                Timber.d("Finished syncing")
                launch(Dispatchers.Main) {
                    view.isEnabled = true
                    showNotification()
                }
            }
        }


        root.findViewById<Switch>(R.id.enable_auto_sync_toggle)
            .setOnCheckedChangeListener { compoundButton, checked ->
                if (checked) {
                    val intervalPickerDialog = IntervalPickerDialog()
                    intervalPickerDialog.show(fragmentManager, "IntervalPickerDialog")
                } else {
                    listener.onCancelAutoSync()
                }

            }

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context

        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as SyncedFragmentListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                (context.toString() +
                        " must implement SyncedFragmentListener")
            )
        }
    }

    private fun getGoogleDriveService(): Drive {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(mContext)

        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            mContext, listOf(DriveScopes.DRIVE_FILE)
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
            mContext,
            DEFAULT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.icon_sync)
            .setContentTitle("SyncFiles")
            .setContentText("Successfully synced directories.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setSound(alarmSound)
        val notificationManager =
            NotificationManagerCompat.from(mContext)
        notificationManager.notify(0, mBuilder.build())
    }

    interface SyncedFragmentListener {
        fun onCancelAutoSync()
    }
}
