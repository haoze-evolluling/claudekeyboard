package com.haoze.claudekeyboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.haoze.claudekeyboard.bluetooth.BluetoothHidService
import com.haoze.claudekeyboard.bluetooth.KeyboardSender
import com.haoze.claudekeyboard.macro.Macro
import com.haoze.claudekeyboard.macro.MacroRepository
import com.haoze.claudekeyboard.ui.device.DeviceListBottomSheetFragment
import com.haoze.claudekeyboard.ui.keyboard.KeyboardFragment
import com.haoze.claudekeyboard.ui.macro.MacroButtonAdapter
import com.haoze.claudekeyboard.ui.macro.MacroEditDialogFragment
import com.haoze.claudekeyboard.util.Constants

class MainActivity : AppCompatActivity() {

    private var hidService: BluetoothHidService? = null
    private var isBound = false
    private lateinit var macroRepository: MacroRepository

    // UI components
    private lateinit var tvDeviceName: TextView
    private lateinit var tvDeviceAction: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var btnYes: MaterialButton
    private lateinit var btnNo: MaterialButton
    private lateinit var btnCtrlC: MaterialButton
    private lateinit var btnYesToAll: MaterialButton
    private lateinit var btnBackspace: MaterialButton
    private lateinit var btnEnter: MaterialButton
    private lateinit var inputLayout: TextInputLayout
    private lateinit var inputText: TextInputEditText
    private lateinit var macroRecyclerView: RecyclerView
    private lateinit var macroAdapter: MacroButtonAdapter
    private var deviceListDialog: DeviceListBottomSheetFragment? = null

