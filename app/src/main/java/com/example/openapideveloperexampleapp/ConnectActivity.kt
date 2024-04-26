// SPDX-FileCopyrightText: 2024 Swarovski-Optik AG & Co KG.
// SPDX-License-Identifier: Apache-2.0

package com.example.openapideveloperexampleapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.example.openapideveloperexampleapp.BuildConfig.DEBUG
import com.jakewharton.threetenabp.AndroidThreeTen
import com.swarovskioptik.comm.SOCommDeviceSearcher
import com.swarovskioptik.comm.SOCommOutsideAPI
import com.swarovskioptik.comm.SOCommOutsideAPIBuilder
import com.swarovskioptik.comm.definition.SOContext
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Complete the necessary steps to connect to the AX Visio
 *
 * This activity guides the user trough the necessary steps to connect the smartphone
 * to the AX Visio. These steps must be done for all apps that want to use the OpenAPI.
 * Therefore they are extract into a distinct activity. This should make code reuse, e.g.
 * copy&paste to other applications easier.
 *
 * Notes how to use the activity:
 *
 * When starting the activity the calling intent must contain two parameters as extra data.
 * It must contain the OpenAPI API key under then name {@link #EXTRA_API_KEY}. And it must contain
 * an intent that should be called under the {@link #EXTRA_NEXT_ACTIVITY_INTENT}, when the connection
 * to the AX Visio was successful.
 *
 * After this activity has finished, the called activity can starting using SO Contexts and
 * publishing and subscribing topics.
 */
class ConnectActivity : Activity() {
    companion object {
        private const val TAG = "ConnectActivity"
        private const val PERMISSION_REQUEST_CODE = 42

        const val EXTRA_NEXT_ACTIVITY_INTENT = "nextActivityIntent"
        const val EXTRA_API_KEY = "apiKey"

        private var sdk: SOCommOutsideAPI? = null
        fun getSdk(): SOCommOutsideAPI? {
            return sdk
        }
    }

    // Screens/States:
    // - no permissions and no bluetooth, start to request permissions
    // - not connected, start to connect
    // - no OpenAPI context available, waiting for OpenAPI context and use it
    enum class UIState {
        NONE, // Only used initially for the first transition
        REQUEST_PERMISSIONS_AND_BLUETOOTH,
        CONNECT_TO_AX_VISIO,
        WAIT_FOR_OPENAPI_INSIDE_APP,
    }

    private var uiState = UIState.NONE
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val disposables = CompositeDisposable()
    private var nextActivityIntent: Intent? = null
    private var apiKey: String? = null

    private fun bluetoothAdapterStateToString(i: Int): String {
        return when (i) {
            BluetoothAdapter.STATE_OFF -> "STATE_OFF"
            BluetoothAdapter.STATE_TURNING_OFF -> "STATE_TURNING_OFF"
            BluetoothAdapter.STATE_ON -> "STATE_ON"
            BluetoothAdapter.STATE_TURNING_ON -> "STATE_TURNING_ON"
            else -> "UNKNOWN"
        }
    }

    // To monitor the system state of bluetooth
    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action != BluetoothAdapter.ACTION_STATE_CHANGED)
                return

            val state = intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR
            )

            if (DEBUG) Log.d(TAG, "bluetooth state: ${bluetoothAdapterStateToString(state)}")
            updateStateAndMaybeUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DEBUG) Log.d(TAG, "onCreate: intent=${intent}")

        nextActivityIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_NEXT_ACTIVITY_INTENT, Intent::class.java)
        } else {
            // See https://stackoverflow.com/a/73019285
            intent.getParcelableExtra(EXTRA_NEXT_ACTIVITY_INTENT)
        }
        if (nextActivityIntent == null) {
            Toast.makeText(this, "Internal Error", Toast.LENGTH_SHORT).show()
            Log.e(
                TAG,
                "Cannot start ConnectActivity: 'nextActivityIntent' is null. Calling Activity must provided it!"
            )
            finish()
            return
        }

        apiKey = intent.getStringExtra(EXTRA_API_KEY)
        if (apiKey == null || apiKey!!.isEmpty()) {
            Toast.makeText(this, "Internal Error", Toast.LENGTH_SHORT).show()
            Log.e(
                TAG,
                "Cannot start ConnectActivity: 'apiKey' is null or empty. Calling Activity must provided it!"
            )
            finish()
            return
        }

        // There is currently a bug in the SOCommOutsideAPI library. Until this bug is resolved
        // this code line must init the transitive dependency AndroidThreeTen. The exception is:
        //    Caused by: org.threeten.bp.zone.ZoneRulesException: No time-zone data files registered
        AndroidThreeTen.init(this)

        // TODO How to handle bluetooth remove/shutdown? The bluetooth broadcast receiver is
        // only in this activity.
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager!!.adapter
        registerReceiver(
            bluetoothBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        // Create the main SDK object
        sdk = SOCommOutsideAPIBuilder(context = this)
            .apiKey(apiKey!!)
            .build()

        updateStateAndMaybeUI()
    }

    override fun onDestroy() {
        super.onDestroy()

        disposables.dispose()
        sdk?.disconnect()?.subscribe()
        sdk = null

        if (bluetoothManager != null) {
            // Only unregister if the receiver was registered in onCreate(). Otherwise it raises
            // an exception.
            unregisterReceiver(bluetoothBroadcastReceiver)
            bluetoothAdapter = null
            bluetoothManager = null
        }
    }

    private fun getPermissionForApiLevel(): List<String> {
        val permissions = LinkedList<String>()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            permissions.addAll(
                mutableListOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        // NOTE: When using the MediaClient more runtime permissions are needed.

        return permissions
    }

    private fun areRuntimePermissionsGranted(): Boolean {
        val permissions = getPermissionForApiLevel()
        permissions.forEach { permission ->
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED)
                return false
        }
        return true
    }

    private fun showRequestPermissionsAndBluetoothScreen() {
        setContentView(R.layout.activity_connect_request_permissions_and_bluetooth)

        val button = findViewById<Button>(R.id.buttonRequestPermissions)
        button.setOnClickListener {
            val permissions = getPermissionForApiLevel()
            this.requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun showConnectToAXVisioScreen() {
        setContentView(R.layout.activity_connect_connect_to_ax_visio)

        val buttonConnectToFolke = findViewById<Button>(R.id.buttonConnectToAXVisio)

        // Disable the button until at least one device is found
        buttonConnectToFolke.isEnabled = false
        buttonConnectToFolke.text = getString(R.string.connect_to_ax_visio, "UNKNOWN")

        // TODO: This code starts the search and stops when the first device is found.
        // A real world application would continue the search and show the user a list of
        // available devices. A further optimization is to save the last used device name
        // and present it to the user without searching for new devices.
        // NOTE: The search process drains the battery. It should be stopped as early as
        // possible.
        val deviceSearchDisposables = CompositeDisposable()
        val atomicDeviceValue: AtomicReference<String> = AtomicReference("")
        val deviceSearcher = SOCommDeviceSearcher.create(this)
        deviceSearcher.search()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ foundDevices ->
                if (DEBUG) Log.d(TAG, "foundDevices: $foundDevices")
                if (foundDevices.isEmpty())
                    return@subscribe

                // Just use the first device. This is only example code to show the concepts.
                // A real world example would add the found devices to a list and show it to the
                // user.
                val foundDevice = foundDevices.first()

                atomicDeviceValue.set(foundDevice.deviceName)
                buttonConnectToFolke.isEnabled = true
                buttonConnectToFolke.text =
                    getString(R.string.connect_to_ax_visio, foundDevice.deviceName)

                // Dispose the Observable now. This will stop the search process.
                deviceSearchDisposables.dispose()
            }, { e ->
                Log.e(TAG, "Error while searching AX Visio devices", e)
                Toast.makeText(
                    this,
                    "Error while searching for AX Visio devices!",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            )
            .addTo(deviceSearchDisposables)

        buttonConnectToFolke.setOnClickListener { button ->
            button.isEnabled = false

            sdk!!.connect(atomicDeviceValue.get())
                // Add a timeout. Otherwise the screen will block forever when no AX Visio device
                // is in reach! But the timeout must also be long enough so the user can handle
                // the initial pairing.
                .timeout(60, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        button.isEnabled = true
                        updateStateAndMaybeUI()
                    }, { e ->
                        button.isEnabled = true
                        Log.e(TAG, "Connect connect to the AX Visio device", e)
                        Toast.makeText(this, "Connecting to AX Visio failed!", Toast.LENGTH_SHORT)
                            .show()
                        updateStateAndMaybeUI()
                    }
                ).addTo(disposables)
        }
    }

    private fun showWaitForOpenAPIInsideAppScreen() {
        setContentView(R.layout.activity_connect_wait_for_openapi_inside_app)

        sdk!!.connectionState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ state ->
                if (DEBUG) Log.d(TAG, "connectionState: $state")
                if (state == SOCommOutsideAPI.ConnectionState.Disconnected) {
                    Log.w(TAG, "Connection to AX Visio lost!")
                    Toast.makeText(
                        this,
                        "Connection to AX Visio lost. Please try to reconnect",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, { e ->
                Log.e(TAG, "Connection state failed!", e)
                Toast.makeText(this, "Connection to AX Visio failed!", Toast.LENGTH_SHORT).show()
                updateStateAndMaybeUI()
            })
            .addTo(disposables)

        sdk!!.availableContexts
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { contexts ->
                if (DEBUG) Log.d(TAG, "availableContexts: $contexts")
                contexts.contains(SOContext.OpenAPIContextBLE).let { contextAvailable ->
                    findViewById<Button>(R.id.buttonContinueToMain).apply {
                        isClickable = contextAvailable
                        isEnabled = contextAvailable
                    }
                }
            }
            .addTo(disposables)

        findViewById<Button>(R.id.buttonContinueToMain).let {
            it.isEnabled = false
            it.isClickable = false
            it.setOnClickListener {
                val intent = nextActivityIntent!!
                intent.putExtra("goto", Intent(this, MainActivity::class.java))
                startActivity(intent)
            }
        }
    }

    private fun getNewUIState(): UIState {
        if (!areRuntimePermissionsGranted() || !bluetoothAdapter!!.isEnabled)
            return UIState.REQUEST_PERMISSIONS_AND_BLUETOOTH

        val connectionState = sdk!!.connectionState.blockingFirst()
        if (connectionState == SOCommOutsideAPI.ConnectionState.Connecting
            || connectionState == SOCommOutsideAPI.ConnectionState.Disconnected
        )
            return UIState.CONNECT_TO_AX_VISIO

        return UIState.WAIT_FOR_OPENAPI_INSIDE_APP
    }

    private fun updateStateAndMaybeUI() {
        val newUIState = getNewUIState()

        if (newUIState != uiState) {
            if (DEBUG) Log.d(TAG, "Switch UI from $uiState to $newUIState")
            when (newUIState) {
                UIState.REQUEST_PERMISSIONS_AND_BLUETOOTH -> showRequestPermissionsAndBluetoothScreen()
                UIState.CONNECT_TO_AX_VISIO -> showConnectToAXVisioScreen()
                UIState.WAIT_FOR_OPENAPI_INSIDE_APP -> showWaitForOpenAPIInsideAppScreen()
                UIState.NONE -> throw RuntimeException("Should never happen")
            }
            uiState = newUIState
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        assert(requestCode == PERMISSION_REQUEST_CODE)

        if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            Log.e(
                TAG,
                "The user did not grant all permissions: permissions=${permissions.contentToString()} grantResults=${grantResults.contentToString()}"
            )
            // TODO for a real applications:
            // Add note, when a user denied the permission once, she has to enable the permission
            // with the App Settings dialog. The app cannot request it again.
            Toast.makeText(this, "Some permissions were not granted!", Toast.LENGTH_SHORT).show()
        }

        updateStateAndMaybeUI()
    }
}
