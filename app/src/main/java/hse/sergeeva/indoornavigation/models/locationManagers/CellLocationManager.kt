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
import hse.sergeeva.indoornavigation.models.openCellIdApi.CellIdCellTower
import hse.sergeeva.indoornavigation.models.openCellIdApi.CellIdLocation
import hse.sergeeva.indoornavigation.models.openCellIdApi.OpenCellIdApi

class CellLocationManager(
    context: Context,
    private val locationReceiver: (success: Boolean, location: Location?) -> Unit
) : ILocationManager {
    private val telephonyManager =
        context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val googleApi: GoogleApi = GoogleApi(context)
    private val cellIdApi: OpenCellIdApi = OpenCellIdApi(context)
    private var scanStopped = false

    @SuppressLint("MissingPermission")
    override fun getLocation(): Boolean {
        val allCellInfo = telephonyManager.allCellInfo
        if (allCellInfo.size == 0) {
            Log.d("CellLocationManager", "Cannot get cell info")
            return false
        }

        processCellInfoToCellId(allCellInfo)
        return true
    }

    override fun stopScan() {
        scanStopped = true
    }

    private fun processCellInfoToGoogle(cellInfoList: List<CellInfo>) {
        val googleCellTowers = arrayListOf<GoogleCellTower>()
        for (cellInfo in cellInfoList) {
            val cellTower = cellInfoToGoogleCellTower(cellInfo)
            if (cellTower != null)
                googleCellTowers.add(cellTower)
        }

        if (scanStopped) return

        googleApi.getLocation(
            cellData = googleCellTowers,
            onSuccess = ::onSuccessDetermineLocation,
            onError = ::onErrorDetermineLocation
        )
    }

    private fun processCellInfoToCellId(cellInfoList: List<CellInfo>) {
        val cellIdTowers = arrayListOf<CellIdCellTower>()
        for (cellInfo in cellInfoList) {
            val cellTower = cellInfoToCellIdTower(cellInfo)
            if (cellTower != null)
                cellIdTowers.add(cellTower)
        }

        if (scanStopped) return

        cellIdApi.getLocation(
            cellData = cellIdTowers,
            onSuccess = ::onResultCellId,
            onError = ::onResultCellId
        )
    }

    private fun cellInfoToGoogleCellTower(cellInfo: CellInfo): GoogleCellTower? {
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

    private fun cellInfoToCellIdTower(cellInfo: CellInfo): CellIdCellTower? {
        when (cellInfo) {
            is CellInfoGsm -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                var timingAdvance = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) timingAdvance = signalStrength.timingAdvance

                return CellIdCellTower(
                    radio = "gsm",
                    cid = cellIdentity.cid,
                    lac = cellIdentity.lac,
                    mcc = cellIdentity.mcc,
                    mnc = cellIdentity.mnc,
                    signal = signalStrength.dbm,
                    ta = timingAdvance
                )
            }
            is CellInfoLte -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return CellIdCellTower(
                    radio = "lte",
                    cid = cellIdentity.ci,
                    lac = cellIdentity.tac,
                    mcc = cellIdentity.mcc,
                    mnc = cellIdentity.mnc,
                    signal = signalStrength.dbm,
                    ta = signalStrength.timingAdvance
                )
            }
            is CellInfoWcdma -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return CellIdCellTower(
                    radio = "wcdma",
                    cid = cellIdentity.cid,
                    lac = cellIdentity.lac,
                    mcc = cellIdentity.mcc,
                    mnc = cellIdentity.mnc,
                    signal = signalStrength.dbm
                )
            }
            is CellInfoCdma -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return CellIdCellTower(
                    radio = "cdma",
                    cid = cellIdentity.basestationId,
                    lac = cellIdentity.networkId,
                    mcc = cellIdentity.systemId,
                    mnc = cellIdentity.systemId,
                    signal = signalStrength.dbm
                )
            }
            else -> return null
        }

    }

    private fun onSuccessDetermineLocation(googleLocation: GoogleLocation) {
        if (scanStopped) return

        val currentLocation = Location(
            latitude = googleLocation.location.lat,
            longitude = googleLocation.location.lng,
            accuracy = googleLocation.accuracy
        )
        locationReceiver(true, currentLocation)
    }

    private fun onErrorDetermineLocation(error: GoogleError) {
        if (scanStopped) return

        Log.d("CellLocationManager", error.message)
        locationReceiver(false, null)
    }

    private fun onResultCellId(cellIdLocation: CellIdLocation) {
        if (scanStopped) return

        if (cellIdLocation.status == "error")
            locationReceiver(false, null)
        else {
            val currentLocation = Location(
                latitude = cellIdLocation.lat,
                longitude = cellIdLocation.lon,
                accuracy = cellIdLocation.accuracy
            )
            locationReceiver(true, currentLocation)
        }
    }
}