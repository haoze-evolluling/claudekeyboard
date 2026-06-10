package com.haoze.claudekeyboard

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/**
 * Splash screen that handles permission requests before launching MainActivity.
 * Ported from Kontroller's SplashScreen pattern.
 */
class SplashActivity : Activity() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check Bluetooth support first
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Check and request permissions
        val missing = getMissingPermissions()
        if (missing.isNotEmpty()) {
            requestPermissions(missing, REQUEST_CODE_PERMISSIONS)
        } else {
            launchMain()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                launchMain()
            } else {
                Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getMissingPermissions(): Array<String> {
        val required = mutableListOf<String>()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // API 31+: BLUETOOTH_CONNECT + BLUETOOTH_ADVERTISE
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.BLUETOOTH_CONNECT)
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // API 29-30: ACCESS_FINE_LOCATION
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // Below 29: BLUETOOTH + BLUETOOTH_ADMIN
                if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.BLUETOOTH)
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        // API 33+: POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return required.toTypedArray()
    }

    private fun launchMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
