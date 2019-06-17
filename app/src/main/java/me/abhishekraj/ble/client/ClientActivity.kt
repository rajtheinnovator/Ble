package me.abhishekraj.ble.client

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
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
import me.abhishekraj.ble.R

class ClientActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_FINE_LOCATION = 2
    private var bluetoothAdapter: BluetoothAdapter? = null

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

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
        Log.d("my_tag","Requested user enables Bluetooth. Try starting the scan again.")
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
}
