package fr.ovski.ovskimap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;

import fr.ovski.ovskimap.models.ORPass;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ORPassesTasks extends AsyncTask<Void, Integer, ArrayList> {

    Context ctx;
    OpenRunnerHelper orHelper;
    OkHttpClient client;
    GeoPoint top;
    GeoPoint bottom;

    public ORPassesTasks(Context ctx,GeoPoint top, GeoPoint bottom) {
        this.ctx = ctx;
        this.orHelper = new OpenRunnerHelper(ctx);
        this.top = top;
        this.bottom = bottom;
    }

    private ArrayList<ORPass> getPasses() {
        Log.i("OPENRUNNER","get passes " + Double.toString(bottom.getLongitude()));
        RequestBody requestBody = new FormBody.Builder()
                .add("latmin", Double.toString(bottom.getLatitude()))
                .add("latmax", Double.toString(top.getLatitude()))
                .add("lngmin", Double.toString(top.getLongitude()))
                .add("lngmax", Double.toString(bottom.getLongitude()))
                .add("level","0")
                .add("type","0")
                .build();
        Request request = new Request.Builder()
                .url("http://www.openrunner.com/poi-utils/showPoiCol.php")
                .post(requestBody)
                .build();
        try {
            Response response = client.newCall(request).execute();
            String html = response.body().string();
            html = html.replace("OR6464","");
            byte[] data = Base64.decode(html, Base64.DEFAULT);
            String text = new String(data, "UTF-8");
            Log.i("OPENRUNNER","response => " + text);
            JSONArray json = new JSONArray(text);
            ArrayList<ORPass> passes = new ArrayList<ORPass>();
            for (int i = 0; i < json.length(); i++) {
                JSONObject row = json.getJSONObject(i);
                String name = row.getString("nom");
                ORPass pass = new ORPass(
                        row.getInt("idpoi"),
                        row.getString("nom"),
                        row.getDouble("lat"),
                        row.getDouble("lng"),
                        row.getInt("alt"),
                        row.getString("desc")
                );
                passes.add(pass);
                Log.i("OPENRUNNER","nom  => " + name);
            }
            Log.i("OPENRUNNER","response => " + text);
            return passes;
            //return this.extractRoutes(html);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new ArrayList<ORPass>();
    }

    @Override
    protected ArrayList doInBackground(Void... params) {
        Log.i("OPENRUNNER","before > ");
        orHelper.login();
        Log.i("OPENRUNNER","before > ");
        this.client = orHelper.getClient();
        // TODO if online
        ArrayList<ORPass> passes =  this.getPasses();
        OpenRunnerRouteDbHelper ordb = new OpenRunnerRouteDbHelper(ctx.getApplicationContext());
        for ( ORPass pass : passes) {
            Log.i("OPENRUNNER",pass.toString());
            ordb.insertPass(pass);
        }
        return passes;
    }

}
