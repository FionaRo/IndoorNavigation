package hse.sergeeva.indoornavigation.models.yandexApi

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONObject
import java.nio.charset.Charset

class YandexApi(context: Context) {
    private val server = "https://api.lbs.yandex.net"
    private val apiKey = "AB_7ulwBAAAA3byNaAIAzXt2WgbesRm6ZIRWv2v9h0zTHDwAAAAAAAAAAAAtxeAjTU0LQW989eZNIAo6DsonNw==\n"
    private val queue: RequestQueue = Volley.newRequestQueue(context.applicationContext)

    fun getLocation(
        wifiData: ArrayList<YandexWifiPoint> = arrayListOf(),
        cellData: ArrayList<YandexCellTower> = arrayListOf(),
        onSuccess: (location: YandexReturnObject) -> Unit,
        onError: (error: YandexReturnError) -> Unit
    ) {
        val url = "$server/geolocation"
        val bodyObject = YandexGeolocationRequestBody(
            common = YandexCommonObject(api_key = apiKey),
            gsm_cells = cellData,
            wifi_networks = wifiData
        )
        val gson = Gson()
        val jsonStr = gson.toJson(bodyObject)
        val jsonObject = JSONObject(jsonStr)

        val jsonRequest = object : StringRequest(
            Request.Method.POST,
            url,
            Response.Listener<String> { respStr ->
                val location = gson.fromJson<YandexReturnObject>(respStr, YandexReturnObject::class.java)
                onSuccess(location)
            },
            Response.ErrorListener { error ->
                val errorMessage = error.networkResponse.data.toString(Charset.forName("UTF-8")).replace('\n', ' ')
                val yandexReturnError = gson.fromJson<YandexReturnError>(errorMessage, YandexError::class.java)
                Log.d("YandexApi.Geolocation", yandexReturnError.error.message)
                onError(yandexReturnError)
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["json"] = jsonStr
                return params
            }
        }

        queue.add(jsonRequest)
    }
}