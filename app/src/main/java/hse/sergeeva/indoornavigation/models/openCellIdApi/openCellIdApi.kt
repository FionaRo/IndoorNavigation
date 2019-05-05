package hse.sergeeva.indoornavigation.models.openCellIdApi

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import hse.sergeeva.indoornavigation.models.TowerStatistics
import org.json.JSONObject
import java.nio.charset.Charset

class OpenCellIdApi(context: Context) {
    private val server = "https://us1.unwiredlabs.com/v2/process.php"
    private val apiKey = "ed6a3ab2fbdd25"
    private val queue: RequestQueue = Volley.newRequestQueue(context.applicationContext)
    val cellIdLocations: HashSet<String> = hashSetOf()
    val myLinkovLocations: HashSet<String> = hashSetOf()

    fun getLocation(
        wifiData: ArrayList<CellIdWiFiAccessPoint> = arrayListOf(),
        cellData: ArrayList<CellIdCellTower> = arrayListOf(),
        onSuccess: (location: CellIdLocation) -> Unit,
        onError: (error: CellIdLocation) -> Unit
    ) {
        val url = "$server/geolocation/v1/geolocate?key=$apiKey"
        val bodyObject = CellIdGeolocationRequestBody(token = apiKey, wifi = wifiData, cells = cellData)
        val gson = Gson()
        val jsonStr = gson.toJson(bodyObject)
        val jsonObject = JSONObject(jsonStr)

        val jsonRequest = JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonObject,
            Response.Listener<JSONObject> { response ->
                val respStr = response.toString()
                val location = gson.fromJson<CellIdLocation>(respStr, CellIdLocation::class.java)
                onSuccess(location)
            },
            Response.ErrorListener { error ->
                val errorMessage = error.networkResponse.data.toString(Charset.forName("UTF-8")).replace('\n', ' ')
                val googleError = gson.fromJson<CellIdLocation>(errorMessage, CellIdLocation::class.java)
                Log.d("OpenCellId", googleError.message)
                onError(googleError)
            })

        queue.add(jsonRequest)
    }

    fun getTowerLocation(cellTower: CellIdCellTower) {
        TowerStatistics.allTowers++

        val url = "https://api.mylnikov.org/geolocation/cell?v=1.1&data=open&" +
                "mcc=${cellTower.mcc}&" +
                "mnc=${cellTower.mnc}&" +
                "lac=${cellTower.lac}&" +
                "cellid=${cellTower.cid}"

        val url2 = "https://opencellid.org/ajax/searchCell.php?" +
                "mcc=${cellTower.mcc}&" +
                "mnc=${cellTower.mnc}&" +
                "lac=${cellTower.lac}&" +
                "cell_id=${cellTower.cid}"

        val gson = Gson()

        val jsonRequest = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            Response.Listener<JSONObject> { response ->
                val respStr = response.toString()
                val location = gson.fromJson<Data>(respStr, Data::class.java)
                if (location.result != 200 || location.data.lon == 0.0) {
                    TowerStatistics.notFoundTowersMylnikov++
                    Log.d("MylnikovAPIError", location.desc)
                } else {
                    val id = "${location.data.lat},${location.data.lon}"
                    if (myLinkovLocations.contains(id))
                        TowerStatistics.duplicateMyLinkov++
                    else {
                        myLinkovLocations.add(id)
                        TowerStatistics.foundTowersMylnikov++
                        Log.d("MylnikovAPI", id)
                    }
                }
            },
            Response.ErrorListener { error ->
                TowerStatistics.notFoundTowersMylnikov++
                Log.d("MylnikovAPIError", "Cannot get cell tower location")
            }
        )

        val jsonRequest2 = JsonObjectRequest(
            Request.Method.GET,
            url2,
            null,
            Response.Listener<JSONObject> { response ->
                val respStr = response.toString()
                val location = gson.fromJson<CellTowerLocation>(respStr, CellTowerLocation::class.java)
                if (location.lon == 0.0) {
                    TowerStatistics.notFoundTowersOpenCellId++
                } else {
                    val id = "${location.lat},${location.lon}"
                    if (cellIdLocations.contains(id))
                        TowerStatistics.duplicateCellId++
                    else {
                        cellIdLocations.add(id)
                        TowerStatistics.foundTowersOpenCellId++
                        Log.d("CellIdAPI", "${location.lat},${location.lon}")
                    }
                }
            },
            Response.ErrorListener { error ->
                TowerStatistics.notFoundTowersOpenCellId++
                Log.d("CellIdAPI", "Cannot get cell tower location")
            }
        )

        queue.add(jsonRequest)
        queue.add(jsonRequest2)
    }
}