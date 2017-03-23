package fr.ovski.ovskimap;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created by ovski on 22/03/17.
 */

public class OpenRunnerHelper {

    SharedPreferences preferences;
    List cookies;

    public OpenRunnerHelper(SharedPreferences pref) throws IOException {
        String username = pref.getString("openrunnerUsername","ovskywalker");
        String password = pref.getString("openrunnerPassword","17c2509");
        URL url = new URL("http://openrunner.com");
        String postData = String.format("user=%s&pwd=%s&stayconnected=on",
                username,
                password
        );
        try{
            URLConnection con = url.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.3");
            con.setRequestProperty("Referer", "http://www.openrunner.com/");
            //con.connect();

            DataOutputStream output = new DataOutputStream(con.getOutputStream());
            output.writeBytes(postData);
            output.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            con.getInputStream()));
            String decodedString;
            while ((decodedString = in.readLine()) != null) {
                Log.i("OPENRUNNER",decodedString);
            }
            cookies = con.getHeaderFields().get("Set-Cookie");
            in.close();
            Log.i("OPENRUNNER","test");

            // http://www.openrunner.com/search/searchMyRoutes.php?u=km
        }
        catch (Exception e){
            Log.i("OPENRUNNER",e.toString());
        }

    }
}
