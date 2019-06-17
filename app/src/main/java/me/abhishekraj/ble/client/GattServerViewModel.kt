package me.abhishekraj.ble.client

import android.bluetooth.BluetoothDevice
import android.databinding.BaseObservable
import android.databinding.Bindable

class GattServerViewModel(private val mBluetoothDevice: BluetoothDevice?) : BaseObservable() {

    val serverName: String
        @Bindable
        get() = if (mBluetoothDevice == null) {
            ""
        } else mBluetoothDevice!!.address
}