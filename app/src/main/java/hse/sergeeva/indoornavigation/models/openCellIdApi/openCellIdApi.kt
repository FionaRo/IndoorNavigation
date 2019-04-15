package hse.sergeeva.indoornavigation.models.openCellIdApi

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONObject
import java.nio.charset.Charset

class OpenCellIdApi(context: Context) {
    private val server = "https://us1.unwiredlabs.com/v2/process.php"
    private val apiKey = "ed6a3ab2fbdd25"
    private val queue: RequestQueue = Volley.newRequestQueue(context.applicationContext)

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
                Log.d("GoogleApi.Geolocation", googleError.message)
                onError(googleError)
            })

        queue.add(jsonRequest)
    }
}