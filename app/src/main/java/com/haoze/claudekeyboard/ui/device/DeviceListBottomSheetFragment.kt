package com.haoze.claudekeyboard.ui.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.haoze.claudekeyboard.R

/**
 * BottomSheet dialog showing paired Bluetooth devices for connection.
 */
class DeviceListBottomSheetFragment : BottomSheetDialogFragment() {

    interface DeviceSelectionListener {
        fun onDeviceSelected(device: BluetoothDevice)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectedAddress = arguments?.getString(ARG_CONNECTED_ADDRESS)
        lastConnectedAddress = arguments?.getString(ARG_LAST_CONNECTED_ADDRESS)
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

        adapter = DeviceAdapter { device ->
            startConnection(device)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bondedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

        if (bondedDevices.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter?.updateDevices(bondedDevices, connectedAddress, lastConnectedAddress)
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
     * Called when connection times out.
     */
    private fun onConnectionTimeout() {
        val address = connectingAddress ?: return
        connectingAddress = null
        adapter?.setConnecting(null)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_connect_timeout_title)
            .setMessage(R.string.dialog_connect_timeout_message)
            .setPositiveButton(R.string.dialog_ok, null)
            .show()
    }
}
