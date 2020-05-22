package com.rafalk.syncfiles

import android.webkit.MimeTypeMap
import com.google.api.client.http.FileContent
import com.google.api.client.util.DateTime
import com.google.api.services.drive.Drive
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class SyncDirs(
    private val driveDirId: String,
    private val localDir: String,
    private val googleDriveService: Drive
) :
    CoroutineScope by MainScope() {

    private var mapOfLocalFiles: Map<String, File>
    private lateinit var mapOfDriveFiles: Map<String, com.google.api.services.drive.model.File>

    init {
        Timber.d("Directories to sync $driveDirId and $localDir")
        mapOfLocalFiles = getMapOfLocalFiles(localDir)
        getMapOfDriveFiles(driveDirId)
    }

    fun getMapOfDriveFiles(driveDirId: String) = launch(Dispatchers.Default) {
        val driveFiles = googleDriveService
            .files().list()
            .setSpaces("drive")
            .setQ("'${driveDirId}' in parents and trashed=false and mimeType != 'application/vnd.google-apps.folder'")
            .setFields("nextPageToken, files(id, name, mimeType, modifiedTime)")
            .setPageToken(null)
            .execute()
        Timber.d("Result received $driveFiles")

        launch(Dispatchers.Main) {
            mapOfDriveFiles = driveFiles.files.map { it.name to it }.toMap()
            showDifference()
        }
    }

    private fun showDifference() {
        val common = ArrayList<String>()
        for (entry in mapOfDriveFiles) {
            if (!mapOfLocalFiles.containsKey(entry.key)) {
                Timber.d("Local ${entry.key} is missing")
                downloadFile(entry.key, entry.value)
            } else{
                common.add(entry.key)
            }
        }

        for (entry in mapOfLocalFiles) {
            if (!mapOfDriveFiles.containsKey(entry.key)) {
                Timber.d("Remote ${entry.key} is missing")
            } else{
                common.add(entry.key)
            }
        }
    }

    private fun downloadFile(name: String, file: com.google.api.services.drive.model.File) =
        launch(Dispatchers.Default) {
            val localFile = File(localDir + '/' + name)
            localFile.setLastModified(file.modifiedTime.value)
            if (!localFile.exists()) {
                localFile.createNewFile()
            }

            val localOutputStream = FileOutputStream(localFile)
            googleDriveService.files().get(file.id)
                .executeMediaAndDownloadTo(localOutputStream)
            localOutputStream.flush()
            localOutputStream.close()

            Timber.d("${localFile.absolutePath} saved to local storage")
        }

    private fun getMapOfLocalFiles(localDir: String): Map<String, File> {
        val local = File(localDir)
        val localFiles = local.listFiles { file ->
            file.isFile
        }
        Timber.d(localFiles.toString())
        return localFiles.map { it.name to it }.toMap()
    }

    private fun local() {
        val local = File(localDir)
        val localFiles = local.listFiles { file ->
            file.isFile
        }
        for (file in localFiles) {
            Timber.d("Local ${file.name}")
            val driveFile = com.google.api.services.drive.model.File()
            val driveContent = FileContent(
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
                file
            )
            driveFile.name = file.name
            driveFile.parents = mutableListOf(driveDirId)
            driveFile.modifiedTime = DateTime(file.lastModified())
            launch(Dispatchers.Default) {
                googleDriveService.files().create(driveFile, driveContent).execute()
                Timber.d("${file.name} uploaded")
            }

        }
    }

    private fun drive() {
        launch(Dispatchers.Default) {
            val driveFiles = googleDriveService
                .files().list()
                .setSpaces("drive")
                .setQ("'${driveDirId}' in parents and trashed=false")
                .setFields("nextPageToken, files(id, name, mimeType, modifiedTime)")
                .setPageToken(null)
                .execute()
            Timber.d("Result received $driveFiles")
            for (file in driveFiles.files) {
                val localFile = File(localDir + '/' + file.name)
                localFile.setLastModified(file.modifiedTime.value)
                if (!localFile.exists()) {
                    localFile.createNewFile()
                }

                val localOutputStream = FileOutputStream(localFile)
                googleDriveService.files().get(file.id)
                    .executeMediaAndDownloadTo(localOutputStream)
                localOutputStream.flush()
                localOutputStream.close()

                Timber.d("${localFile.absolutePath} saved to local storage")
            }
        }
    }
}
