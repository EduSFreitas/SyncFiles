package com.rafalk.syncfiles

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.rafalk.syncfiles.database.AppDatabase
import com.rafalk.syncfiles.database.DirsPair
import kotlinx.coroutines.*
import timber.log.Timber

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    PairsListFragment.OnListFragmentInteractionListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var model: MainViewModel
    private lateinit var db: AppDatabase

    companion object {
        private const val REQUEST_SIGN_IN = 1
        private const val GET_DRIVE_DIR_PATH = 2
        private const val GET_LOCAL_DIR_PATH = 3
    }


    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setup logger
        Timber.plant(Timber.DebugTree());

        // setup database
        db = AppDatabase.getDatabase(this)

        // get model
        model = ViewModelProviders.of(this).get(MainViewModel::class.java)


        // google sign in
        requestSignInToGoogleAccount()

        setContentView(R.layout.activity_main)
//        val toolbar: Toolbar = findViewById(R.id.toolbar)
//        setSupportActionBar(toolbar)

//        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
//        val navView: NavigationView = findViewById(R.id.nav_view)
//        val navController = findNavController(R.id.nav_host_fragment)
//        // Passing each menu ID as a set of Ids because each
//        // menu should be considered as top level destinations.
//        appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
//            ), drawerLayout
//        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        navView.setupWithNavController(navController)

        val addDriveDirButton: Button = findViewById(R.id.add_remote_dir_button)
        addDriveDirButton.setOnClickListener {
            val intent = Intent(this, PickerActivity::class.java)
            intent.putExtra("PICKER_TYPE", "drive")
            startActivityForResult(intent, GET_DRIVE_DIR_PATH)
        }

        val addLocalDirButton = findViewById<Button>(R.id.add_local_dir_button)
        addLocalDirButton.setOnClickListener {
            val intent = Intent(this, PickerActivity::class.java)
            intent.putExtra("PICKER_TYPE", "local")
            startActivityForResult(intent, GET_LOCAL_DIR_PATH)
        }

        val syncButton = findViewById<Button>(R.id.sync_button)
        syncButton.setOnClickListener {
            launch(Dispatchers.Default) {
                val dirs =  db.dirsPairDao().getAll()
                for (pair in dirs) {
                    SyncDirs(pair.remoteDirId, pair.localDir, getGoogleDriveService())
                }
            }.invokeOnCompletion { Timber.d("Finished syncing") }
        }

        val addDirsPairButton = findViewById<Button>(R.id.add_pair)
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
            }
        }

        val remoteDirText = findViewById<EditText>(R.id.remote_dir_text)
        model.remoteDir.observe(this, Observer { remoteDirText.setText(it) })

        val localDirText = findViewById<EditText>(R.id.local_dir_text)
        model.localDir.observe(this, Observer { localDirText.setText(it) })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment)
//        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
//    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        Timber.d("onActivityResult=$requestCode")
        when (requestCode) {
            REQUEST_SIGN_IN -> {
                if (resultCode == RESULT_OK && result != null) {
                    Timber.d("Signin successful")
                } else {
                    Timber.d("Signin request failed")
                }
            }
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

    private fun requestSignInToGoogleAccount() {
        if (GoogleSignIn.getLastSignedInAccount(this) == null) {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Scope(DriveScopes.DRIVE))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
            Timber.d("Client received")
            startActivityForResult(googleSignInClient.signInIntent, REQUEST_SIGN_IN)
        }
    }

    private fun getGoogleDriveService(): Drive {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(this)

        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = googleAccount!!.account
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(getString(R.string.app_name))
            .build()
    }

    override fun onListFragmentInteraction(item: DirsPair?) {


    }
}
