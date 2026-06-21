package com.haoze.claudekeyboard

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.haoze.claudekeyboard.bluetooth.BluetoothViewModel
import com.haoze.claudekeyboard.bluetooth.KeyboardSender
import com.haoze.claudekeyboard.macro.Macro
import com.haoze.claudekeyboard.macro.MacroRepository
import com.haoze.claudekeyboard.ui.device.DeviceListBottomSheetFragment
import com.haoze.claudekeyboard.ui.home.HomeAdapter
import com.haoze.claudekeyboard.ui.home.HomeItem
import com.haoze.claudekeyboard.ui.keyboard.KeyboardFragment
import com.haoze.claudekeyboard.ui.macro.MacroButtonAdapter
import com.haoze.claudekeyboard.ui.macro.MacroEditDialogFragment
import com.haoze.claudekeyboard.ui.settings.SettingsAdapter
import com.haoze.claudekeyboard.ui.settings.SettingsItem
import com.haoze.claudekeyboard.ui.touchpad.TouchpadFragment
import com.haoze.claudekeyboard.ui.tvremote.TvRemoteFragment
import com.haoze.claudekeyboard.util.fixM3Background
import com.haoze.claudekeyboard.util.performKeyClick

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 0
    }

    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    private lateinit var macroRepository: MacroRepository

    // UI components
    private lateinit var btnYes: MaterialButton
    private lateinit var btnNo: MaterialButton
    private lateinit var btnCtrlC: MaterialButton
    private lateinit var btnYesToAll: MaterialButton
    private lateinit var btnBackspace: MaterialButton
    private lateinit var btnEnter: MaterialButton
    private lateinit var macroRecyclerView: RecyclerView
    private lateinit var macroAdapter: MacroButtonAdapter
    private lateinit var btnSettings: ImageButton
    private lateinit var tvClaudeStatus: TextView
    private lateinit var tvHomeStatus: TextView
    private var deviceListDialog: DeviceListBottomSheetFragment? = null

    // Navigation
    private lateinit var contentHome: View
    private lateinit var contentClaude: View
    private lateinit var contentKeyboard: View
    private lateinit var contentTouchpad: View
    private lateinit var contentSettings: View
    private val allContentViews by lazy { listOf(contentHome, contentClaude, contentKeyboard, contentTouchpad, contentTvRemote, contentSettings) }
    private var keyboardFragment: KeyboardFragment? = null
    private var touchpadFragment: TouchpadFragment? = null
    private lateinit var contentTvRemote: View
    private var tvRemoteFragment: TvRemoteFragment? = null
    private lateinit var homeAdapter: HomeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val themeIndex = prefs.getInt("theme_mode_index", 0)
        AppCompatDelegate.setDefaultNightMode(
            when (themeIndex) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Check Bluetooth support
        if (!bluetoothViewModel.hasBluetoothSupport()) {
            Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Check and request permissions
        val missing = getMissingPermissions()
        if (missing.isNotEmpty()) {
            requestPermissions(missing, REQUEST_CODE_PERMISSIONS)
            // Don't start Bluetooth service yet — permissions not granted.
            // Service will be started in onRequestPermissionsResult on success.
        } else {
            // Permissions already granted, safe to start service
            bluetoothViewModel.startAndBindService()
        }

        macroRepository = MacroRepository(this)
        initViews()
        setupWindowInsets()
        setupHomePage()
        setupSettingsPage()
        setupCoreButtons()
        setupMacroRecyclerView()
        observeViewModel()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (contentHome.visibility != View.VISIBLE) {
                    navigateToHome()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isEmpty() || grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
            } else {
                // Permissions granted — safe to start Bluetooth service now
                bluetoothViewModel.startAndBindService()
            }
        }
    }

    private fun getMissingPermissions(): Array<String> {
        val required = mutableListOf<String>()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.BLUETOOTH_CONNECT)
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.BLUETOOTH)
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
                    required.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return required.toTypedArray()
    }

    /**
     * Handle display cutout (front camera) + system bars.
     */
    private fun setupWindowInsets() {
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                insets.displayCutout
            } else null

            val left = maxOf(systemBars.left, cutout?.safeInsetLeft ?: 0)
            val top = maxOf(systemBars.top, cutout?.safeInsetTop ?: 0)
            val right = maxOf(systemBars.right, cutout?.safeInsetRight ?: 0)
            v.setPadding(left, top, right, 0)
            insets
        }
    }

    private fun initViews() {
        btnYes = findViewById(R.id.btn_yes)
        btnNo = findViewById(R.id.btn_no)
        btnCtrlC = findViewById(R.id.btn_ctrl_c)
        btnYesToAll = findViewById(R.id.btn_yes_to_all)
        btnBackspace = findViewById(R.id.btn_backspace)
        btnEnter = findViewById(R.id.btn_enter)
        macroRecyclerView = findViewById(R.id.rv_macros)
        contentHome = findViewById(R.id.content_home)
        contentClaude = findViewById(R.id.content_claude)
        contentKeyboard = findViewById(R.id.content_keyboard)
        contentTouchpad = findViewById(R.id.content_touchpad)
        contentSettings = findViewById(R.id.content_settings)
        contentTvRemote = findViewById(R.id.content_tvremote)
        tvClaudeStatus = findViewById(R.id.tv_claude_status)
        tvHomeStatus = findViewById(R.id.tv_home_status)
        btnSettings = findViewById(R.id.btn_settings)
        btnSettings.setOnClickListener {
            it.performKeyClick()
            navigateToPage(contentSettings, landscape = false)
        }
        findViewById<ImageButton>(R.id.btn_back_claude).setOnClickListener {
            it.performKeyClick()
            navigateToHome()
        }
        findViewById<ImageButton>(R.id.btn_home_settings).setOnClickListener {
            it.performKeyClick()
            navigateToPage(contentSettings, landscape = false)
        }
        findViewById<View>(R.id.card_home_status).setOnClickListener {
            it.performKeyClick()
            showDeviceListDialog()
        }
    }

    private fun setupHomePage() {
        val rvHome = findViewById<RecyclerView>(R.id.rv_home)
        homeAdapter = HomeAdapter { entry ->
            when (entry.id) {
                "keyboard" -> navigateToPage(contentKeyboard, landscape = true)
                "touchpad" -> navigateToPage(contentTouchpad, landscape = true)
                "tvremote" -> navigateToPage(contentTvRemote, landscape = false)
                "claude" -> navigateToPage(contentClaude, landscape = false)
                "settings" -> navigateToPage(contentSettings, landscape = false)
            }
        }
        rvHome.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = homeAdapter
        }
        loadHomeItems()
    }

    private fun loadHomeItems() {
        val items = listOf(
            HomeItem.Section(getString(R.string.home_functions)),
            HomeItem.Entry(
                id = "keyboard",
                title = getString(R.string.home_keyboard_title),
                subtitle = getString(R.string.home_keyboard_subtitle),
                iconRes = R.drawable.baseline_keyboard_24,
                iconBackgroundRes = R.drawable.bg_home_icon_keyboard
            ),
            HomeItem.Entry(
                id = "touchpad",
                title = getString(R.string.home_touchpad_title),
                subtitle = getString(R.string.home_touchpad_subtitle),
                iconRes = R.drawable.baseline_mouse_24,
                iconBackgroundRes = R.drawable.bg_home_icon_touchpad
            ),
            HomeItem.Entry(
                id = "tvremote",
                title = getString(R.string.home_tvremote_title),
                subtitle = getString(R.string.home_tvremote_subtitle),
                iconRes = R.drawable.baseline_settings_remote_24,
                iconBackgroundRes = R.drawable.bg_home_icon_tvremote
            ),
            HomeItem.Entry(
                id = "claude",
                title = getString(R.string.home_claude_title),
                subtitle = getString(R.string.home_claude_subtitle),
                iconRes = R.drawable.baseline_terminal_24,
                iconBackgroundRes = R.drawable.bg_home_icon_claude
            ),
            HomeItem.Entry(
                id = "settings",
                title = getString(R.string.home_settings_title),
                subtitle = getString(R.string.home_settings_subtitle),
                iconRes = R.drawable.baseline_settings_24,
                iconBackgroundRes = R.drawable.bg_home_icon_settings
            )
        )
        homeAdapter.submitList(items)
    }

    private fun showOnly(target: View) {
        allContentViews.forEach { it.visibility = if (it == target) View.VISIBLE else View.GONE }
    }

    fun navigateToPage(targetContent: View, landscape: Boolean) {
        showOnly(targetContent)

        requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (targetContent == contentKeyboard) {
            keyboardFragment = supportFragmentManager.findFragmentById(R.id.keyboard_fragment_container) as? KeyboardFragment
            updateFragmentEnabledStates()
        } else if (targetContent == contentTouchpad) {
            touchpadFragment = supportFragmentManager.findFragmentById(R.id.touchpad_fragment_container) as? TouchpadFragment
            touchpadFragment?.reloadSettings()
            updateFragmentEnabledStates()
        } else if (targetContent == contentTvRemote) {
            tvRemoteFragment = supportFragmentManager.findFragmentById(R.id.tvremote_fragment_container) as? TvRemoteFragment
            updateFragmentEnabledStates()
        }
    }

    fun navigateToHome() {
        showOnly(contentHome)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        updateDeviceSubtitle()
        updateKeepScreenOn(bluetoothViewModel.isConnected())
    }

    private fun updateDeviceSubtitle() {
        val isConnected = bluetoothViewModel.isConnected()
        val deviceName = bluetoothViewModel.getConnectedDeviceName()
        tvHomeStatus.text = if (isConnected && deviceName != null) {
            getString(R.string.device_name_status, deviceName, getString(R.string.status_connected_label))
        } else {
            getString(R.string.status_not_connected)
        }
    }

    fun switchToTouchpadTab() {
        navigateToPage(contentTouchpad, landscape = true)
    }

    fun switchToKeyboardTab() {
        navigateToPage(contentKeyboard, landscape = true)
    }

    /**
     * Observe ViewModel LiveData for connection state changes.
     */
    private fun observeViewModel() {
        bluetoothViewModel.connectionState.observe(this) { isConnected ->
            val deviceName = bluetoothViewModel.connectedDeviceName.value
            updateStatusUI(isConnected, deviceName)
            updateKeepScreenOn(isConnected)
            if (isConnected) {
                deviceListDialog?.onConnectionSuccess()
                deviceListDialog = null
            }
        }

        bluetoothViewModel.connectedDeviceName.observe(this) {
            updateDeviceSubtitle()
            val isConnected = bluetoothViewModel.isConnected()
            updateStatusUI(isConnected, it)
        }

        bluetoothViewModel.registrationState.observe(this) { isRegistered ->
            if (!isRegistered) {
                Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            }
        }

        bluetoothViewModel.keyboardSender.observe(this) {
            updateFragmentEnabledStates()
        }

        bluetoothViewModel.mouseSender.observe(this) {
            updateFragmentEnabledStates()
        }

        bluetoothViewModel.tvRemoteSender.observe(this) {
            updateFragmentEnabledStates()
        }

        // Show a brief toast when a HID report fails to send, so the user
        // knows their input was not delivered to the host.
        bluetoothViewModel.sendError.observe(this) { message ->
            Toast.makeText(this, getString(R.string.toast_send_error, message), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Update fragment enabled states based on connection.
     */
    private fun updateFragmentEnabledStates() {
        val connected = bluetoothViewModel.isConnected()
        keyboardFragment?.setKeyboardEnabled(connected)
        touchpadFragment?.setTouchpadEnabled(connected)
        tvRemoteFragment?.setTvRemoteEnabled(connected)
    }

    private fun updateKeepScreenOn(isConnected: Boolean) {
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val keepScreenOn = prefs.getBoolean("keep_screen_on", true)
        if (isConnected && keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setupSettingsPage() {
        val btnBack = findViewById<ImageButton>(R.id.btn_back_settings)
        btnBack.setOnClickListener {
            it.performKeyClick()
            navigateToHome()
        }

        val rvSettings = findViewById<RecyclerView>(R.id.rv_settings)
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

        val adapter = SettingsAdapter(
            prefs = prefs,
            onToggleGroupChanged = { key, index ->
                if (key == "theme_mode_index") {
                    AppCompatDelegate.setDefaultNightMode(
                        when (index) {
                            1 -> AppCompatDelegate.MODE_NIGHT_NO
                            2 -> AppCompatDelegate.MODE_NIGHT_YES
                            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                    )
                    recreate()
                }
            },
            onButtonClick = { item ->
                when (item.title) {
                    getString(R.string.settings_reset_macros) -> {
                        val dlg = MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.settings_reset_macros)
                            .setMessage(R.string.dialog_reset_macros_confirm)
                            .setPositiveButton(R.string.dialog_reset) { _, _ ->
                                macroRepository.resetToDefaults()
                                loadMacros()
                            }
                            .setNegativeButton(R.string.dialog_cancel, null)
                            .create()
                        dlg.fixM3Background()
                        dlg.show()
                    }
                }
            },
            onSwitchChanged = { key, value ->
                if (key == "connection_notifications" && !value) {
                    bluetoothViewModel.dismissConnectionNotification()
                }
            }
        )

        rvSettings.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = adapter
        }

        val versionName = try {
            val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        val items = listOf(
            SettingsItem.SectionHeader(getString(R.string.settings_section_connection)),
            SettingsItem.SwitchItem("auto_connect_on_launch", getString(R.string.settings_auto_connect_launch), null, true),
            SettingsItem.SwitchItem("auto_reconnect_on_disconnect", getString(R.string.settings_auto_reconnect), null, true),
            SettingsItem.SwitchItem("keep_screen_on", getString(R.string.settings_keep_screen_on), getString(R.string.settings_keep_screen_on_subtitle), true),
            SettingsItem.SwitchItem("connection_notifications", getString(R.string.settings_connection_notifications), getString(R.string.settings_connection_notifications_subtitle), true),
            SettingsItem.SectionHeader(getString(R.string.settings_section_touchpad)),
            SettingsItem.SliderItem("touchpad_sensitivity", getString(R.string.settings_touchpad_sensitivity), null, 5, 1f, 10f, 1f),
            SettingsItem.SliderItem("cursor_speed", getString(R.string.settings_cursor_speed), null, 5, 1f, 10f, 1f),
            SettingsItem.SwitchItem("scroll_direction_natural", getString(R.string.settings_scroll_direction_natural), null, false),
            SettingsItem.SectionHeader(getString(R.string.settings_section_interaction)),
            SettingsItem.SwitchItem("haptic_feedback", getString(R.string.settings_haptic_feedback), getString(R.string.settings_haptic_feedback_subtitle), true),
            SettingsItem.SectionHeader(getString(R.string.settings_section_data)),
            SettingsItem.ButtonItem(getString(R.string.settings_reset_macros)),
            SettingsItem.SectionHeader(getString(R.string.settings_section_appearance)),
            SettingsItem.ToggleGroupItem("theme_mode_index", getString(R.string.settings_theme_mode), listOf(getString(R.string.settings_theme_system), getString(R.string.settings_theme_light), getString(R.string.settings_theme_dark)), 0),
            SettingsItem.SectionHeader(getString(R.string.settings_section_about)),
            SettingsItem.InfoItem(getString(R.string.settings_app_name_label), getString(R.string.app_name)),
            SettingsItem.InfoItem(getString(R.string.settings_version), versionName),
            SettingsItem.InfoItem(getString(R.string.settings_open_source), "", onClick = {
                // Placeholder for open source license
            })
        )
        adapter.submitList(items)
    }

    private fun setupCoreButtons() {
        setupCoreButton(btnYes) { it.sendText("y") }
        setupCoreButton(btnNo) { it.sendText("n") }
        setupCoreButton(btnCtrlC) { it.sendKeyPress(KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.KEY_C) }
        setupCoreButton(btnYesToAll) { it.sendText("a") }
        setupCoreButton(btnBackspace) { it.sendKeyPress(0x00, KeyboardSender.KEY_BACKSPACE) }
        setupCoreButton(btnEnter) { it.sendKeyPress(0x00, KeyboardSender.KEY_ENTER) }
    }

    private fun setupCoreButton(button: MaterialButton, action: (KeyboardSender) -> Unit) {
        button.setOnClickListener {
            it.performKeyClick()
            bluetoothViewModel.getKeyboardSenderDirect()?.let { s -> Thread { action(s) }.start() }
        }
    }

    private fun setupMacroRecyclerView() {
        macroAdapter = MacroButtonAdapter(
            onMacroClick = { macro ->
                if (macro.id == "add_custom") {
                    showAddMacroDialog()
                } else {
                    bluetoothViewModel.getKeyboardSenderDirect()?.let { s ->
                        Thread {
                            if (macro.sendEnter) s.sendMacro(macro.command)
                            else s.sendText(macro.command)
                        }.start()
                    }
                }
            },
            onMacroLongClick = { macro ->
                if (!macro.isPreset) showEditMacroDialog(macro)
            }
        )
        macroRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = macroAdapter
        }
        loadMacros()
    }

    private fun loadMacros() {
        val macros = macroRepository.getAllMacros().toMutableList()
        macros.add(Macro(id = "add_custom", label = getString(R.string.btn_add_macro), command = "", isPreset = true, sortOrder = Int.MAX_VALUE))
        macroAdapter.submitList(macros)
    }

    private fun updateStatusUI(isConnected: Boolean, deviceName: String?) {
        if (isConnected) {
            enableAllButtons()
            tvClaudeStatus.text = getString(R.string.device_name_status, deviceName ?: getString(R.string.status_unknown_device), getString(R.string.status_connected_label))
            tvClaudeStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            disableAllButtons()
            tvClaudeStatus.text = getString(R.string.status_not_connected)
            tvClaudeStatus.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
        }
        // Update fragment enabled states
        updateFragmentEnabledStates()
        updateDeviceSubtitle()
    }

    private fun enableAllButtons() {
        btnYes.isEnabled = true; btnNo.isEnabled = true; btnCtrlC.isEnabled = true
        btnYesToAll.isEnabled = true
    }

    private fun disableAllButtons() {
        btnYes.isEnabled = false; btnNo.isEnabled = false; btnCtrlC.isEnabled = false
        btnYesToAll.isEnabled = false
    }

    private fun showEditMacroDialog(macro: Macro) {
        val dialog = MacroEditDialogFragment.newInstance(macro)
        dialog.setOnSaveListener { id, label, description, command, sendEnter ->
            if (id != null) { macroRepository.updateCustomMacro(id, label, description, command, sendEnter); Toast.makeText(this, R.string.toast_macro_updated, Toast.LENGTH_SHORT).show() }
            loadMacros()
        }
        dialog.setOnDeleteListener { id -> showDeleteConfirmationDialog(id) }
        dialog.show(supportFragmentManager, "edit_macro")
    }

    private fun showAddMacroDialog() {
        val dialog = MacroEditDialogFragment.newInstance()
        dialog.setOnSaveListener { _, label, description, command, sendEnter ->
            macroRepository.addCustomMacro(label, description, command, sendEnter)
            Toast.makeText(this, R.string.toast_macro_added, Toast.LENGTH_SHORT).show()
            loadMacros()
        }
        dialog.show(supportFragmentManager, "add_macro")
    }

    private fun showDeleteConfirmationDialog(macroId: String) {
        val dlg = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_macro)
            .setMessage(R.string.dialog_delete_macro_message)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                macroRepository.deleteCustomMacro(macroId)
                Toast.makeText(this, R.string.toast_macro_deleted, Toast.LENGTH_SHORT).show()
                loadMacros()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
        dlg.fixM3Background()
        dlg.show()
    }

    private fun showDeviceListDialog() {
        val dialog = DeviceListBottomSheetFragment.newInstance(
            connectedAddress = bluetoothViewModel.getConnectedDeviceAddress(),
            lastConnectedAddress = bluetoothViewModel.getLastConnectedDeviceAddress()
        )
        dialog.setDeviceSelectionListener(object : DeviceListBottomSheetFragment.DeviceSelectionListener {
            override fun onDeviceSelected(device: android.bluetooth.BluetoothDevice) {
                Thread {
                    val success = bluetoothViewModel.connectToDevice(device.address)
                    if (!success) {
                        runOnUiThread {
                            dialog.onConnectionFailed()
                        }
                    }
                }.start()
            }

            override fun onDisconnectRequested(device: android.bluetooth.BluetoothDevice) {
                bluetoothViewModel.disconnect()
                dialog.dismiss()
            }

            override fun onConnectionCancelled() {
                bluetoothViewModel.disconnect()
            }
        })
        deviceListDialog = dialog
        dialog.show(supportFragmentManager, "device_list")
    }
}
