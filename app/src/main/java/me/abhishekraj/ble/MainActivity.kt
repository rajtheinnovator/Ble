package me.abhishekraj.ble

import android.Manifest
import android.Manifest.permission.*
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.support.annotation.RequiresApi
import java.nio.file.Files.size
import android.support.annotation.NonNull
import android.annotation.TargetApi
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import me.abhishekraj.ble.client.ClientActivity
import me.abhishekraj.ble.server.ServerActivity


class MainActivity : AppCompatActivity() {

    var permissions: Array<String>? = null
    lateinit var buttonContainer: LinearLayout
    lateinit var clientButton: Button
    lateinit var serverButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissions = arrayOf(BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION, BLUETOOTH)

        buttonContainer = findViewById(R.id.ll_button_container)
        clientButton = findViewById(R.id.bt_client)
        serverButton = findViewById(R.id.bt_server)

        clientButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, ClientActivity::class.java))
        }
        serverButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, ServerActivity::class.java))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (arePermissionsEnabled()) {
                buttonContainer.visibility = View.VISIBLE
            } else {
                requestMultiplePermissions();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun arePermissionsEnabled(): Boolean {
        for (permission in permissions!!) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun requestMultiplePermissions() {
        val remainingPermissions = ArrayList<String>()
        for (permission in permissions!!) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                remainingPermissions.add(permission)
            }
        }
        requestPermissions(remainingPermissions.toTypedArray(), 101)
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
                            .setPositiveButton("Allow", { dialog, which -> requestMultiplePermissions() })
                            .setNegativeButton("Cancel", { dialog, which -> dialog.dismiss() })
                            .create()
                            .show()
                    }
                    return
                }
            }
            //permission available, continue
            buttonContainer.visibility = View.VISIBLE
        }
    }
}
