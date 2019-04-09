package hse.sergeeva.indoornavigation.models.wigletApi

import android.util.Base64
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class RequestWithCredentials(
    method: Int, url: String,
    jsonObject: JSONObject?,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener,
    credentials: String
) : JsonObjectRequest(method, url, jsonObject, listener, errorListener) {
    private val mCredentials = credentials

    @Throws(AuthFailureError::class)
    override fun getHeaders(): Map<String, String> {
        val headers = HashMap<String, String>()
        val auth = "Basic " + Base64.encodeToString(mCredentials.toByteArray(), Base64.NO_WRAP)
        headers["Authorization"] = auth
        return headers
    }
}
