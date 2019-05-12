package hse.sergeeva.indoornavigation.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import hse.sergeeva.indoornavigation.R
import hse.sergeeva.indoornavigation.models.Location
import hse.sergeeva.indoornavigation.models.TowerStatistics
import hse.sergeeva.indoornavigation.models.locationManagers.LocationManagerType
import hse.sergeeva.indoornavigation.presenters.LocationScanners
import hse.sergeeva.indoornavigation.presenters.UiRunner
import io.indoorlocation.core.IndoorLocation
import io.indoorlocation.manual.ManualIndoorLocationProvider
import io.mapwize.mapwizecomponents.ui.MapwizeFragment
import io.mapwize.mapwizecomponents.ui.MapwizeFragmentUISettings
import io.mapwize.mapwizeformapbox.api.LatLngFloor
import io.mapwize.mapwizeformapbox.api.MapwizeObject
import io.mapwize.mapwizeformapbox.map.MapOptions
import io.mapwize.mapwizeformapbox.map.MapwizePlugin
import io.mapwize.mapwizeformapbox.map.Marker
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, ILocationActivity,
    MapwizeFragment.OnFragmentInteractionListener {

    private lateinit var navigationMethodsSpinner: Spinner
    private var locationScanner: LocationScanners? = null

    private var mapwizeFragment: MapwizeFragment? = null
    private var map: MapboxMap? = null
    private var mapwizePlugin: MapwizePlugin? = null
    private var locationProvider: ManualIndoorLocationProvider? = null

    private var marker: Marker? = null
    private var allTowers: TextView? = null
    private var cellId: TextView? = null
    private var myLinkov: TextView? = null
    private val mapViewBundleKey = "MapViewBundleKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigationMethodsSpinner = findViewById(R.id.navigation_methods)
        ArrayAdapter.createFromResource(this, R.array.indoor_navigation_methods, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                navigationMethodsSpinner.adapter = adapter
            }

        val opts = MapOptions.Builder()
            .restrictContentToVenue("5cd6f30c7b40c40016e4e85b")
            .centerOnVenue("5cd6f30c7b40c40016e4e85b")
            .build()

        val uiSettings = MapwizeFragmentUISettings.Builder()
            .menuButtonHidden(true)
            .followUserButtonHidden(true)
            .floorControllerHidden(false)
            .compassHidden(false)
            .build()

        mapwizeFragment = MapwizeFragment.newInstance(opts, uiSettings)
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.add(fragmentContainer.id, mapwizeFragment!!)
        ft.commit()

        allTowers = findViewById(R.id.allTowers)
        cellId = findViewById(R.id.cellIdTowers)
        myLinkov = findViewById(R.id.myLinkovTowers)

        checkPermissions()
        UiRunner.activity = this
    }

    override fun onFragmentReady(mapboxMap: MapboxMap?, mapwizePlugin: MapwizePlugin?) {
        this.map = mapboxMap

        this.mapwizePlugin = mapwizePlugin
        val nn = LatLng(56.268416, 43.877797)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(nn, 16.0))

        this.locationProvider = ManualIndoorLocationProvider()
        this.mapwizePlugin?.setLocationProvider(this.locationProvider!!)

        locationScanner = LocationScanners(applicationContext, this)
        locationScanner!!.scanLocation()
        navigationMethodsSpinner.onItemSelectedListener = this
    }

    override fun onMenuButtonClick() {

    }

    override fun onInformationButtonClick(mapwizeObject: MapwizeObject?) {

    }

    override fun onFollowUserButtonClickWithoutLocation() {
        Log.i("Debug", "onFollowUserButtonClickWithoutLocation")
    }

    override fun shouldDisplayInformationButton(mapwizeObject: MapwizeObject?): Boolean {
        return false
    }

    override fun shouldDisplayFloorController(floors: MutableList<Double>?): Boolean {
        Log.i("Debug", "shouldDisplayFloorController")
        if (floors == null || floors.size <= 1) {
            return false
        }
        return true
    }


    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        var mapViewBundle = outState!!.getBundle(mapViewBundleKey)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(mapViewBundleKey, mapViewBundle)
        }
    }

    override fun onResume() {
        super.onResume()

        UiRunner.activity = this
        locationScanner?.scanLocation()
    }

    override fun onStart() {
        super.onStart()

        UiRunner.activity = this
        locationScanner?.scanLocation()
    }

    override fun onStop() {
        locationScanner?.stopScanning()
        UiRunner.activity = null

        super.onStop()
    }

    override fun onPause() {
        locationScanner?.stopScanning()
        UiRunner.activity = null

        super.onPause()
    }

    override fun onDestroy() {
        locationScanner?.stopScanning()
        UiRunner.activity = null

        super.onDestroy()
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

    override fun updateLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        val indoorLocation = IndoorLocation(
            "manual_provider",
            location.latitude,
            location.longitude,
            location.floor.toDouble(),
            System.currentTimeMillis()
        )
        this.locationProvider?.setIndoorLocation(indoorLocation)
    }

    override fun setMarker(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        mapwizePlugin?.addMarker(LatLngFloor(latLng))
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
