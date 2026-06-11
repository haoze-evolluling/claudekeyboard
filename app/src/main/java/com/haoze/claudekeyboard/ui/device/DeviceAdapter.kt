package com.haoze.claudekeyboard.ui.device

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.haoze.claudekeyboard.R

/**
 * Adapter for displaying paired Bluetooth devices in a list.
 */
class DeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private var devices: List<BluetoothDevice> = emptyList()
    private var connectedAddress: String? = null
    private var lastConnectedAddress: String? = null
    private var connectingAddress: String? = null

    /**
     * Update the device list and status.
     */
    fun updateDevices(devices: List<BluetoothDevice>, connectedAddress: String?, lastConnectedAddress: String?) {
        this.devices = devices
        this.connectedAddress = connectedAddress
        this.lastConnectedAddress = lastConnectedAddress
        notifyDataSetChanged()
    }

    /**
     * Set the address of the device currently being connected.
     * Pass null to clear the connecting state.
     */
    fun setConnecting(address: String?) {
        this.connectingAddress = address
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_device_name)
        private val tvAddress: TextView = itemView.findViewById(R.id.tv_device_address)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_device_status)
        private val progressConnecting: LinearProgressIndicator = itemView.findViewById(R.id.progress_connecting)

        fun bind(device: BluetoothDevice) {
            tvName.text = device.name ?: itemView.context.getString(R.string.status_unknown_device)
            tvAddress.text = device.address

            val isConnecting = device.address == connectingAddress

            when {
                isConnecting -> {
                    tvStatus.visibility = View.GONE
                    progressConnecting.visibility = View.VISIBLE
                    itemView.isEnabled = false
                    itemView.alpha = 1.0f
                }
                connectingAddress != null -> {
                    // Another device is connecting, disable all items
                    tvStatus.visibility = View.GONE
                    progressConnecting.visibility = View.GONE
                    itemView.isEnabled = false
                    itemView.alpha = 0.5f
                }
                device.address == connectedAddress -> {
                    tvStatus.text = itemView.context.getString(R.string.device_connected)
                    tvStatus.visibility = View.VISIBLE
                    progressConnecting.visibility = View.GONE
                    itemView.isEnabled = false
                    itemView.alpha = 0.6f
                }
                device.address == lastConnectedAddress -> {
                    tvStatus.text = itemView.context.getString(R.string.device_last_connected)
                    tvStatus.visibility = View.VISIBLE
                    progressConnecting.visibility = View.GONE
                    itemView.isEnabled = true
                    itemView.alpha = 1.0f
                }
                else -> {
                    tvStatus.visibility = View.GONE
                    progressConnecting.visibility = View.GONE
                    itemView.isEnabled = true
                    itemView.alpha = 1.0f
                }
            }

            itemView.setOnClickListener {
                if (device.address != connectedAddress && connectingAddress == null) {
                    onDeviceClick(device)
                }
            }
        }
    }
}