    // Bottom navigation
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var contentClaude: View
    private lateinit var contentKeyboard: View
    private var keyboardFragment: KeyboardFragment? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothHidService.LocalBinder
            hidService = binder.getService()
            isBound = true
            setupServiceCallbacks()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        macroRepository = MacroRepository(this)
        initViews()
        setupWindowInsets()
        setupBottomNavigation()
        setupCoreButtons()
        setupMacroRecyclerView()
        setupTextInput()
        startAndBindHidService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun applyTheme() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)
        val themeMode = prefs.getInt(Constants.KEY_THEME_MODE, Constants.THEME_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(when (themeMode) {
            Constants.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        })
    }

    private fun getThemeModeName(mode: Int): String {
        val modeName = when (mode) {
            Constants.THEME_LIGHT -> getString(R.string.theme_light)
            Constants.THEME_DARK -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_follow_system)
        }
        return "${getString(R.string.settings_theme_mode)}：$modeName"
    }

    /**
     * Handle display cutout (front camera) + system bars.
     * Bottom inset is handled by BottomNavigationView separately.
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
            // Don't apply bottom padding here; bottom nav handles it
            v.setPadding(left, top, right, 0)
            insets
        }

        // Let bottom nav handle the bottom system bar inset
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        tvDeviceName = findViewById(R.id.tv_device_name)
        tvDeviceAction = findViewById(R.id.tv_device_action)
        btnSettings = findViewById(R.id.btn_settings)
        btnYes = findViewById(R.id.btn_yes)
        btnNo = findViewById(R.id.btn_no)
        btnCtrlC = findViewById(R.id.btn_ctrl_c)
        btnYesToAll = findViewById(R.id.btn_yes_to_all)
        btnBackspace = findViewById(R.id.btn_backspace)
        btnEnter = findViewById(R.id.btn_enter)
        inputLayout = findViewById(R.id.til_input)
        inputText = findViewById(R.id.et_input_text)
        macroRecyclerView = findViewById(R.id.rv_macros)
        bottomNav = findViewById(R.id.bottom_nav)
        contentClaude = findViewById(R.id.content_claude)
        contentKeyboard = findViewById(R.id.content_keyboard)

        tvDeviceAction.setOnClickListener {
            val service = hidService
            if (service != null && service.isConnected()) {
                service.disconnect()
            } else {
                showDeviceListDialog()
            }
        }
        btnSettings.setOnClickListener { showSettingsDialog() }
    }

    private fun setupBottomNavigation() {
        // Divider above bottom nav
        val dividerAboveNav = findViewById<View>(R.id.divider_above_nav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_claude -> {
                    contentClaude.visibility = View.VISIBLE
                    contentKeyboard.visibility = View.GONE
                    // Show bottom nav for Claude tab
                    bottomNav.visibility = View.VISIBLE
                    dividerAboveNav.visibility = View.VISIBLE
                    // Switch back to portrait for Claude tab
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    true
                }
                R.id.nav_keyboard -> {
                    contentClaude.visibility = View.GONE
                    contentKeyboard.visibility = View.VISIBLE
                    // Hide bottom nav for keyboard tab
                    bottomNav.visibility = View.GONE
                    dividerAboveNav.visibility = View.GONE
                    // Switch to landscape for keyboard tab
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    // Find keyboard fragment
                    keyboardFragment = supportFragmentManager.findFragmentById(R.id.keyboard_fragment_container) as? KeyboardFragment
                    updateKeyboardEnabled()
                    true
                }
                else -> false
            }
        }
        // Default: Claude tab selected
        bottomNav.selectedItemId = R.id.nav_claude
    }

    /**
     * Get the KeyboardSender for use by KeyboardFragment.
     */
    fun getKeyboardSender(): KeyboardSender? {
        return hidService?.getKeyboardSender()
    }

    /**
     * Switch to Claude tab programmatically (called from KeyboardFragment).
     */
    fun switchToClaudeTab() {
        bottomNav.selectedItemId = R.id.nav_claude
    }

    /**
     * Update keyboard fragment enabled state based on connection.
     */
    private fun updateKeyboardEnabled() {
        val isConnected = hidService?.isConnected() == true
        keyboardFragment?.setKeyboardEnabled(isConnected)
    }

    private fun setupCoreButtons() {
        btnYes.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("y") }.start() }
        }
        btnNo.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("n") }.start() }
        }
        btnCtrlC.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.KEY_C) }.start() }
        }
        btnYesToAll.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("a") }.start() }
        }
        btnBackspace.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(0x00, KeyboardSender.KEY_BACKSPACE) }.start() }
        }
        btnEnter.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(0x00, KeyboardSender.KEY_ENTER) }.start() }
        }
    }

    private fun setupMacroRecyclerView() {
        macroAdapter = MacroButtonAdapter(
            onMacroClick = { macro ->
                if (macro.id == "add_custom") {
                    showAddMacroDialog()
                } else {
                    hidService?.getKeyboardSender()?.let { s ->
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

    private fun setupTextInput() {
        inputLayout.setEndIconOnClickListener { sendInputText() }
        inputText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) { sendInputText(); true } else false
        }
    }

    private fun sendInputText() {
        val text = inputText.text.toString()
        if (text.isNotEmpty()) {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText(text); s.sendKeyPress(0x00, KeyboardSender.KEY_ENTER) }.start() }
            inputText.text?.clear()
        }
    }

    private fun startAndBindHidService() {
        val intent = Intent(this, BluetoothHidService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupServiceCallbacks() {
        hidService?.setOnConnectionStateChangedListener { isConnected, deviceName ->
            runOnUiThread {
                updateStatusUI(isConnected, deviceName)
                if (isConnected) {
                    deviceListDialog?.onConnectionSuccess()
                    deviceListDialog = null
                }
            }
        }
        hidService?.setOnRegistrationStateChangedListener { isRegistered ->
            runOnUiThread { if (!isRegistered) Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_SHORT).show() }
        }
        hidService?.let { updateStatusUI(it.isConnected(), it.getConnectedDeviceName()) }
    }

    private fun updateStatusUI(isConnected: Boolean, deviceName: String?) {
        if (isConnected) {
            val name = deviceName ?: getString(R.string.status_unknown_device)
            tvDeviceName.text = getString(R.string.device_name_status, name, getString(R.string.status_connected_label))
            tvDeviceName.setTextColor(getColor(R.color.status_connected))
            tvDeviceAction.text = getString(R.string.disconnect_device)
            enableAllButtons()
        } else {
            val lastName = hidService?.getLastConnectedDeviceName()
            if (lastName != null) {
                tvDeviceName.text = getString(R.string.device_name_status, lastName, getString(R.string.status_disconnected_label))
            } else {
                tvDeviceName.text = ""
            }
            tvDeviceName.setTextColor(getColor(R.color.status_disconnected))
            tvDeviceAction.text = getString(R.string.connect_device)
            disableAllButtons()
        }
        // Update keyboard fragment enabled state
        updateKeyboardEnabled()
    }

    private fun enableAllButtons() {
        btnYes.isEnabled = true; btnNo.isEnabled = true; btnCtrlC.isEnabled = true
        btnYesToAll.isEnabled = true; inputText.isEnabled = true; inputLayout.isEnabled = true
    }

    private fun disableAllButtons() {
        btnYes.isEnabled = false; btnNo.isEnabled = false; btnCtrlC.isEnabled = false
        btnYesToAll.isEnabled = false; inputText.isEnabled = false; inputLayout.isEnabled = false
    }

    private fun showEditMacroDialog(macro: Macro) {
        val dialog = MacroEditDialogFragment.newInstance(macro)
        dialog.setOnSaveListener { id, label, command, sendEnter ->
            if (id != null) { macroRepository.updateCustomMacro(id, label, command, sendEnter); Toast.makeText(this, R.string.toast_macro_updated, Toast.LENGTH_SHORT).show() }
            loadMacros()
        }
        dialog.setOnDeleteListener { id -> showDeleteConfirmationDialog(id) }
        dialog.show(supportFragmentManager, "edit_macro")
    }

    private fun showAddMacroDialog() {
        val dialog = MacroEditDialogFragment.newInstance()
        dialog.setOnSaveListener { _, label, command, sendEnter ->
            macroRepository.addCustomMacro(label, command, sendEnter)
            Toast.makeText(this, R.string.toast_macro_added, Toast.LENGTH_SHORT).show()
            loadMacros()
        }
        dialog.show(supportFragmentManager, "add_macro")
    }

    private fun showDeleteConfirmationDialog(macroId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_macro)
            .setMessage(R.string.dialog_delete_macro_message)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                macroRepository.deleteCustomMacro(macroId)
                Toast.makeText(this, R.string.toast_macro_deleted, Toast.LENGTH_SHORT).show()
                loadMacros()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val tvThemeMode = dialogView.findViewById<TextView>(R.id.tv_theme_mode)
        val tvResetMacros = dialogView.findViewById<TextView>(R.id.tv_reset_macros)

        val prefs = getSharedPreferences(Constants.PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)
        val currentTheme = prefs.getInt(Constants.KEY_THEME_MODE, Constants.THEME_FOLLOW_SYSTEM)
        tvThemeMode.text = getThemeModeName(currentTheme)

        tvThemeMode.setOnClickListener {
            showThemeSelectionDialog(tvThemeMode)
        }

        tvResetMacros.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_reset_macros)
                .setMessage(R.string.dialog_reset_macros_confirm)
                .setPositiveButton(R.string.dialog_reset) { _, _ ->
                    macroRepository.resetToDefaults()
                    loadMacros()
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setView(dialogView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showThemeSelectionDialog(tvThemeMode: TextView) {
        val themes = arrayOf(
            getString(R.string.theme_follow_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )

        val prefs = getSharedPreferences(Constants.PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)
        val currentTheme = prefs.getInt(Constants.KEY_THEME_MODE, Constants.THEME_FOLLOW_SYSTEM)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_theme_mode)
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                prefs.edit().putInt(Constants.KEY_THEME_MODE, which).apply()
                tvThemeMode.text = getThemeModeName(which)
                AppCompatDelegate.setDefaultNightMode(when (which) {
                    Constants.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    Constants.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                })
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showDeviceListDialog() {
        val service = hidService ?: return
        val dialog = DeviceListBottomSheetFragment.newInstance(
            connectedAddress = null,
            lastConnectedAddress = service.getLastConnectedDeviceAddress()
        )
        dialog.setDeviceSelectionListener(object : DeviceListBottomSheetFragment.DeviceSelectionListener {
            override fun onDeviceSelected(device: android.bluetooth.BluetoothDevice) {
                Thread {
                    service.connectToDevice(device.address)
                }.start()
            }
        })
        deviceListDialog = dialog
        dialog.show(supportFragmentManager, "device_list")
    }
}
