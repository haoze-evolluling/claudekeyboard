package com.haoze.claudekeyboard

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haoze.claudekeyboard.bluetooth.BluetoothHidService
import com.haoze.claudekeyboard.bluetooth.BluetoothPermissionHelper
import com.haoze.claudekeyboard.bluetooth.HidReportHelper
import com.haoze.claudekeyboard.macro.Macro
import com.haoze.claudekeyboard.macro.MacroRepository
import com.haoze.claudekeyboard.ui.macro.MacroButtonAdapter
import com.haoze.claudekeyboard.ui.macro.MacroEditDialogFragment

/**
 * Main Activity - Landscape control panel for Claude Code Bluetooth HID Keyboard.
 */
class MainActivity : AppCompatActivity() {

    // Bluetooth HID service
    private var hidService: BluetoothHidService? = null
    private var isBound = false

    // Macro repository
    private lateinit var macroRepository: MacroRepository

    // UI components
    private lateinit var statusIcon: ImageView
    private lateinit var statusText: TextView
    private lateinit var btnDisconnect: Button
    private lateinit var btnSettings: Button
    private lateinit var btnYes: Button
    private lateinit var btnNo: Button
    private lateinit var btnCtrlC: Button
    private lateinit var btnYesToAll: Button
    private lateinit var inputText: EditText
    private lateinit var btnSend: Button
    private lateinit var macroRecyclerView: RecyclerView

    // Macro adapter
    private lateinit var macroAdapter: MacroButtonAdapter

