package hse.sergeeva.indoornavigation.presenters

import android.annotation.SuppressLint
import android.app.Activity

class UiRunner {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var activity: Activity? = null
        fun runOnUiThread(action: () -> Unit) {
            activity?.runOnUiThread(action)
        }
    }
}