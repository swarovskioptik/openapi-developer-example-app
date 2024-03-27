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
import com.swarovskioptik.comm.SOCommDeviceSearcher
import com.swarovskioptik.comm.SOCommOutsideAPI
import com.swarovskioptik.comm.SOCommOutsideAPIBuilder
import com.swarovskioptik.comm.definition.SOContext
import com.swarovskioptik.comm.definition.topic.ConfigureKeyActionProcedure
import com.swarovskioptik.comm.definition.topic.KeyAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class MainActivity : Activity() {
    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_CODE = 42
    }

    // Screens/States:
    // - no permissions and no bluetooth, start to request permissions
    // - not connected, start to connect
    // - no OpenAPI context available, waiting for OpenAPI context and use it
    // - using OpenAPI features
    enum class UIState {
        NONE, // Only used initially for the first transition
        REQUEST_PERMISSIONS_AND_BLUETOOTH,
        CONNECT_TO_AX_VISIO,
        WAIT_FOR_OPENAPI_INSIDE_APP,
        MAIN,
    }

    private var uiState = UIState.NONE
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var sdk: SOCommOutsideAPI? = null
    private val disposables = CompositeDisposable()

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
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager!!.adapter

        registerReceiver(
            bluetoothBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        // Create the main SDK object
        sdk = SOCommOutsideAPIBuilder(context = this)
            .apiKey(BuildConfig.OPENAPI_API_KEY)
            .build()

        updateStateAndMaybeUI()

        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        disposables.dispose()
        sdk?.disconnect()?.subscribe()
        sdk = null

        unregisterReceiver(bluetoothBroadcastReceiver)
        bluetoothAdapter = null
        bluetoothManager = null

        super.onDestroy()
    }

    private fun getPermissionForApiLevel(): List<String> {
        val permissions = LinkedList<String>()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            permissions.addAll(
                mutableListOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
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
        setContentView(R.layout.request_permissions_and_bluetooth)

        val button = findViewById<Button>(R.id.buttonRequestPermissions)
        button.setOnClickListener {
            val permissions = getPermissionForApiLevel()
            this.requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun showConnectToAXVisioScreen() {
        setContentView(R.layout.connect_to_ax_visio)

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
                Toast.makeText(this, "Error while searching for AX Visio devices!", Toast.LENGTH_SHORT)
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
        setContentView(R.layout.wait_for_openapi_inside_app)

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
                if (contexts.contains(SOContext.OpenAPIContextBLE)) {
                    sdk!!.use(SOContext.OpenAPIContextBLE)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            // Successfully used the OpenAPIContextBLE. Go to the next screen.
                            updateStateAndMaybeUI()
                        }, { e ->
                            Log.e(TAG, "Cannot use OpenAPIBLE context", e)
                            Toast.makeText(
                                this,
                                "Cannot connect to OpenAPI inside App",
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                        .addTo(disposables)
                } else {
                    // OpenAPIContextBLE context was removed. Maybe the user has turned off
                    // the screen or selected another app with the selection wheel on the AX Visio.
                    updateStateAndMaybeUI()
                }
            }
            .addTo(disposables)
    }

    private fun showMainScreen() {
        setContentView(R.layout.main)

        // NOTE: You should check for errors on the Completable object. But these will only include
        // errors on the local side, e.g. a lost connection to the AX Visio.
        // The remote side, the AX Visio, does not report errors, e.g. a wrong keyCode name or
        // a wrong procedure value.
        val params = ConfigureKeyActionProcedure.Params(
            "SCROLL_KEY",
            KeyAction.Down,
            "TRIGGER_CAMERA_TAKEPICTURE"
        )
        sdk!!.publishTopic(ConfigureKeyActionProcedure, params)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, { e ->
                Log.e(TAG, "Configure the key ActionProcedure failed!", e)
                Toast.makeText(
                    this,
                    "Failed to configure the key on the AX Visio.",
                    Toast.LENGTH_SHORT
                ).show()
            })
            .addTo(disposables)
    }

    private fun getNewUIState(): UIState {
        if (!areRuntimePermissionsGranted() || !bluetoothAdapter!!.isEnabled)
            return UIState.REQUEST_PERMISSIONS_AND_BLUETOOTH

        val connectionState = sdk!!.connectionState.blockingFirst()
        if (connectionState == SOCommOutsideAPI.ConnectionState.Connecting
            || connectionState == SOCommOutsideAPI.ConnectionState.Disconnected
        )
            return UIState.CONNECT_TO_AX_VISIO

        if (!sdk!!.availableContexts.blockingFirst().contains(SOContext.OpenAPIContextBLE)
            || !sdk!!.contextsInUse.blockingFirst().contains(SOContext.OpenAPIContextBLE)
        )
            return UIState.WAIT_FOR_OPENAPI_INSIDE_APP

        return UIState.MAIN
    }

    private fun updateStateAndMaybeUI() {
        val newUIState = getNewUIState()

        if (newUIState != uiState) {
            if (DEBUG) Log.d(TAG, "Switch UI from $uiState to $newUIState")
            when (newUIState) {
                UIState.REQUEST_PERMISSIONS_AND_BLUETOOTH -> showRequestPermissionsAndBluetoothScreen()
                UIState.CONNECT_TO_AX_VISIO -> showConnectToAXVisioScreen()
                UIState.WAIT_FOR_OPENAPI_INSIDE_APP -> showWaitForOpenAPIInsideAppScreen()
                UIState.MAIN -> showMainScreen()
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
