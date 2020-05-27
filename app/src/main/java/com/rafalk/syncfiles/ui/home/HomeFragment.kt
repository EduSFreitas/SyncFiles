package com.rafalk.syncfiles.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.rafalk.syncfiles.MainActivity
import com.rafalk.syncfiles.MainActivity.Companion.GET_DRIVE_DIR_PATH
import com.rafalk.syncfiles.MainActivity.Companion.GET_LOCAL_DIR_PATH
import com.rafalk.syncfiles.MainViewModel
import com.rafalk.syncfiles.R
import com.rafalk.syncfiles.database.AppDatabase
import com.rafalk.syncfiles.database.DirsPair
import com.rafalk.syncfiles.picker.PickerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var mContext: Context
    private lateinit var model: MainViewModel
    private lateinit var db: AppDatabase

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        model = ViewModelProviders.of(this).get(MainViewModel::class.java)
        db = AppDatabase.getDatabase(mContext)

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val addDriveDirButton: Button = root.findViewById(R.id.add_remote_dir_button)
        addDriveDirButton.setOnClickListener {
            val intent = Intent(activity, PickerActivity::class.java)
            intent.putExtra("PICKER_TYPE", "drive")
            startActivityForResult(intent, MainActivity.GET_DRIVE_DIR_PATH)
        }

        val addLocalDirButton = root.findViewById<Button>(R.id.add_local_dir_button)
        addLocalDirButton.setOnClickListener {
            val intent = Intent(activity, PickerActivity::class.java)
            intent.putExtra("PICKER_TYPE", "local")
            startActivityForResult(intent, MainActivity.GET_LOCAL_DIR_PATH)
        }

        val addDirsPairButton = root.findViewById<Button>(R.id.add_pair)
        addDirsPairButton.setOnClickListener {
            launch(Dispatchers.Default) {
                if (model.localDir.value != null && model.remoteDir.value != null && model.remoteDirId.value != null) {
                    if (db.dirsPairDao()
                            .count(model.localDir.value!!, model.remoteDirId.value!!) == 0
                    ) {
                        db.dirsPairDao().insertAll(
                            DirsPair(
                                model.localDir.value!!,
                                model.remoteDir.value!!,
                                model.remoteDirId.value!!
                            )
                        )
                    }
                }
            }.invokeOnCompletion {
                model.localDir.postValue(null)
                model.remoteDir.postValue(null)
                model.remoteDirId.postValue(null)
            }
        }

        val remoteDirText = root.findViewById<EditText>(R.id.remote_dir_text)
        model.remoteDir.observe(this, Observer { remoteDirText.setText(it) })

        val localDirText = root.findViewById<EditText>(R.id.local_dir_text)
        model.localDir.observe(this, Observer { localDirText.setText(it) })

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        Timber.d("onActivityResult=$requestCode")
        when (requestCode) {
            GET_DRIVE_DIR_PATH -> {
                if (resultCode == Activity.RESULT_OK && result != null) {
                    val path = result.getStringExtra("path")
                    val id = result.getStringExtra("id")

                    model.remoteDir.apply { value = path }
                    model.remoteDirId.apply { value = id }

                    Timber.d("Received $path")
                    Timber.d("and id $id")
                }
            }
            GET_LOCAL_DIR_PATH -> {
                if (resultCode == Activity.RESULT_OK && result != null) {
                    val path = result.getStringExtra("path")

                    model.localDir.apply { value = path }

                    Timber.d("Received $path")
                }
            }
        }
    }
}
