package com.rafalk.syncfiles.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DirsPair(
    @ColumnInfo(name = "local_dir") val localDir: String,
    @ColumnInfo(name = "remote_dir") val remoteDir: String,
    @ColumnInfo(name = "remote_dir_id") val remoteDirId: String
){
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0
        set(value) {
            field = value
        }
}