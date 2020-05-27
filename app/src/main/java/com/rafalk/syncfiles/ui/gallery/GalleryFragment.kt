package com.rafalk.syncfiles.ui.gallery

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.rafalk.syncfiles.MainActivity
import com.rafalk.syncfiles.R
import com.rafalk.syncfiles.SyncDirs
import com.rafalk.syncfiles.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class GalleryFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var galleryViewModel: GalleryViewModel
    private lateinit var mContext: Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        galleryViewModel =
            ViewModelProviders.of(this).get(GalleryViewModel::class.java)

        val db = AppDatabase.getDatabase(mContext)

        val root = inflater.inflate(R.layout.fragment_gallery, container, false)

        val syncButton = root.findViewById<Button>(R.id.sync_button)
        syncButton.setOnClickListener {
            launch(Dispatchers.Default) {
                val dirs = db.dirsPairDao().getAll()
                for (pair in dirs) {
                    SyncDirs(pair.remoteDirId, pair.localDir, getGoogleDriveService())
                }
            }.invokeOnCompletion { Timber.d("Finished syncing") }
        }

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
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
}