    // Service connection
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
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize
        macroRepository = MacroRepository(this)
        initViews()
        setupCoreButtons()
        setupMacroRecyclerView()
        setupTextInput()
        checkBluetoothSupport()
        requestBluetoothPermissions()
        startAndBindHidService()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindHidService()
    }

    /**
     * Initialize all views.
     */
    private fun initViews() {
        statusIcon = findViewById(R.id.status_icon)
        statusText = findViewById(R.id.status_text)
        btnDisconnect = findViewById(R.id.btn_disconnect)
        btnSettings = findViewById(R.id.btn_settings)
        btnYes = findViewById(R.id.btn_yes)
        btnNo = findViewById(R.id.btn_no)
        btnCtrlC = findViewById(R.id.btn_ctrl_c)
        btnYesToAll = findViewById(R.id.btn_yes_to_all)
        inputText = findViewById(R.id.et_input_text)
        btnSend = findViewById(R.id.btn_send)
        macroRecyclerView = findViewById(R.id.rv_macros)

        // Setup disconnect button
        btnDisconnect.setOnClickListener {
            hidService?.disconnect()
        }

        // Setup settings button
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    /**
     * Setup core button click handlers.
     */
    private fun setupCoreButtons() {
        // Yes button: send "y" + Enter
        btnYes.setOnClickListener {
            hidService?.let { service ->
                Thread {
                    service.sendText("y")
                    service.sendKeyPress(0x00, HidReportHelper.KEY_ENTER)
                }.start()
            }
        }

        // No button: send "n" + Enter
        btnNo.setOnClickListener {
            hidService?.let { service ->
                Thread {
                    service.sendText("n")
                    service.sendKeyPress(0x00, HidReportHelper.KEY_ENTER)
                }.start()
            }
        }

        // Ctrl+C button: send Ctrl+C
        btnCtrlC.setOnClickListener {
            hidService?.sendKeyPress(
                HidReportHelper.MODIFIER_CTRL_LEFT,
                HidReportHelper.KEY_C
            )
        }

        // Yes to All button: send "y!" + Enter
        btnYesToAll.setOnClickListener {
            hidService?.let { service ->
                Thread {
                    service.sendText("y!")
                    service.sendKeyPress(0x00, HidReportHelper.KEY_ENTER)
                }.start()
            }
        }
    }

    /**
     * Setup macro RecyclerView.
     */
    private fun setupMacroRecyclerView() {
        macroAdapter = MacroButtonAdapter(
            onMacroClick = { macro ->
                // Send macro command
                hidService?.let { service ->
                    Thread {
                        service.sendMacro(macro.command)
                    }.start()
                }
            },
            onMacroLongClick = { macro ->
                // Show edit dialog for custom macros
                if (!macro.isPreset) {
                    showEditMacroDialog(macro)
                }
            }
        )

        macroRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = macroAdapter
        }

        // Add "Add custom macro" button to the list
        loadMacros()
    }

    /**
     * Load macros and update RecyclerView.
     */
    private fun loadMacros() {
        val macros = macroRepository.getAllMacros().toMutableList()
        // Add "Add custom macro" entry at the end
        macros.add(
            Macro(
                id = "add_custom",
                label = getString(R.string.btn_add_macro),
                command = "",
                isPreset = true,
                sortOrder = Int.MAX_VALUE
            )
        )
        macroAdapter.submitList(macros)
    }

    /**
     * Setup text input with system IME.
     */
    private fun setupTextInput() {
        // Send button: send text via HID then clear
        btnSend.setOnClickListener {
            sendInputText()
        }

        // IME action (Enter key on soft keyboard): send text via HID
        inputText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendInputText()
                true
            } else {
                false
            }
        }
    }

    /**
     * Send the current input text via Bluetooth HID and clear the field.
     */
    private fun sendInputText() {
        val text = inputText.text.toString()
        if (text.isNotEmpty()) {
            hidService?.let { service ->
                Thread {
                    service.sendText(text)
                    service.sendKeyPress(0x00, HidReportHelper.KEY_ENTER)
                }.start()
            }
            inputText.text.clear()
        }
    }

    /**
     * Show soft keyboard for the input field.
     */
    private fun showSoftKeyboard() {
        inputText.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(inputText, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Check if device supports Bluetooth HID.
     */
    private fun checkBluetoothSupport() {
        if (!BluetoothPermissionHelper.isBleHidSupported(this)) {
            updateStatusUI(false, getString(R.string.status_not_supported))
            disableAllButtons()
            Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Request Bluetooth permissions.
     */
    private fun requestBluetoothPermissions() {
        if (!BluetoothPermissionHelper.hasRequiredPermissions(this)) {
            BluetoothPermissionHelper.requestPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BluetoothPermissionHelper.REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (!BluetoothPermissionHelper.onRequestPermissionsResult(
                    requestCode,
                    permissions,
                    grantResults
                )
            ) {
                Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
                updateStatusUI(false, getString(R.string.status_disconnected))
            }
        }
    }

    /**
     * Start and bind to the BluetoothHidService.
     */
    private fun startAndBindHidService() {
        val intent = Intent(this, BluetoothHidService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Unbind from the BluetoothHidService.
     */
    private fun unbindHidService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    /**
     * Setup service callbacks.
     */
    private fun setupServiceCallbacks() {
        hidService?.setOnConnectionStateChangedListener { isConnected, deviceName ->
            runOnUiThread {
                updateStatusUI(isConnected, deviceName)
            }
        }

        hidService?.setOnRegistrationStateChangedListener { isRegistered ->
            runOnUiThread {
                if (!isRegistered) {
                    Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Update UI based on current state
        hidService?.let { service ->
            updateStatusUI(service.isConnected(), service.getConnectedDeviceName())
        }
    }

    /**
     * Update status UI based on connection state.
     */
    private fun updateStatusUI(isConnected: Boolean, deviceName: String?) {
        if (isConnected) {
            statusText.text = getString(R.string.status_connected, deviceName ?: getString(R.string.status_unknown_device))
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
            btnDisconnect.visibility = View.VISIBLE
            enableAllButtons()
        } else {
            statusText.text = getString(R.string.status_waiting)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_waiting))
            btnDisconnect.visibility = View.GONE
        }
    }

    /**
     * Enable all buttons.
     */
    private fun enableAllButtons() {
        btnYes.isEnabled = true
        btnNo.isEnabled = true
        btnCtrlC.isEnabled = true
        btnYesToAll.isEnabled = true
        inputText.isEnabled = true
        btnSend.isEnabled = true
    }

    /**
     * Disable all buttons.
     */
    private fun disableAllButtons() {
        btnYes.isEnabled = false
        btnNo.isEnabled = false
        btnCtrlC.isEnabled = false
        btnYesToAll.isEnabled = false
        inputText.isEnabled = false
        btnSend.isEnabled = false
    }

    /**
     * Show edit macro dialog.
     */
    private fun showEditMacroDialog(macro: Macro) {
        val dialog = MacroEditDialogFragment.newInstance(macro)
        dialog.setOnSaveListener { id, label, command ->
            if (id != null) {
                macroRepository.updateCustomMacro(id, label, command)
                Toast.makeText(this, R.string.toast_macro_updated, Toast.LENGTH_SHORT).show()
            }
            loadMacros()
        }
        dialog.setOnDeleteListener { id ->
            showDeleteConfirmationDialog(id)
        }
        dialog.show(supportFragmentManager, "edit_macro")
    }

    /**
     * Show add macro dialog.
     */
    private fun showAddMacroDialog() {
        val dialog = MacroEditDialogFragment.newInstance()
        dialog.setOnSaveListener { _, label, command ->
            macroRepository.addCustomMacro(label, command)
            Toast.makeText(this, R.string.toast_macro_added, Toast.LENGTH_SHORT).show()
            loadMacros()
        }
        dialog.show(supportFragmentManager, "add_macro")
    }

    /**
     * Show delete confirmation dialog.
     */
    private fun showDeleteConfirmationDialog(macroId: String) {
        AlertDialog.Builder(this)
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

    /**
     * Show settings dialog.
     */
    private fun showSettingsDialog() {
        val options = arrayOf(
            getString(R.string.settings_disconnect),
            getString(R.string.settings_reset_macros)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> hidService?.disconnect()
                    1 -> {
                        macroRepository.resetToDefaults()
                        loadMacros()
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}