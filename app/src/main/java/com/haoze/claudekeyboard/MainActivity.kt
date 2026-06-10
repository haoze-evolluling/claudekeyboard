package com.haoze.claudekeyboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.haoze.claudekeyboard.bluetooth.BluetoothHidService
import com.haoze.claudekeyboard.bluetooth.KeyboardSender
import com.haoze.claudekeyboard.macro.Macro
import com.haoze.claudekeyboard.macro.MacroRepository
import com.haoze.claudekeyboard.ui.macro.MacroButtonAdapter
import com.haoze.claudekeyboard.ui.macro.MacroEditDialogFragment

class MainActivity : AppCompatActivity() {

    private var hidService: BluetoothHidService? = null
    private var isBound = false
    private lateinit var macroRepository: MacroRepository

    // UI components
    private lateinit var statusChip: Chip
    private lateinit var btnDisconnect: MaterialButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnYes: MaterialButton
    private lateinit var btnNo: MaterialButton
    private lateinit var btnCtrlC: MaterialButton
    private lateinit var btnYesToAll: MaterialButton
    private lateinit var inputLayout: TextInputLayout
    private lateinit var inputText: TextInputEditText
    private lateinit var macroRecyclerView: RecyclerView
    private lateinit var macroAdapter: MacroButtonAdapter

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

        setupWindowInsets()

        macroRepository = MacroRepository(this)
        initViews()
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
            val bottom = maxOf(systemBars.bottom, cutout?.safeInsetBottom ?: 0)
            v.setPadding(left, top, right, bottom)
            insets
        }
    }

    private fun initViews() {
        statusChip = findViewById(R.id.status_chip)
        btnDisconnect = findViewById(R.id.btn_disconnect)
        btnSettings = findViewById(R.id.btn_settings)
        btnYes = findViewById(R.id.btn_yes)
        btnNo = findViewById(R.id.btn_no)
        btnCtrlC = findViewById(R.id.btn_ctrl_c)
        btnYesToAll = findViewById(R.id.btn_yes_to_all)
        inputLayout = findViewById(R.id.til_input)
        inputText = findViewById(R.id.et_input_text)
        macroRecyclerView = findViewById(R.id.rv_macros)

        btnDisconnect.setOnClickListener {
            val service = hidService ?: return@setOnClickListener
            if (service.isConnected()) {
                service.disconnect()
            } else if (!service.connectToLastDevice()) {
                Toast.makeText(this, R.string.toast_connect_failed, Toast.LENGTH_SHORT).show()
            }
        }
        btnSettings.setOnClickListener { showSettingsDialog() }
    }

    private fun setupCoreButtons() {
        btnYes.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("y"); s.sendKeyPress(0x00, KeyboardSender.KEY_ENTER) }.start() }
        }
        btnNo.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("n"); s.sendKeyPress(0x00, KeyboardSender.KEY_ENTER) }.start() }
        }
        btnCtrlC.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.KEY_C) }.start() }
        }
        btnYesToAll.setOnClickListener {
            hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("y!"); s.sendKeyPress(0x00, KeyboardSender.KEY_ENTER) }.start() }
        }
    }

    private fun setupMacroRecyclerView() {
        macroAdapter = MacroButtonAdapter(
            onMacroClick = { macro ->
                hidService?.getKeyboardSender()?.let { s -> Thread { s.sendMacro(macro.command) }.start() }
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
            runOnUiThread { updateStatusUI(isConnected, deviceName) }
        }
        hidService?.setOnRegistrationStateChangedListener { isRegistered ->
            runOnUiThread { if (!isRegistered) Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_SHORT).show() }
        }
        hidService?.let { updateStatusUI(it.isConnected(), it.getConnectedDeviceName()) }
    }

    private fun updateStatusUI(isConnected: Boolean, deviceName: String?) {
        if (isConnected) {
            statusChip.text = getString(R.string.status_connected, deviceName ?: getString(R.string.status_unknown_device))
            statusChip.setChipBackgroundColorResource(R.color.status_connected_bg)
            statusChip.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
            btnDisconnect.text = getString(R.string.settings_disconnect)
            btnDisconnect.visibility = View.VISIBLE
            enableAllButtons()
        } else {
            val lastName = hidService?.getLastConnectedDeviceName()
            if (lastName != null) {
                statusChip.text = getString(R.string.status_last_device, lastName)
            } else {
                statusChip.text = getString(R.string.status_waiting)
            }
            statusChip.setChipBackgroundColorResource(R.color.chip_bg)
            statusChip.setTextColor(ContextCompat.getColor(this, R.color.chip_text))
            btnDisconnect.text = getString(R.string.btn_connect)
            btnDisconnect.visibility = if (hidService?.hasLastConnectedDevice() == true) View.VISIBLE else View.GONE
            disableAllButtons()
        }
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
        dialog.setOnSaveListener { id, label, command ->
            if (id != null) { macroRepository.updateCustomMacro(id, label, command); Toast.makeText(this, R.string.toast_macro_updated, Toast.LENGTH_SHORT).show() }
            loadMacros()
        }
        dialog.setOnDeleteListener { id -> showDeleteConfirmationDialog(id) }
        dialog.show(supportFragmentManager, "edit_macro")
    }

    private fun showAddMacroDialog() {
        val dialog = MacroEditDialogFragment.newInstance()
        dialog.setOnSaveListener { _, label, command ->
            macroRepository.addCustomMacro(label, command)
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
        val options = arrayOf(getString(R.string.settings_disconnect), getString(R.string.settings_reset_macros))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> hidService?.disconnect()
                    1 -> { macroRepository.resetToDefaults(); loadMacros() }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
