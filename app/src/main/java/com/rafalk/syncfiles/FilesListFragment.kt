package com.rafalk.syncfiles

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [FilesListFragment.OnListFragmentInteractionListener] interface.
 */
class FilesListFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var googleDriveService: Drive

    // TODO: Customize parameters
    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null
    private var driveListener: OnDriveListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_files_list, container, false)
        getGoogleDriveService()
//        listSomeFiles()

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                adapter = DriveFilesAdapter(driveListener, googleDriveService)
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(
                    DividerItemDecoration(view.getContext(),
                        DividerItemDecoration.VERTICAL
                    )
                )
            }
        }
        return view
    }

    private fun getGoogleDriveService() {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)

        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = googleAccount!!.account
        googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(getString(R.string.app_name))
            .build()
    }

    private fun listSomeFiles() {
        launch(Dispatchers.Default) {
            try {
                val result = googleDriveService
                    .files().list()
                    .setSpaces("drive")
                    .setQ("'root' in parents")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(null)
                    .execute()
                Timber.d("Result received $result")
                for (file in result.files) {
                    Timber.d("name=${file.name}, id=${file.id}")
                }
            } catch (e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, PickerActivity.REQUEST_SIGN_IN);
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        }
        if(context is OnDriveListFragmentInteractionListener){
            driveListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        driveListener = null
    }


    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: SystemFilesAdapter.FileItem?)
    }

    interface OnDriveListFragmentInteractionListener {
        fun onDriveListFragmentInteraction(item: DriveFilesAdapter.DriveItem?)
    }

    companion object {

        const val ARG_COLUMN_COUNT = "column-count"

    }
}
