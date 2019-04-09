package hse.sergeeva.indoornavigation.models.wigletApi

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import hse.sergeeva.indoornavigation.models.wigletApi.RequestWithCredentials
import org.json.JSONObject

class WigletApi(private val context: Context) {
    private val username = "AIDd70d29a39462c4b829fbe971d0d03276"
    private val password = "dee3b1a1fa4eee8e0de27243d41db914"
/*
    private val username = "AIDecb04fb7dcfa4e2e88c795165e5a60fa"
    private val password = "b848187d81ef0498bb9f32f1b1675a3d"
*/
    private val server = "https://api.wigle.net/api/v2"
    private val queue = Volley.newRequestQueue(context)

    fun getNetworkDetailByBssid(bssid: String, listener: Response.Listener<JSONObject>) {
        val url = "$server/network/detail?netid=$bssid"
        request(url, listener)
    }

    fun getNetworkDetailByCid(cid: String, listener: Response.Listener<JSONObject>) {
        val url = "$server/network/detail?cid=$cid"
        request(url, listener)
    }

    fun searchNetworkByBssid(bssid: String, listener: Response.Listener<JSONObject>) {
        val url = "$server/network/search?netid=$bssid"
        request(url, listener)
    }

    fun searchNetworkByName(name: String, listener: Response.Listener<JSONObject>) {
        val url = "$server/network/search?ssid=$name"
        request(url, listener)
    }

    private fun request(url: String, listener: Response.Listener<JSONObject>) {
        val request = RequestWithCredentials(
            Request.Method.GET, url, null, listener,
            Response.ErrorListener { error ->
                Log.e(
                    "RequestError",
                    "Error while connecting to wiglet: ${error.message}"
                )
            },
            "$username:$password"
        )

        queue.add(request)
    }
}