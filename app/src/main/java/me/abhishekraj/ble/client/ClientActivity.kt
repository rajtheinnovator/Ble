package me.abhishekraj.ble.client

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.*
import android.os.Handler
import android.os.ParcelUuid
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.databinding.DataBindingUtil
import me.abhishekraj.ble.Constants.SCAN_PERIOD
import me.abhishekraj.ble.Constants.SERVICE_UUID
import me.abhishekraj.ble.R
import me.abhishekraj.ble.databinding.ActivityClientBinding
import me.abhishekraj.ble.databinding.ViewGattServerBinding


class ClientActivity : AppCompatActivity(), GattClientActionListener {

    override fun log(message: String) {

    }

    override fun logError(message: String) {
        Log.e("my_tag", "Error: " + message);
    }

    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_FINE_LOCATION = 2
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanCallback: ScanCallback? = null
    private var scanResults: Map<String, BluetoothDevice>? = null
    private var gatt: BluetoothGatt? = null

    private var scanning: Boolean? = false
    private var handler: Handler? = null

    private var connected: Boolean = false;
    private var timeInitialized: Boolean = false;
    private var echoInitialized: Boolean = false;
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    private var binding: ActivityClientBinding? = null

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_client)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        @SuppressLint("HardwareIds")
        val deviceInfo = ("Device Info"
                + "\nName: " + bluetoothAdapter!!.getName()
                + "\nAddress: " + bluetoothAdapter!!.getAddress())

        scanCallback = BtLeScanCallback(scanResults as MutableMap<String, BluetoothDevice>);
        hasBluetoothTurnedOn()
    }

    override fun onResume() {
        super.onResume()

        // Check low energy support
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e("my_tag", "No LE Support.")
            finish()
        }
    }

    private fun hasBluetoothTurnedOn(): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled()) {
            requestBluetoothEnable()
            return false
        } else if (!hasLocationPermissions()) {
            requestLocationPermission()
            return false
        }
        return true
    }

    private fun requestBluetoothEnable() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT)
        Log.d("my_tag", "Requested user enables Bluetooth. Try starting the scan again.")
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun hasLocationPermissions(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestLocationPermission() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(permissions[i])) {
                        AlertDialog.Builder(this)
                            .setMessage("Allow permission ${permissions[i]} to make the app work smoothly")
                            .setPositiveButton("Allow", { dialog, which -> requestLocationPermission() })
                            .setNegativeButton("Cancel", { dialog, which -> dialog.dismiss() })
                            .create()
                            .show()
                    }
                    return
                }
            }
            //permission available, continue
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class BtLeScanCallback internal constructor(private val mScanResults: MutableMap<String, BluetoothDevice>) :
        ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("my_tag", "BLE Scan Failed with code $errorCode")
        }

        private fun addScanResult(result: ScanResult) {
            val device = result.getDevice()
            val deviceAddress = device.getAddress()
            mScanResults[deviceAddress] = device
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun startScan() {
        if (!hasBluetoothTurnedOn() || scanning!!) {
            return
        }

        disconnectGattServer()

        binding?.serverListContainer?.removeAllViews()

        scanResults = HashMap()
        scanCallback = BtLeScanCallback(scanResults as HashMap<String, BluetoothDevice>)

        bluetoothLeScanner = bluetoothAdapter?.getBluetoothLeScanner()

        // Note: Filtering does not work the same (or at all) on most devices. It also is unable to
        // search for a mask or anything less than a full UUID.
        // Unless the full UUID of the server is known, manual filtering may be necessary.
        // For example, when looking for a brand of device that contains a char sequence in the UUID
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val filters = ArrayList<ScanFilter>()
        filters.add(scanFilter)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        bluetoothLeScanner?.startScan(filters, settings, scanCallback)

        handler = Handler()
        handler!!.postDelayed({ this.stopScan() }, SCAN_PERIOD)

        scanning = true
        Log.d("my_tag", "Started scanning.")
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun stopScan() {
        if (scanning!! && bluetoothAdapter != null && bluetoothAdapter!!.isEnabled() && bluetoothLeScanner != null) {
            bluetoothLeScanner?.stopScan(scanCallback)
            scanComplete()
        }

        scanCallback = null
        scanning = false
        handler = null
        Log.d("my_tag", "Stopped scanning.")
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun scanComplete() {
        if (scanResults?.isEmpty()!!) {
            return
        }

        for (deviceAddress in scanResults!!.keys) {
            val device = scanResults!!.get(deviceAddress)
            val viewModel = GattServerViewModel(device)

            val binding: ViewGattServerBinding = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.view_gatt_server,
                binding?.serverListContainer,
                true
            )
            binding.setViewModel(viewModel)
            binding.connectGattServerButton.setOnClickListener({ connectDevice(device!!) })
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun connectDevice(device: BluetoothDevice) {
        Log.d("my_tag", "Connecting to " + device.address)
        val gattClientCallback = GattClientCallback(this)
        gatt = device.connectGatt(this, false, gattClientCallback)
    }

    override fun setConnected(connectedStatus: Boolean) {
        connected = connectedStatus
    }

    override fun initializeTime() {
        timeInitialized = true
    }

    override fun initializeEcho() {
        echoInitialized = true
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun disconnectGattServer() {
        Log.d("my_tag", "Closing Gatt connection")
        connected = false
        echoInitialized = false
        timeInitialized = false
        if (gatt != null) {
            gatt!!.disconnect()
            gatt!!.close()
        }
    }
}
