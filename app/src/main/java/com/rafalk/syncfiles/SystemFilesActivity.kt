package com.rafalk.syncfiles

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_system_files.*
import timber.log.Timber
import java.io.File


class SystemFilesActivity : AppCompatActivity(), SystemFilesListFragment.OnListFragmentInteractionListener {
    private val REQUEST_WRITE_STORAGE_REQUEST_CODE = 2
    lateinit var currentDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        requestAppPermissions();

        setContentView(R.layout.activity_system_files)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, currentDirectory.absolutePath, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

    }

    override fun onListFragmentInteraction(item: SystemFilesViewAdapter.FileItem?) {
        Timber.d("Clicked ${item?.file?.absolutePath}")
        currentDirectory = item?.file!!
    }

    private fun requestAppPermissions() {
        if (hasReadPermissions() && hasWritePermissions()) {
            return
        }
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), REQUEST_WRITE_STORAGE_REQUEST_CODE
        ) // your request code
    }

    private fun hasReadPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasWritePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
