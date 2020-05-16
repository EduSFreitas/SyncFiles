package com.rafalk.syncfiles

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.Button
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
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.*
import timber.log.Timber

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var googleDriveService: Drive
    private lateinit var appBarConfiguration: AppBarConfiguration

    companion object {
        private const val REQUEST_SIGN_IN = 1
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

        val addFileButton: Button = findViewById(R.id.add_file_button)
//        addFileButton.setOnClickListener { openFile() }
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
                    getGoogleDriveService(result)
                    listSomeFiles()
                } else {
                    Timber.d("Signin request failed")
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, result)
    }

    private fun listSomeFiles() {
        launch(Dispatchers.Default) {
            try {
                val result = googleDriveService
                    .files().list()
                    .setSpaces("drive")
                    .setQ("'root' in parents")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(null)
                    .execute()
                Timber.d("Result received $result")
                for (file in result.files) {
                    Timber.d("name=${file.name}, id=${file.id}")
                }
            } catch (e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_SIGN_IN);
            }
        }
    }

    private fun getGoogleDriveService(intent: Intent) {
        val googleAccount = GoogleSignIn.getSignedInAccountFromIntent(intent).result
        Timber.d("Signin successful")
        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = googleAccount!!.account
        googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(getString(R.string.app_name))
            .build()
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
}
