package fr.ovski.ovskimap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;

import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;

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

class OpenRunnerTask extends AsyncTask<OpenRunnerLogin, Integer, HashMap<Integer,String>> {

    private OkHttpClient client;
    Context ctx;

    private void login(OpenRunnerLogin openRunnerLogin) {
        RequestBody requestBody = new FormBody.Builder()
                .add("user",openRunnerLogin.username)
                .add("pwd",openRunnerLogin.password)
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
            e.printStackTrace();
        }
    }

    private HashMap<Integer,String> getRoutes() {
        Request request = new Request.Builder()
                .url("http://www.openrunner.com/search/searchMyRoutes.php?u=km")
                .get()
                .build();
        try {
            Response response = client.newCall(request).execute();
            String html = response.body().string();
            Log.i("OPENRUNNER","response => " + html);
            return this.extractRoutes(html);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<Integer,String> routes = new HashMap<Integer,String>();
        return routes;
    }

    private HashMap<Integer,String> extractRoutes(String html) {
        HashMap<Integer,String> routes = new HashMap<Integer,String>();
        Document doc = Jsoup.parse(html);
        Elements tr = doc.select("tbody>tr");
        for (Element t : tr){
            Element checkbox = t.select("input").first();
            Element name = t.select("div").first();
            routes.put(Integer.parseInt(checkbox.val()),name.html());
            Log.i("OPENRUNNER","chcekbox => " + checkbox.val());
            Log.i("OPENRUNNER","name => " + name.html());
        }

        return routes;
    }

    @Override
    protected HashMap<Integer,String> doInBackground(OpenRunnerLogin... openRunnerLogins) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
         client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();
        int count = openRunnerLogins.length;
        Long totalSize = new Long(0);
        for (int i = 0; i < count; i++) {
            OpenRunnerLogin oLogin = openRunnerLogins[i];
            totalSize += i;
            this.login(oLogin);
            return this.getRoutes();
        }
        return new HashMap<Integer,String>();
    }

    public interface AsyncResponse {
        void processFinish(String output);
    }

    public OpenRunnerTask(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void onPostExecute(final HashMap<Integer,String> res) {
        CharSequence[] items = res.values().toArray(new CharSequence[res.size()]);
        //final CharSequence[] items = {"A", "B", "C"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this.ctx);
        builder.setTitle("Make your selection");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                String id = (String) res.keySet().toArray()[item].toString();
                Log.i("OPENRUNNER","name => " + id);
                downloadKML(id);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void downloadKML(String id){
        String url = String.format("http://www.openrunner.com/kml/exportImportKml.php?id=%s&km=0",id);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Some descrition");
        request.setTitle("Some title");
        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "name-of-the-file.kml");
        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        BroadcastReceiver onDownloadComplete=new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                Toast.makeText(ctx, "Download Complete",Toast.LENGTH_LONG).show();
                KmlDocument kmlDocument = new KmlDocument();
                kmlDocument.parseKMLFile(new File("/storage/emulated/0/Download/name-of-the-file.kml"));
                View rootView = ((Activity)ctx).getWindow().getDecorView().findViewById(android.R.id.content);
                MapView map = (MapView) rootView.findViewById(R.id.map);
                FolderOverlay kmlOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument);
                map.getOverlays().add(kmlOverlay);
                map.invalidate();
                //BoundingBox bb = kmlDocument.mKmlRoot.getBoundingBox();
                //map.zoomToBoundingBox(bb,true);
                //map.getController().setCenter(bb.getCenter());
            }
        };
        ctx.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        manager.enqueue(request);
    }


}