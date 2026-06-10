package com.haoze.claudekeyboard.bluetooth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class for handling Bluetooth permissions across different Android versions.
 */
object BluetoothPermissionHelper {

    const val REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1001

    /**
     * Check if all required Bluetooth permissions are granted.
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        val bluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return bluetoothGranted && notificationGranted
    }

    /**
     * Get the list of permissions that need to be requested.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                permissions.add(Manifest.permission.BLUETOOTH)
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    /**
     * Request Bluetooth permissions from the user.
     */
    fun requestPermissions(activity: Activity) {
        val permissions = getRequiredPermissions()
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            REQUEST_CODE_BLUETOOTH_PERMISSIONS
        )
    }

    /**
     * Check if the device supports Bluetooth HID.
     */
    fun isBleHidSupported(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    /**
     * Handle the permission request result.
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            return grantResults.isNotEmpty() && grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }
        }
        return false
    }
}