package com.trekle.trekle.strava

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.sweetzpot.stravazpot.authenticaton.api.ApprovalPrompt
import com.sweetzpot.stravazpot.authenticaton.api.AuthenticationAPI
import com.sweetzpot.stravazpot.authenticaton.model.AppCredentials
import com.sweetzpot.stravazpot.authenticaton.model.LoginResult
import com.sweetzpot.stravazpot.authenticaton.ui.StravaLoginActivity
import com.sweetzpot.stravazpot.common.api.AuthenticationConfig
import com.trekle.trekle.MainActivity
import com.trekle.trekle.R

class StravaLogin(private val context: Context?) {
    private var clientSecret: String? = null
    private var clientID: Int? = null
    private var redirectURI: String? = null
    private var approvalPrompt: ApprovalPrompt? = null
    private var accessScope: String? = null

    fun onReceiveResultCode(code: String?, cb: (token: String) -> Unit) {
        Thread {
            val config: AuthenticationConfig = AuthenticationConfig.create()
                    .debug()
                    .build()
            val api = AuthenticationAPI(config)
            val result: LoginResult = api.getTokenForApp(AppCredentials.with(this.clientID!!, this.clientSecret))
                    .withCode(code)
                    .execute()
            cb(result.token.toString())
        }.start()

    }
    fun withClientID(clientID: Int): StravaLogin {
        this.clientID = clientID
        return this
    }

    fun withClientSecret(clientSecret: String): StravaLogin {
        this.clientSecret = clientSecret
        return this
    }

    fun withRedirectURI(redirectURI: String?): StravaLogin {
        this.redirectURI = redirectURI
        return this
    }

    fun withApprovalPrompt(approvalPrompt: ApprovalPrompt?): StravaLogin {
        this.approvalPrompt = approvalPrompt
        return this
    }

    fun withAccessScope(accessScope: String?): StravaLogin {
        this.accessScope = accessScope
        return this
    }

    fun makeIntent(): Intent {
        val intent = Intent(context, StravaLoginActivity::class.java)
        intent.putExtra(StravaLoginActivity.EXTRA_LOGIN_URL, makeLoginURL())
        intent.putExtra(StravaLoginActivity.EXTRA_REDIRECT_URL, redirectURI)
        return intent
    }

    private fun makeLoginURL(): String {
        val loginURLBuilder = StringBuilder()
        loginURLBuilder.append(Companion.STRAVA_LOGIN_URL)
        loginURLBuilder.append(clientIDParameter())
        loginURLBuilder.append(redirectURIParameter())
        loginURLBuilder.append(approvalPromptParameter())
        loginURLBuilder.append(accessScopeParameter())
        return loginURLBuilder.toString()
    }

    private fun clientIDParameter(): String {
        return "&client_id=$clientID"
    }

    private fun redirectURIParameter(): String {
        return if (redirectURI != null) {
            "&redirect_uri=$redirectURI"
        } else {
            ""
        }
    }

    private fun approvalPromptParameter(): String {
        return if (approvalPrompt != null) {
            "&approval_prompt=" + approvalPrompt.toString()
        } else {
            ""
        }
    }

    private fun accessScopeParameter(): String {
        return if (accessScope != null) {
            "&scope=" + accessScope.toString()
        } else {
            ""
        }
    }

    companion object {
        private const val STRAVA_LOGIN_URL = "https://www.strava.com/oauth/mobile/authorize?response_type=code"
    }

}