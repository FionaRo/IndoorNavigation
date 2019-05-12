package hse.sergeeva.indoornavigation.views

import android.app.Application
import io.mapwize.mapwizeformapbox.AccountManager

class IndoorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AccountManager.start(this, "b6b0467dc770cee0e57ac1c5a3202d87")
    }

}
