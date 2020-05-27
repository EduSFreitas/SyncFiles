package com.rafalk.syncfiles.autosync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class AutoSyncReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("Received broadcast!")
        context?.let { AutoSyncIntentService.startActionSync(it) }
    }
}