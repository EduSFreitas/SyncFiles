package com.rafalk.syncfiles.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DirsPairDao {
    @Query("SELECT * FROM dirspair")
    fun getAll(): LiveData<List<DirsPair>>

    @Query("SELECT COUNT(*) from dirspair where local_dir=:localDir and remote_dir_id=:remoteDirId")
    fun count(localDir: String, remoteDirId: String): Int

    @Insert
    fun insertAll(vararg dirspairs: DirsPair)

    @Delete
    fun delete(dirspair: DirsPair)


}