package com.rafalk.syncfiles

import android.webkit.MimeTypeMap
import com.google.api.client.http.FileContent
import com.google.api.client.util.DateTime
import com.google.api.services.drive.Drive
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class SyncDirs(
    private val driveDirId: String,
    private val localDir: String,
    private val googleDriveService: Drive
) {

    private var mapOfLocalFiles: Map<String, File>

    private var mapOfLocalDirs: Map<String, File>

    init {
        Timber.d("Directories to sync $driveDirId and $localDir")
        mapOfLocalFiles = getMapOfLocalFiles()
        getMapOfDriveFiles()

        mapOfLocalDirs = getMapOfLocalDirs()
        getMapOfDriveDirs()
    }

    private fun getMapOfDriveDirs() {
        val driveDirs = googleDriveService
            .files().list()
            .setSpaces("drive")
            .setQ("'${driveDirId}' in parents and trashed=false and mimeType = 'application/vnd.google-apps.folder'")
            .setFields("nextPageToken, files(id, name)")
            .setPageToken(null)
            .execute()
        Timber.d("Result received $driveDirs")

        syncDirsTree(driveDirs.files.map { it.name to it }.toMap())
    }

    private fun syncDirsTree(mapOfDriveDirs: Map<String, com.google.api.services.drive.model.File>) {
        //create remote dirs
        for (entry in mapOfLocalDirs) {
            if (!mapOfDriveDirs.containsKey(entry.key)) {
                val dirMetadata = com.google.api.services.drive.model.File()
                dirMetadata.name = entry.key
                dirMetadata.mimeType = "application/vnd.google-apps.folder"
                dirMetadata.parents = mutableListOf(driveDirId)
                val dir: com.google.api.services.drive.model.File =
                    googleDriveService.files().create(dirMetadata)
                        .setFields("id")
                        .execute()
                Timber.d("Created remote dir ${entry.key}")
                SyncDirs(dir.id, localDir + '/' + entry.key, googleDriveService)
            } else {
                SyncDirs(
                    mapOfDriveDirs.getValue(entry.key).id,
                    localDir + '/' + entry.key,
                    googleDriveService
                )
            }
        }

        //create local dirs
        for (entry in mapOfDriveDirs) {
            if (!mapOfLocalDirs.containsKey(entry.key)) {
                val newLocalDir = File(localDir + '/' + entry.key)
                newLocalDir.mkdir()
                Timber.d("Created local dir ${newLocalDir.absolutePath}")
                SyncDirs(entry.value.id, newLocalDir.absolutePath, googleDriveService)
            }
        }
    }

    private fun getMapOfLocalDirs(): Map<String, File> {
        val local = File(localDir)
        val localDirs = local.listFiles { file ->
            file.isDirectory
        }
        Timber.d("Dirs in ${local.absolutePath}: ${localDirs.contentToString()}")
        return localDirs.map { it.name to it }.toMap()
    }

    fun getMapOfDriveFiles() {
        val driveFiles = googleDriveService
            .files().list()
            .setSpaces("drive")
            .setQ("'${driveDirId}' in parents and trashed=false and mimeType != 'application/vnd.google-apps.folder'")
            .setFields("nextPageToken, files(id, name, mimeType, modifiedTime)")
            .setPageToken(null)
            .execute()
        Timber.d("Result received $driveFiles")

        syncFilesDiff(driveFiles.files.map { it.name to it }.toMap())
    }

    private fun syncFilesDiff(mapOfDriveFiles: Map<String, com.google.api.services.drive.model.File>) {
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
        googleDriveService.files().update(id, driveFile, driveContent).execute()
        Timber.d("${file.name} updated")
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
        googleDriveService.files().create(driveFile, driveContent).execute()
        Timber.d("${file.name} uploaded")
    }

    private fun downloadFile(name: String, file: com.google.api.services.drive.model.File) {
        val localFile = File(localDir + '/' + name)
        val localOutputStream = FileOutputStream(localFile)
        googleDriveService.files().get(file.id)
            .executeMediaAndDownloadTo(localOutputStream)
        localOutputStream.flush()
        localOutputStream.close()
        localFile.setLastModified(file.modifiedTime.value)

        Timber.d("${localFile.absolutePath} saved to local storage ${DateTime(localFile.lastModified())}")
    }

    private fun getMapOfLocalFiles(): Map<String, File> {
        val local = File(localDir)
        val localFiles = local.listFiles { file ->
            file.isFile
        }
        Timber.d(localFiles.contentToString())
        return localFiles.map { it.name to it }.toMap()
    }
}
