package com.haoze.claudekeyboard.util

/**
 * Global constants for the Claude Code Keyboard app.
 */
object Constants {

    // Timing constants (milliseconds)
    const val KEY_PRESS_DELAY = 20L
    const val KEY_TYPING_DELAY = 30L
    const val MACRO_TYPING_DELAY = 30L

    // UI constants
    const val BUTTON_CORNER_RADIUS = 16f
    const val MACRO_BUTTON_CORNER_RADIUS = 12f

    // SharedPreferences keys
    const val PREFS_NAME_MACRO = "macro_prefs"
    const val KEY_MACROS = "macros"

    // Notification constants
    const val NOTIFICATION_CHANNEL_ID = "bluetooth_hid_channel"
    const val NOTIFICATION_ID = 1001

    // Request codes
    const val REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1001
    const val REQUEST_CODE_ENABLE_BLUETOOTH = 1002

    // Connection states
    const val STATE_DISCONNECTED = 0
    const val STATE_CONNECTING = 1
    const val STATE_CONNECTED = 2

    // Log tags
    const val TAG_BLUETOOTH = "BluetoothHid"
    const val TAG_MACRO = "MacroManager"
    const val TAG_UI = "KeyboardUI"

    // Error messages
    const val ERROR_BLUETOOTH_NOT_SUPPORTED = "此设备不支持蓝牙HID"
    const val ERROR_BLUETOOTH_DISABLED = "蓝牙已关闭"
    const val ERROR_PERMISSION_DENIED = "蓝牙权限被拒绝"
    const val ERROR_HID_REGISTRATION_FAILED = "HID注册失败"
    const val ERROR_SEND_FAILED = "发送按键报告失败"

    // Success messages
    const val SUCCESS_CONNECTED = "已连接到设备"
    const val SUCCESS_DISCONNECTED = "已断开连接"
    const val SUCCESS_MACRO_ADDED = "宏添加成功"
    const val SUCCESS_MACRO_UPDATED = "宏更新成功"
    const val SUCCESS_MACRO_DELETED = "宏删除成功"
}