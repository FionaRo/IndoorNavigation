package hse.sergeeva.indoornavigation.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import hse.sergeeva.indoornavigation.R
import hse.sergeeva.indoornavigation.models.Location
import hse.sergeeva.indoornavigation.models.TowerStatistics
import hse.sergeeva.indoornavigation.models.locationManagers.LocationManagerType
import hse.sergeeva.indoornavigation.presenters.LocationScanners

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, ILocationActivity, OnMapReadyCallback {

    private lateinit var navigationMethodsSpinner: Spinner
    private var locationScanner: LocationScanners? = null
    private lateinit var map: GoogleMap
    private lateinit var mapView: MapView
    private var marker: Marker? = null
    private var allTowers: TextView? = null
    private var cellId: TextView? = null
    private var myLinkov: TextView? = null
    private val mapViewBundleKey = "MapViewBundleKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigationMethodsSpinner = findViewById(R.id.naviation_methods)
        ArrayAdapter.createFromResource(this, R.array.indoor_navigation_methods, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                navigationMethodsSpinner.adapter = adapter
            }

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) mapViewBundle = savedInstanceState.getBundle(mapViewBundleKey)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        allTowers = findViewById(R.id.allTowers)
        cellId = findViewById(R.id.cellIdTowers)
        myLinkov = findViewById(R.id.myLinkovTowers)

        checkPermissions()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        var mapViewBundle = outState!!.getBundle(mapViewBundleKey)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(mapViewBundleKey, mapViewBundle)
        }

        mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onResume() {
        super.onResume()

        mapView.onResume()
        locationScanner?.scanLocation()
    }

    override fun onStart() {
        super.onStart()

        mapView.onStart()
        locationScanner?.scanLocation()
    }

    override fun onStop() {
        locationScanner?.stopScanning()
        mapView.onStop()

        super.onStop()
    }

    override fun onPause() {
        locationScanner?.stopScanning()
        mapView.onPause()

        super.onPause()
    }

    override fun onDestroy() {
        locationScanner?.stopScanning()
        mapView.onDestroy()

        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        mapView.onLowMemory()
    }

    private fun checkPermissions() {
        val permission: Array<String> = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
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
        when (position) {
            0 -> locationScanner!!.changeLocationManager(LocationManagerType.WiFi)
            1 -> locationScanner!!.changeLocationManager(LocationManagerType.CellId)
            2 -> locationScanner!!.changeLocationManager(LocationManagerType.Beacons)
            3 -> locationScanner!!.changeLocationManager(LocationManagerType.Vlc)
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        this.map = map!!
        locationScanner = LocationScanners(applicationContext, this)
        locationScanner!!.scanLocation()
        navigationMethodsSpinner.onItemSelectedListener = this

        val nn = LatLng(56.267762, 43.8770023)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(nn, 12f))
    }

    override fun updateLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        if (marker == null) {
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            marker = map.addMarker(markerOptions)
        } else {
            marker!!.position = latLng
        }
    }

    override fun setMarker(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        map.addMarker(markerOptions)
    }

    override fun updateData() {
        allTowers?.text = "All Towers: ${TowerStatistics.allTowers}"
        cellId?.text =
            "CellId. Found: ${TowerStatistics.foundTowersOpenCellId}. " +
                    "Not found: ${TowerStatistics.notFoundTowersOpenCellId}. " +
                    "Duplicate: ${TowerStatistics.duplicateCellId}"
        myLinkov?.text =
            "MyLinkov. Found: ${TowerStatistics.foundTowersMylnikov}. " +
                    "Not found: ${TowerStatistics.notFoundTowersMylnikov}. " +
                    "Duplicate: ${TowerStatistics.duplicateMyLinkov}"
    }

}
