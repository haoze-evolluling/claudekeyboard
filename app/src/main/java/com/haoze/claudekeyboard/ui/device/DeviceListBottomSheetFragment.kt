package com.haoze.claudekeyboard.ui.device

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.util.fixM3Background

/**
 * BottomSheet dialog showing paired Bluetooth devices for connection.
 */
class DeviceListBottomSheetFragment : BottomSheetDialogFragment() {

    interface DeviceSelectionListener {
        fun onDeviceSelected(device: BluetoothDevice)
        fun onDisconnectRequested(device: BluetoothDevice)
        fun onConnectionCancelled()
    }

    private var listener: DeviceSelectionListener? = null
    private var connectedAddress: String? = null
    private var lastConnectedAddress: String? = null
    private var adapter: DeviceAdapter? = null
    private var connectingAddress: String? = null

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { onConnectionTimeout() }

    companion object {
        private const val ARG_CONNECTED_ADDRESS = "connected_address"
        private const val ARG_LAST_CONNECTED_ADDRESS = "last_connected_address"
        private const val CONNECTION_TIMEOUT_MS = 10_000L

        fun newInstance(
            connectedAddress: String?,
            lastConnectedAddress: String?
        ): DeviceListBottomSheetFragment {
            return DeviceListBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONNECTED_ADDRESS, connectedAddress)
                    putString(ARG_LAST_CONNECTED_ADDRESS, lastConnectedAddress)
                }
            }
        }
    }

    fun setDeviceSelectionListener(listener: DeviceSelectionListener) {
        this.listener = listener
    }

    override fun onStart() {
        super.onStart()
        val dlg = dialog ?: return
        val bgColor = ContextCompat.getColor(requireContext(), R.color.home_card)
        val cornerRadius = 12f * resources.displayMetrics.density // 12dp in pixels
        // Fix bottom sheet background only (not the entire window)
        dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(bgColor)
                this.cornerRadius = cornerRadius
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectedAddress = arguments?.getString(ARG_CONNECTED_ADDRESS)
        lastConnectedAddress = arguments?.getString(ARG_LAST_CONNECTED_ADDRESS)

        // Handle back press during connection - cancel and disconnect
        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (connectingAddress != null) {
                        listener?.onConnectionCancelled()
                        connectingAddress = null
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                    }
                    dismiss()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_device_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tv_device_list_title)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_device_empty)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_devices)

        tvTitle.text = getString(R.string.device_list_title)

        adapter = DeviceAdapter(
            onDeviceClick = { device -> startConnection(device) },
            onDisconnectClick = { device -> listener?.onDisconnectRequested(device) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Enforce max height for RecyclerView (android:maxHeight doesn't work natively)
        recyclerView.post {
            val maxHeight = (400 * resources.displayMetrics.density).toInt()
            if (recyclerView.height > maxHeight) {
                recyclerView.layoutParams = recyclerView.layoutParams.apply {
                    height = maxHeight
                }
            }
        }

        // Check BLUETOOTH_CONNECT permission before accessing bonded devices (API 31+)
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasPermission) {
            tvEmpty.text = getString(R.string.toast_permission_denied)
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        }

        try {
            val bluetoothAdapter = requireContext().getSystemService(BluetoothManager::class.java)?.adapter
            val bondedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

            if (bondedDevices.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter?.updateDevices(bondedDevices, connectedAddress, lastConnectedAddress)
            }
        } catch (e: SecurityException) {
            tvEmpty.text = getString(R.string.toast_permission_denied)
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }

    private fun startConnection(device: BluetoothDevice) {
        connectingAddress = device.address
        adapter?.setConnecting(device.address)

        // Start timeout
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, CONNECTION_TIMEOUT_MS)

        // Notify listener to initiate connection
        listener?.onDeviceSelected(device)
    }

    /**
     * Called by MainActivity when connection succeeds.
     */
    fun onConnectionSuccess() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        connectingAddress = null
        dismiss()
    }

    /**
     * Called by MainActivity when connection fails immediately.
     */
    fun onConnectionFailed() {
        if (!isAdded) return
        timeoutHandler.removeCallbacks(timeoutRunnable)
        connectingAddress = null
        adapter?.setConnecting(null)

        Toast.makeText(requireContext(), R.string.dialog_connect_timeout_message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Called when connection times out.
     */
    private fun onConnectionTimeout() {
        if (!isAdded) return
        val address = connectingAddress ?: return
        connectingAddress = null
        adapter?.setConnecting(null)

        val dlg = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_connect_timeout_title)
            .setMessage(R.string.dialog_connect_timeout_message)
            .setPositiveButton(R.string.dialog_ok, null)
            .create()
        dlg.fixM3Background()
        dlg.show()
    }
}
