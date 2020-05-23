package com.rafalk.syncfiles

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var remoteDir = MutableLiveData<String>()
    var remoteDirId = MutableLiveData<String>()
    var localDir = MutableLiveData<String>()
}