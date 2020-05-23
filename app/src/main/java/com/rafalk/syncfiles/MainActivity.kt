package com.rafalk.syncfiles

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.*
import timber.log.Timber

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var appBarConfiguration: AppBarConfiguration

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

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val addAccountButton: Button = findViewById(R.id.add_account_button)
        addAccountButton.setOnClickListener { view ->
            Snackbar.make(view, "Elo", Snackbar.LENGTH_SHORT).show()
            requestSignInToGoogleAccount()
        }

        val addDriveDirButton: Button = findViewById(R.id.add_remote_dir_button)
        addDriveDirButton.setOnClickListener {
            run {
                val intent = Intent(this, PickerActivity::class.java)
                intent.putExtra("PICKER_TYPE", "drive")
                startActivityForResult(intent, GET_DRIVE_DIR_PATH)
            }
        }

        val addLocalDirButton = findViewById<Button>(R.id.add_local_dir_button)
        addLocalDirButton.setOnClickListener {
            run {
                val intent = Intent(this, PickerActivity::class.java)
                intent.putExtra("PICKER_TYPE", "local")
                startActivityForResult(intent, GET_LOCAL_DIR_PATH)
            }
        }

        val syncButton = findViewById<Button>(R.id.sync_button)
        syncButton.setOnClickListener {
            run {
                val driveDirId = "1UIvISGnOktQN9dTmq5-ab-JfKBGvO4sl"
                val localDir = "/storage/emulated/0/Download/test"
                launch(Dispatchers.Default) {
                    SyncDirs(driveDirId, localDir, getGoogleDriveService())
                }.invokeOnCompletion { Timber.d("Finished syncing") }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
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
                    Timber.d("Received ${result.getStringExtra("path")}")
                    val remoteDirText = findViewById<EditText>(R.id.remote_dir_text)
                    remoteDirText.setText(result.getStringExtra("path"))
                }
            }
            GET_LOCAL_DIR_PATH -> {
                if (resultCode == Activity.RESULT_OK && result != null) {
                    Timber.d("Received ${result.getStringExtra("path")}")
                    val remoteDirText = findViewById<EditText>(R.id.local_dir_text)
                    remoteDirText.setText(result.getStringExtra("path"))
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, result)
    }

    private fun requestSignInToGoogleAccount() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
        Timber.d("Client received")
        startActivityForResult(googleSignInClient.signInIntent, REQUEST_SIGN_IN)
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
}
