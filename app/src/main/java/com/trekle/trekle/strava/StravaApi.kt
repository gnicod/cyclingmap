package com.trekle.trekle.strava

import android.content.Context
import android.util.Log
import com.sweetzpot.stravazpot.authenticaton.api.AuthenticationAPI
import com.sweetzpot.stravazpot.authenticaton.model.AppCredentials
import com.sweetzpot.stravazpot.authenticaton.model.LoginResult
import com.sweetzpot.stravazpot.common.api.AuthenticationConfig
import com.sweetzpot.stravazpot.common.api.StravaConfig
import com.sweetzpot.stravazpot.common.api.exception.StravaUnauthorizedException
import com.sweetzpot.stravazpot.route.api.RouteAPI
import com.sweetzpot.stravazpot.route.model.Route
import com.trekle.trekle.R
import com.trekle.trekle.TrekleApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StravaApi {

    val PREF_ACCESS_TOKEN = "StravaApi.PREF_ACCESS_TOKEN"
    val PREF_REFRESH_TOKEN = "StravaApi.PREF_REFRESH_TOKEN"

    private var sharedPreferences = TrekleApplication.appContext!!.getSharedPreferences("shared_preference", Context.MODE_PRIVATE)
    val clientID = TrekleApplication.appContext!!.getString(R.string.strava_client_id).toInt()
    val clientSecret = TrekleApplication.appContext!!.getString(R.string.strava_client_secret)
    var accessToken = sharedPreferences.getString(PREF_ACCESS_TOKEN, null)
    var refreshToken = sharedPreferences.getString(PREF_REFRESH_TOKEN, null)

    fun loginWithCode(code: String, cb: (StravaApi) -> Unit) {
        Thread {
            val config: AuthenticationConfig = AuthenticationConfig.create()
                    .debug()
                    .build()
            val api = AuthenticationAPI(config)
            val result: LoginResult = api.getTokenForApp(AppCredentials.with(this.clientID, this.clientSecret))
                    .withCode(code)
                    .execute()
            accessToken = result.token.toString()
            refreshToken = result.refreshToken
            val editor = sharedPreferences.edit()
            editor.putString(PREF_ACCESS_TOKEN, accessToken)
            editor.putString(PREF_REFRESH_TOKEN, refreshToken)
            editor.apply()
            cb(this)
        }.start()
    }

    fun refreshToken(cb: () -> Unit) : Any {
        val config: AuthenticationConfig = AuthenticationConfig.create()
                .debug()
                .build()
        val api = AuthenticationAPI(config)
        val result: LoginResult = api.refreshTokenForApp(AppCredentials.with(this.clientID, this.clientSecret))
                .withRefreshToken(refreshToken)
                .execute()
        accessToken = result.token.toString()
        refreshToken = result.refreshToken
        val editor = sharedPreferences.edit()
        editor.putString(PREF_ACCESS_TOKEN, accessToken)
        editor.putString(PREF_REFRESH_TOKEN, refreshToken)
        editor.apply()
        return cb()
    }

    fun getRoutes(): MutableList<Route>? {
        Log.i("TREKLE", "accesstoken "+ accessToken)
        val config: StravaConfig = StravaConfig.withToken(accessToken)
                .debug()
                .build()
        val routeAPI = RouteAPI(config)
        var routes: MutableList<com.sweetzpot.stravazpot.route.model.Route>? = null
        try {
            routes = routeAPI.listRoutes(5981362).execute()
        } catch (e: StravaUnauthorizedException) {
            return refreshToken { getRoutes() } as MutableList<Route>
        }
        return routes
    }


}