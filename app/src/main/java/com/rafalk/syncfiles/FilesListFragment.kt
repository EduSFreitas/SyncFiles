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
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import timber.log.Timber


/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [FilesListFragment.OnListFragmentInteractionListener] interface.
 */
class FilesListFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var googleDriveService: Drive


    private var listener: OnListFragmentInteractionListener? = null
    private var driveListener: OnDriveListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_files_list, container, false)
        getGoogleDriveService()

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                adapter = getRequestedAdapter()
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

    private fun getRequestedAdapter(): RecyclerView.Adapter<*>? {
        when(activity?.intent?.getStringExtra("PICKER_TYPE")){
            "drive" -> {
                return DriveFilesAdapter(driveListener, googleDriveService)
            }
            "local" -> {
                return SystemFilesAdapter(listener)
            }
        }
        return null
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
}
