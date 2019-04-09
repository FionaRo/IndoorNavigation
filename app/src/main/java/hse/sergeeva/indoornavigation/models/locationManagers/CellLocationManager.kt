package hse.sergeeva.indoornavigation.models.locationManagers

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.*
import android.util.Log
import hse.sergeeva.indoornavigation.models.Location
import hse.sergeeva.indoornavigation.models.googleApi.GoogleApi
import hse.sergeeva.indoornavigation.models.googleApi.GoogleCellTower
import hse.sergeeva.indoornavigation.models.googleApi.GoogleError
import hse.sergeeva.indoornavigation.models.googleApi.GoogleLocation

class CellLocationManager(
    private val context: Context,
    private val locationReceiver: (success: Boolean, location: Location?) -> Unit
) : ILocationManager {
    private val telephonyManager =
        context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val googleApi: GoogleApi = GoogleApi(context)

    @SuppressLint("MissingPermission")
    override fun getLocation(): Boolean {
        val allCellInfo = telephonyManager.allCellInfo
        if (allCellInfo.size == 0) {
            Log.d("CellLocationManager", "Cannot get cell info")
            return false
        }

        processCellInfo(allCellInfo)
        return true
    }

    private fun processCellInfo(cellInfoList: List<CellInfo>) {
        val googleCellTowers = arrayListOf<GoogleCellTower>()
        for (cellInfo in cellInfoList) {
            googleCellTowers.add(cellInfoToGoogleCellTower(cellInfo)!!)
        }

        googleApi.getLocation(
            cellData = googleCellTowers,
            onSuccess = ::onSuccessDetermineLocation,
            onError = ::onErrorDetermineLocation
        )
    }

    public fun cellInfoToGoogleCellTower(cellInfo: CellInfo): GoogleCellTower? {
        when (cellInfo) {
            is CellInfoGsm -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                var timingAdvance = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) timingAdvance = signalStrength.timingAdvance

                return GoogleCellTower(
                    cellId = cellIdentity.cid,
                    locationAreaCode = cellIdentity.lac,
                    mobileCountryCode = cellIdentity.mcc,
                    mobileNetworkCode = cellIdentity.mnc,
                    signalStrength = signalStrength.dbm,
                    timingAdvance = timingAdvance
                )
            }
            is CellInfoLte -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return GoogleCellTower(
                    cellId = cellIdentity.ci,
                    locationAreaCode = cellIdentity.tac,
                    mobileCountryCode = cellIdentity.mcc,
                    mobileNetworkCode = cellIdentity.mnc,
                    signalStrength = signalStrength.dbm,
                    timingAdvance = signalStrength.timingAdvance
                )
            }
            is CellInfoWcdma -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return GoogleCellTower(
                    cellId = cellIdentity.cid,
                    locationAreaCode = cellIdentity.lac,
                    mobileCountryCode = cellIdentity.mcc,
                    mobileNetworkCode = cellIdentity.mnc,
                    signalStrength = signalStrength.dbm
                )
            }
            is CellInfoCdma -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return GoogleCellTower(
                    cellId = cellIdentity.basestationId,
                    locationAreaCode = cellIdentity.networkId,
                    mobileCountryCode = cellIdentity.systemId,
                    mobileNetworkCode = cellIdentity.systemId,
                    signalStrength = signalStrength.dbm
                )
            }
            else -> return null
        }

    }

    private fun onSuccessDetermineLocation(googleLocation: GoogleLocation) {
        val currentLocation = Location(
            latitude = googleLocation.location.lat,
            longtitude = googleLocation.location.lng,
            accuracy = googleLocation.accuracy
        )
        locationReceiver(true, currentLocation)
    }

    private fun onErrorDetermineLocation(error: GoogleError) {
        Log.d("CellLocationManager", error.message)
        locationReceiver(false, null)
    }
}