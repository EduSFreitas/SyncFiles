package com.rafalk.syncfiles

import android.webkit.MimeTypeMap
import com.google.api.client.http.FileContent
import com.google.api.client.util.DateTime
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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
        val common = HashSet<String>()
        for (entry in mapOfDriveFiles) {
            if (!mapOfLocalFiles.containsKey(entry.key)) {
                Timber.d("Local ${entry.key} is missing")
                downloadFile(entry.key, entry.value)
            } else {
                common.add(entry.key)
            }
        }

        for (entry in mapOfLocalFiles) {
            if (!mapOfDriveFiles.containsKey(entry.key)) {
                Timber.d("Remote ${entry.key} is missing")
                uploadFile(entry.key, entry.value)
            } else {
                common.add(entry.key)
            }
        }

        for (name in common) {
            val localLastModified = mapOfLocalFiles.getValue(name).lastModified()
            val remoteLastModified = mapOfDriveFiles.getValue(name).modifiedTime.value
            when {
                localLastModified > remoteLastModified -> {
                    Timber.d("Local file ${name} is newer")
                    updateFile(mapOfLocalFiles.getValue(name), mapOfDriveFiles.getValue(name).id)
                }
                localLastModified < remoteLastModified -> {
                    Timber.d("Drive file ${name} is newer")
                    downloadFile(name, mapOfDriveFiles.getValue(name))
                }
                else -> {
                    Timber.d("File ${name} is up to date")
                }
            }
        }
    }

    private fun updateFile(file: File, id: String) {
        val driveFile = com.google.api.services.drive.model.File()
        val driveContent = FileContent(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
            file
        )
        driveFile.modifiedTime = DateTime(file.lastModified())
        launch(Dispatchers.Default) {
            googleDriveService.files().update(id, driveFile, driveContent).execute()
            Timber.d("${file.name} updated")
        }
    }

    private fun uploadFile(name: String, file: File) {
        val driveFile = com.google.api.services.drive.model.File()
        val driveContent = FileContent(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
            file
        )
        driveFile.name = name
        driveFile.parents = mutableListOf(driveDirId)
        driveFile.modifiedTime = DateTime(file.lastModified())
        launch(Dispatchers.Default) {
            googleDriveService.files().create(driveFile, driveContent).execute()
            Timber.d("${file.name} uploaded")
        }
    }

    private fun downloadFile(name: String, file: com.google.api.services.drive.model.File) =
        launch(Dispatchers.Default) {
            val localFile = File(localDir + '/' + name)
            val localOutputStream = FileOutputStream(localFile)
            googleDriveService.files().get(file.id)
                .executeMediaAndDownloadTo(localOutputStream)
            localOutputStream.flush()
            localOutputStream.close()
            localFile.setLastModified(file.modifiedTime.value)

            Timber.d("${localFile.absolutePath} saved to local storage ${DateTime(localFile.lastModified())}")
        }

    private fun getMapOfLocalFiles(localDir: String): Map<String, File> {
        val local = File(localDir)
        val localFiles = local.listFiles { file ->
            file.isFile
        }
        Timber.d(localFiles.contentToString())
        return localFiles.map { it.name to it }.toMap()
    }
}
