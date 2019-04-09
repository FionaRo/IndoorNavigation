package hse.sergeeva.indoornavigation.views

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import android.support.v4.app.ActivityCompat
import android.telephony.TelephonyManager
import android.util.Log
import com.android.volley.Response
import hse.sergeeva.indoornavigation.R
import hse.sergeeva.indoornavigation.models.locationManagers.LocationManagerType
import hse.sergeeva.indoornavigation.models.wigletApi.WigletApi
import hse.sergeeva.indoornavigation.presenters.LocationScanners

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, ILocationActivity {
    lateinit var navigationMethodsSpinner: Spinner
    lateinit var locationScanner: LocationScanners

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigationMethodsSpinner = findViewById(R.id.naviation_methods)
        ArrayAdapter.createFromResource(this, R.array.indoor_navigation_methods, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                navigationMethodsSpinner.adapter = adapter
            }
        navigationMethodsSpinner.onItemSelectedListener = this

        checkPermissions()
        locationScanner = LocationScanners(applicationContext, this)
    }

    private fun checkPermissions() {
        val permission: Array<String> = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
        )

        if (!hasPermissions(permission)) {
            ActivityCompat.requestPermissions(this, permission, 1)
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when(position) {
            0 -> locationScanner.changeLocationManager(LocationManagerType.WiFi)
            1 -> locationScanner.changeLocationManager(LocationManagerType.CellId)
            2 -> locationScanner.changeLocationManager(LocationManagerType.Beacons)
            3 -> locationScanner.changeLocationManager(LocationManagerType.Vlc)
        }
    }
}
