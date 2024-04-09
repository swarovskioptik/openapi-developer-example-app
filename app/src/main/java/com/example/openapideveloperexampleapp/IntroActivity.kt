// SPDX-FileCopyrightText: 2024 Swarovski-Optik AG & Co KG.
// SPDX-License-Identifier: Apache-2.0

package com.example.openapideveloperexampleapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button

/**
 * Introduction activity
 *
 * The first activity of the Android application that the users interacts with. It's the entry
 * point of the app before the connection process to the AX Visio starts.
 */
class IntroActivity : Activity() {
    companion object {
        private const val TAG = "IntroActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: intent=${intent}")

        setContentView(R.layout.activity_intro)

        findViewById<Button>(R.id.buttonStart).setOnClickListener {
            val intent = Intent(this, ConnectActivity::class.java)
            intent.putExtra(
                ConnectActivity.EXTRA_NEXT_ACTIVITY_INTENT,
                Intent(this, MainActivity::class.java)
            )
            intent.putExtra(ConnectActivity.EXTRA_API_KEY, BuildConfig.OPENAPI_API_KEY)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}