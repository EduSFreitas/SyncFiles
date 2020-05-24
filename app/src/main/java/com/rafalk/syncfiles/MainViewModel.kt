package com.rafalk.syncfiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.rafalk.syncfiles.database.AppDatabase
import com.rafalk.syncfiles.database.DirsPair

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var remoteDir = MutableLiveData<String>()
    var remoteDirId = MutableLiveData<String>()
    var localDir = MutableLiveData<String>()

    val allPairs : LiveData<List<DirsPair>>

    init {
        val dao = AppDatabase.getDatabase(application).dirsPairDao()
        allPairs = dao.getAll()
    }
}