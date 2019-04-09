package hse.sergeeva.indoornavigation.models.googleApi

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

class GoogleApi(context: Context) {
    private val server = "https://www.googleapis.com"
    private val apiKey = "AIzaSyAAIXs5e6CnvBgWIVSFYSLW0tEWwieJZpg"
    private val queue: RequestQueue = Volley.newRequestQueue(context.applicationContext)

    fun getLocation(
        wifiData: ArrayList<GoogleWiFiAccessPoint> = arrayListOf(),
        cellData: ArrayList<GoogleCellTower> = arrayListOf(),
        onSuccess: (location: GoogleLocation) -> Unit,
        onError: (error: GoogleError) -> Unit
    ) {
        val url = "$server/geolocation/v1/geolocate?key=$apiKey"
        val bodyObject = GoogleGeolocationRequestBody(wifiAccessPoints = wifiData, cellTowers = cellData)
        val gson = Gson()
        val jsonStr = gson.toJson(bodyObject)
        val jsonObject = JSONObject(jsonStr)

        val jsonRequest = JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonObject,
            Response.Listener<JSONObject> { response ->
                val respStr = response.toString()
                val location = gson.fromJson<GoogleLocation>(respStr, GoogleLocation::class.java)
                onSuccess(location)
            },
            Response.ErrorListener { error ->
                val errorMessage = error.networkResponse.data.toString(Charset.forName("UTF-8")).replace('\n', ' ')
                val googleError = gson.fromJson<GoogleError>(errorMessage, GoogleError::class.java)
                Log.d("GoogleApi.Geolocation", googleError.message)
                onError(googleError)
            })

        queue.add(jsonRequest)
    }
}