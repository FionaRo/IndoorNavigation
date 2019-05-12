package hse.sergeeva.indoornavigation.models.locationManagers

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.*
import android.util.Log
import hse.sergeeva.indoornavigation.models.Location
import hse.sergeeva.indoornavigation.models.googleApi.*
import hse.sergeeva.indoornavigation.models.openCellIdApi.CellIdCellTower
import hse.sergeeva.indoornavigation.models.openCellIdApi.CellIdLocation
import hse.sergeeva.indoornavigation.models.openCellIdApi.OpenCellIdApi
import hse.sergeeva.indoornavigation.models.yandexApi.YandexApi
import hse.sergeeva.indoornavigation.models.yandexApi.YandexCellTower
import hse.sergeeva.indoornavigation.models.yandexApi.YandexReturnError
import hse.sergeeva.indoornavigation.models.yandexApi.YandexReturnObject

class CellLocationManager(
    context: Context,
    private val locationReceiver: (success: Boolean, location: Location?) -> Unit
) : ILocationManager {
    private val telephonyManager =
        context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val googleApi: GoogleApi = GoogleApi(context)
    private val cellIdApi: OpenCellIdApi = OpenCellIdApi(context)
    private val yandexApi: YandexApi = YandexApi(context)
    private var scanStopped = false
    private val cells: HashSet<String> = hashSetOf()

    @SuppressLint("MissingPermission")
    override fun getLocation(): Boolean {
        val allCellInfo = telephonyManager.allCellInfo
        telephonyManager.neighboringCellInfo
        if (allCellInfo.size == 0) {
            Log.d("CellLocationManager", "Cannot get cell info")
            return false
        }

        processCellInfoToGoogle(allCellInfo)
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
            onSuccess = ::onSuccessDetermineLocationGoogle,
            onError = ::onErrorDetermineLocationGoogle
        )
    }

    private fun processCellInfoToCellId(cellInfoList: List<CellInfo>) {
        //val cellIdTowers = arrayListOf<CellIdCellTower>()
        for (cellInfo in cellInfoList) {
            val cellTower = cellInfoToCellIdTower(cellInfo)
            if (cellTower != null) {

                val id = "${cellTower.cid};${cellTower.lac};${cellTower.mnc};${cellTower.mcc}"
                if (cells.contains(id)) continue

                cells.add(id)
                //cellIdTowers.add(cellTower)
                cellIdApi.getTowerLocation(cellTower)
            }
        }

        if (scanStopped) return

//        cellIdApi.getLocation(
//            cellData = cellIdTowers,
//            onSuccess = ::onResultCellId,
//            onError = ::onResultCellId
//        )
    }

    private fun processCellInfoToYandex(cellInfoList: List<CellInfo>) {
        val cellIdTowers = arrayListOf<YandexCellTower>()
        for (cellInfo in cellInfoList) {
            val cellTower = cellInfoToYandexCellTower(cellInfo)
            if (cellTower != null)
                cellIdTowers.add(cellTower)
        }

        if (scanStopped) return

        yandexApi.getLocation(
            cellData = cellIdTowers,
            onSuccess = ::onSuccessDetermineLocationYandex,
            onError = ::onErrorDetermineLocationYandex
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

    private fun cellInfoToYandexCellTower(cellInfo: CellInfo): YandexCellTower? {
        when (cellInfo) {
            is CellInfoGsm -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                var timingAdvance = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) timingAdvance = signalStrength.timingAdvance

                return YandexCellTower(
                    cellid = cellIdentity.cid,
                    lac = cellIdentity.lac,
                    countrycode = cellIdentity.mcc,
                    operatorid = cellIdentity.mnc,
                    signal_strength = signalStrength.dbm
                )
            }
            is CellInfoLte -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return YandexCellTower(
                    cellid = cellIdentity.ci,
                    lac = cellIdentity.tac,
                    countrycode = cellIdentity.mcc,
                    operatorid = cellIdentity.mnc,
                    signal_strength = signalStrength.dbm
                )
            }
            is CellInfoWcdma -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return YandexCellTower(
                    cellid = cellIdentity.cid,
                    lac = cellIdentity.lac,
                    countrycode = cellIdentity.mcc,
                    operatorid = cellIdentity.mnc,
                    signal_strength = signalStrength.dbm
                )
            }
            is CellInfoCdma -> {
                val cellIdentity = cellInfo.cellIdentity
                val signalStrength = cellInfo.cellSignalStrength

                return YandexCellTower(
                    cellid = cellIdentity.basestationId,
                    lac = cellIdentity.networkId,
                    countrycode = cellIdentity.systemId,
                    operatorid = cellIdentity.systemId,
                    signal_strength = signalStrength.dbm
                )
            }
            else -> return null
        }

    }

    private fun onSuccessDetermineLocationGoogle(googleLocation: GoogleLocation) {
        if (scanStopped) return

        val currentLocation = Location(
            latitude = googleLocation.location.lat,
            longitude = googleLocation.location.lng,
            accuracy = googleLocation.accuracy
        )
        locationReceiver(true, currentLocation)
    }

    private fun onErrorDetermineLocationGoogle(error: GoogleError) {
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

    private fun onSuccessDetermineLocationYandex(yandexLocation: YandexReturnObject) {
        if (scanStopped) return

        val currentLocation = Location(
            latitude = yandexLocation.position.latitude,
            longitude = yandexLocation.position.longitude,
            accuracy = yandexLocation.position.precision.toInt()
        )
        locationReceiver(true, currentLocation)
    }

    private fun onErrorDetermineLocationYandex(yandexError: YandexReturnError) {
        if (scanStopped) return

        Log.d("CellLocationManager", yandexError.error.message)
        locationReceiver(false, null)
    }
}