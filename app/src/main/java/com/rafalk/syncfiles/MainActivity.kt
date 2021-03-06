package com.rafalk.syncfiles

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.navigation.NavigationView
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.rafalk.syncfiles.autosync.AutoSyncReceiver
import com.rafalk.syncfiles.database.AppDatabase
import com.rafalk.syncfiles.database.DirsPair
import com.rafalk.syncfiles.picker.PickerActivity
import com.rafalk.syncfiles.ui.synced.IntervalPickerDialog
import com.rafalk.syncfiles.ui.synced.PairsListFragment
import com.rafalk.syncfiles.ui.synced.SyncedFragment
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    PairsListFragment.OnListFragmentInteractionListener,
    IntervalPickerDialog.IntervalPickerDialogListener,
    SyncedFragment.SyncedFragmentListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var model: MainViewModel
    private lateinit var db: AppDatabase
    private lateinit var alarmManager: AlarmManager
    lateinit var googleDriveService: Drive

    companion object {
        private const val REQUEST_SIGN_IN = 1
        internal const val GET_DRIVE_DIR_PATH = 2
        internal const val GET_LOCAL_DIR_PATH = 3
        private const val REQUEST_WRITE_STORAGE_REQUEST_CODE = 4
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

        //get alarm manager
        alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // google sign in
        requestSignInToGoogleAccount()

        // request storage
        requestAppPermissions()

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_synced
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
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
        super.onActivityResult(requestCode, resultCode, result)
        Timber.d("onActivityResult=$requestCode")
        when (requestCode) {
            REQUEST_SIGN_IN -> {
                if (resultCode == RESULT_OK && result != null) {
                    Timber.d("Signin successful")
                    getGoogleDriveService()
                } else {
                    Timber.d("Signin request failed")
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

    private fun getGoogleDriveService() {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(this)

        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(DriveScopes.DRIVE)
        )
        credential.selectedAccount = googleAccount!!.account
        googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(getString(R.string.app_name))
            .build()
        launch(Dispatchers.Default) {
            try {
                googleDriveService.files().get("root").setFields("id, name, mimeType").execute()
            } catch (exception: UserRecoverableAuthIOException) {
                startActivityForResult(exception.intent, REQUEST_SIGN_IN);
            }
        }
    }

    override fun onListFragmentInteraction(item: DirsPair?) {
        if (item != null) {
            launch(Dispatchers.Default) {
                db.dirsPairDao().delete(item)
            }
        }
    }

    override fun onIntervalConfirmation(intervalMillis: Long) {
        Timber.d(
            """Got interval in milliseconds $intervalMillis: 
                ${TimeUnit.MILLISECONDS.toDays(intervalMillis)} days
                ${TimeUnit.MILLISECONDS.toHours(intervalMillis)} hours
                ${TimeUnit.MILLISECONDS.toMinutes(intervalMillis)} minutes
                ${TimeUnit.MILLISECONDS.toSeconds(intervalMillis)} seconds"""
        )

        val intent = Intent(applicationContext, AutoSyncReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            intervalMillis,
            alarmIntent
        )
    }

    override fun onCancelAutoSync() {
        val intent = Intent(applicationContext, AutoSyncReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmIntent.cancel()
        alarmManager.cancel(alarmIntent)
    }

    override fun isAutoSync(): Boolean {
        val intent = Intent(applicationContext, AutoSyncReceiver::class.java)
        val alarmIntent: PendingIntent? = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE
        )
        return alarmIntent != null
    }

    private fun requestAppPermissions() {
        if (hasReadPermissions() && hasWritePermissions()) {
            return
        }
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_WRITE_STORAGE_REQUEST_CODE
        )
    }

    private fun hasReadPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasWritePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
