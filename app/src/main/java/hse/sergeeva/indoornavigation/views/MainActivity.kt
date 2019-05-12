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
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.MapView
//import com.google.android.gms.maps.OnMapReadyCallback
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.Marker
//import com.google.android.gms.maps.model.MarkerOptions
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
import io.mapwize.mapwizeformapbox.AccountManager
import io.mapwize.mapwizeformapbox.api.LatLngFloor
import io.mapwize.mapwizeformapbox.api.MapwizeObject
import io.mapwize.mapwizeformapbox.api.Place
import io.mapwize.mapwizeformapbox.map.MapOptions
import io.mapwize.mapwizeformapbox.map.MapwizePlugin
import io.mapwize.mapwizeformapbox.map.MapwizePluginFactory
import io.mapwize.mapwizeformapbox.map.Marker
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener,
    ILocationActivity, OnMapReadyCallback, MapwizeFragment.OnFragmentInteractionListener {

    private lateinit var navigationMethodsSpinner: Spinner
    private var locationScanner: LocationScanners? = null

    private var mapwizeFragment: MapwizeFragment? = null
    private var map: MapboxMap? = null
    //private lateinit var mapView: MapView
    private var mapwizePlugin: MapwizePlugin? = null
    private var locationProvider: ManualIndoorLocationProvider? = null

    private var marker: Marker? = null
    private var allTowers: TextView? = null
    private var cellId: TextView? = null
    private var myLinkov: TextView? = null
    private val mapViewBundleKey = "MapViewBundleKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val box = Mapbox.getInstance(this, "pk.mapwize")

        try {
            setContentView(R.layout.activity_main)
        } catch (ex: Exception) {
            Log.d("MainActivity", ex.message)
        } catch (ex: Error) {
            Log.d("MainActivity", ex.message)
        }
        navigationMethodsSpinner = findViewById(R.id.naviation_methods)
        ArrayAdapter.createFromResource(this, R.array.indoor_navigation_methods, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                navigationMethodsSpinner.adapter = adapter
            }


        // Uncomment and fill place holder to test MapwizeUI on your venue
        val opts = MapOptions.Builder()
            //.restrictContentToOrganization("YOUR_ORGANIZATION_ID")
            //.restrictContentToVenue("YOUR_VENUE_ID")
            //.centerOnVenue("YOUR_VENUE_ID")
            //.centerOnPlace("YOUR_PLACE_ID")
            .build()

        // Uncomment and change value to test different settings configuration
        var uiSettings = MapwizeFragmentUISettings.Builder()
            //.menuButtonHidden(true)
            //.followUserButtonHidden(false)
            //.floorControllerHidden(false)
            //.compassHidden(true)
            .build()

        mapwizeFragment = MapwizeFragment.newInstance(opts, uiSettings)
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.add(fragmentContainer.id, mapwizeFragment!!)
        ft.commit()

        //mapView = findViewById(R.id.mapView)
        //mapView.onCreate(savedInstanceState)

//        val options = MapOptions.Builder()
//            .centerOnVenue("hse_university")
//            .floor(1.0)
//            .build()
//
//        mapwizePlugin = MapwizePluginFactory.create(mapView, options)

//        var mapViewBundle: Bundle? = null
//        if (savedInstanceState != null) mapViewBundle = savedInstanceState.getBundle(mapViewBundleKey)
//        mapView.onCreate(mapViewBundle)
//        mapView.getMapAsync(this)

        allTowers = findViewById(R.id.allTowers)
        cellId = findViewById(R.id.cellIdTowers)
        myLinkov = findViewById(R.id.myLinkovTowers)

        checkPermissions()
        UiRunner.activity = this
    }

    override fun onFragmentReady(mapboxMap: MapboxMap?, mapwizePlugin: MapwizePlugin?) {
        this.map = mapboxMap
        this.mapwizePlugin = mapwizePlugin

        this.locationProvider = ManualIndoorLocationProvider()
        this.mapwizePlugin?.setLocationProvider(this.locationProvider!!)

        this.mapwizePlugin?.addOnLongClickListener {
            val indoorLocation = IndoorLocation(
                "manual_provider",
                it.latLngFloor.latitude,
                it.latLngFloor.longitude,
                it.latLngFloor.floor,
                System.currentTimeMillis()
            )
            this.locationProvider?.setIndoorLocation(indoorLocation)
        }
    }

    override fun onMenuButtonClick() {

    }

    override fun onInformationButtonClick(mapwizeObject: MapwizeObject?) {

    }

    override fun onFollowUserButtonClickWithoutLocation() {
        Log.i("Debug", "onFollowUserButtonClickWithoutLocation")
    }

    override fun shouldDisplayInformationButton(mapwizeObject: MapwizeObject?): Boolean {
        Log.i("Debug", "shouldDisplayInformationButton")
        when (mapwizeObject) {
            is Place -> return true
        }
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

        //mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onResume() {
        super.onResume()

        UiRunner.activity = this
        //mapView.onResume()
        locationScanner?.scanLocation()
    }

    override fun onStart() {
        super.onStart()

        UiRunner.activity = this
        //mapView.onStart()
        locationScanner?.scanLocation()
    }

    override fun onStop() {
        locationScanner?.stopScanning()
        //mapView.onStop()
        UiRunner.activity = null

        super.onStop()
    }

    override fun onPause() {
        locationScanner?.stopScanning()
        //mapView.onPause()
        UiRunner.activity = null

        super.onPause()
    }

    override fun onDestroy() {
        locationScanner?.stopScanning()
        //mapView.onDestroy()
        UiRunner.activity = null

        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        //mapView.onLowMemory()
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

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.map = mapboxMap

        map?.setStyle(
            "https://outdoor.mapwize.io/styles/mapwize/style.json?key="
                    + AccountManager.getInstance().apiKey
        )

        locationScanner = LocationScanners(applicationContext, this)
        locationScanner!!.scanLocation()
        navigationMethodsSpinner.onItemSelectedListener = this

        val nn = LatLng(56.268416, 43.877797)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(nn, 16.0))
    }

    override fun updateLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        if (marker == null) {
            marker = mapwizePlugin?.addMarker(LatLngFloor(latLng))
        } else {
            marker!!.latLngFloor = LatLngFloor(latLng)
        }
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
