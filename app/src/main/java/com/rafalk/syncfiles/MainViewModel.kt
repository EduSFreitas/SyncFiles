package com.rafalk.syncfiles

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var remoteDir = MutableLiveData<String>().apply { value = "" }
    var remoteDirId = MutableLiveData<String>().apply { value = "" }
    var localDir = MutableLiveData<String>().apply { value = "" }
}