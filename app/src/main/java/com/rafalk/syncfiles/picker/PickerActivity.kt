package com.rafalk.syncfiles.picker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rafalk.syncfiles.R
import kotlinx.android.synthetic.main.activity_picker.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import timber.log.Timber
import java.io.File


class PickerActivity : AppCompatActivity(),
    FilesListFragment.OnListFragmentInteractionListener,
    FilesListFragment.OnDriveListFragmentInteractionListener,
    CoroutineScope by MainScope() {

    lateinit var currentDirectory: File
    lateinit var currentDriveDirectory: DriveFilesAdapter.DriveItem

    companion object {
        const val REQUEST_SIGN_IN = 1
        private const val REQUEST_WRITE_STORAGE_REQUEST_CODE = 2
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)

        Timber.d("onActivityResult=$requestCode")
        when (requestCode) {
            REQUEST_SIGN_IN -> {
                if (resultCode == RESULT_OK && result != null) {
                    Timber.d("Signin successful")
                } else {
                    Timber.d("Signin request failed")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestAppPermissions()

        setContentView(R.layout.activity_picker)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            val resultIntent = Intent()
            Timber.d("Returning ${getPath()}")
            resultIntent.putExtra("path", getPath())
            if (intent.getStringExtra("PICKER_TYPE") == "drive") {
                resultIntent.putExtra("id", currentDriveDirectory.file.id)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

    }

    private fun getPath(): String? {
        when (intent.getStringExtra("PICKER_TYPE")) {
            "drive" -> return getDrivePath(currentDriveDirectory)
            "local" -> return currentDirectory.absolutePath
        }
        return null
    }

    private fun getDrivePath(directory: DriveFilesAdapter.DriveItem): String {
        if (directory.parent == null) {
            return "/"
        }
        return getDrivePath(directory.parent) + directory.file.name + '/'
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        setResult(Activity.RESULT_CANCELED, Intent())
        finish()
    }

    override fun onListFragmentInteraction(item: SystemFilesAdapter.FileItem?) {
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
            ),
            REQUEST_WRITE_STORAGE_REQUEST_CODE
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

    override fun onDriveListFragmentInteraction(item: DriveFilesAdapter.DriveItem?) {
        Timber.d("Clicked ${item?.content}")
        if (item != null) {
            currentDriveDirectory = item
        }
    }
}
