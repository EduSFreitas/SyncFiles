package com.rafalk.syncfiles

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.rafalk.syncfiles.dummy.DummyContent
import kotlinx.android.synthetic.main.activity_system_files.*
import timber.log.Timber

class SystemFilesActivity : AppCompatActivity(), SystemFilesListFragment.OnListFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree());
        setContentView(R.layout.activity_system_files)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onListFragmentInteraction(item: DummyContent.DummyItem?) {
//        TODO("Not yet implemented")
        Timber.d("Clicked ${item?.id}")
    }

}
