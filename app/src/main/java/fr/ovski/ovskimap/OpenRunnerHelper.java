package fr.ovski.ovskimap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.JavaNetCookieJar;

class OpenRunnerLogin {
    String username;
    String password;

    public OpenRunnerLogin(String username,String password) {
        this.username = username;
        this.password = password;
    }
}


public class OpenRunnerHelper {

    SharedPreferences pref;
    List cookies;
    OkHttpClient client;
    String username;
    String password;

    public void login() {
        RequestBody requestBody = new FormBody.Builder()
                .add("user",username)
                .add("pwd",password)
                .add("stayconnected","on")
                .build();
        Request request = new Request.Builder()
                .url("http://www.openrunner.com/account/login.php")
                .post(requestBody)
                .build();
        try {
            Response response = client.newCall(request).execute();
            Log.i("OPENRUNNER","response => " + response.body().string());
            // TODO store cookie somewhere ?
        } catch (IOException e) {
            Log.i("OPENRUNNER","fuck");
            e.printStackTrace();
        }
    }

    public OpenRunnerHelper(Context ctx){
        pref = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        username = pref.getString("openrunner_username","");
        password = pref.getString("openrunner_password","");
        Log.i("OPENRUNNER",username);
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();
    }

    public OkHttpClient getClient(){
        return client;
    }
}
