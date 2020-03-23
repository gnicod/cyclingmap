package com.trekle.trekle

import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.sweetzpot.stravazpot.authenticaton.api.AuthenticationAPI
import com.sweetzpot.stravazpot.authenticaton.model.AppCredentials
import com.sweetzpot.stravazpot.authenticaton.model.LoginResult
import com.sweetzpot.stravazpot.common.api.AuthenticationConfig
import com.trekle.trekle.strava.StravaApi
import com.sweetzpot.stravazpot.common.api.StravaConfig
import com.sweetzpot.stravazpot.common.api.exception.StravaUnauthorizedException
import com.sweetzpot.stravazpot.route.api.RouteAPI
import com.sweetzpot.stravazpot.route.model.Route
import com.trekle.trekle.adapter.RoutesAdapter
import kotlinx.android.synthetic.main.activity_strava_routes.*
import kotlinx.coroutines.*


class StravaRoutesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strava_routes)
        setSupportActionBar(toolbar)
        val uiScope = CoroutineScope(Dispatchers.Main)
        fun loadRoutes(): Job = uiScope.launch {
            // ui thread, show loading
            var routes: MutableList<Route>? = null
            withContext(Dispatchers.IO) { // background thread
                routes = StravaApi.getRoutes()
            }
            withContext(Dispatchers.Main) { // background thread
                val listview = findViewById<ListView>(R.id.strava_list_routes)
                listview.adapter = RoutesAdapter(TrekleApplication.appContext!!, android.R.layout.simple_list_item_1, routes!!.toList().toTypedArray())
            }
        }
        loadRoutes()


        /*
        val uiScope = CoroutineScope(Dispatchers.Main)
        fun loadData(): Job = uiScope.launch {
            // ui thread, show loading
            val stravaInfo = withContext(Dispatchers.IO) { // background thread
                TrekleUser.getUser()
            }
            Log.i("TAG", "token = " + stravaInfo?.token)
            Thread {
                Log.i("TAG", "token from thread = " + stravaInfo?.token)
                val config: StravaConfig = StravaConfig.withToken(stravaInfo?.token)
                        .debug()
                        .build()
                val routeAPI = RouteAPI(config)
                var routes:MutableList<com.sweetzpot.stravazpot.route.model.Route>? = null
                try {
                    routes = routeAPI.listRoutes(5981362).execute()
                } catch (e: StravaUnauthorizedException) {
                    val clientID = getString(R.string.strava_client_id).toInt()
                    val clientSecret = getString(R.string.strava_client_secret)
                    val api = AuthenticationAPI(config)
                    val refreshTokenForApp = api.refreshTokenForApp(AppCredentials.with(clientID, clientSecret))
                    refreshTokenForApp.execute()
                }
                if (routes != null) {
                    listview.adapter = RoutesAdapter(TrekleApplication.appContext!!, android.R.layout.simple_list_item_1, routes.toList().toTypedArray())
                }
                routes?.forEach {
                    Log.i("TAG", "athlete "+ it.athlete)
                    Log.i("TAG", "routemap "+ it.name + ',' + it.id)
                }
                Log.i("TAG", "user ui "+ routes.toString())
            }.start()
            // ui thread
        }
        loadData()
         */
    }

}
