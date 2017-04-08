package fr.ovski.ovskimap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
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
import java.util.Iterator;
import java.util.Map;

import fr.ovski.ovskimap.OpenRunnerHelper;
import fr.ovski.ovskimap.OpenRunnerRouteDbHelper;
import fr.ovski.ovskimap.R;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.JavaNetCookieJar;


class ORRoutesTask extends AsyncTask<Void, Integer, HashMap<Integer,String>> {

    private OkHttpClient client;
    Context ctx;
    OpenRunnerHelper orHelper;

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
    protected HashMap<Integer,String> doInBackground(Void... params ) {
        orHelper.login();
        this.client = orHelper.getClient();
        return this.getRoutes();
    }

    public interface AsyncResponse {
        void processFinish(String output);
    }

    public ORRoutesTask(Context ctx) {

        this.ctx = ctx;
        this.orHelper = new OpenRunnerHelper(ctx);
    }

    @Override
    protected void onPostExecute(final HashMap<Integer,String> res) {
        Iterator it = res.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer,String> pair = (Map.Entry) it.next();
            Log.i("OPENRUNNER",pair.getKey() + "=> " + pair.getValue());
            putRouteIntoDB(pair.getKey(),pair.getValue(),"false");
        };

        CharSequence[] items = res.values().toArray(new CharSequence[res.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.ctx);
        builder.setTitle("Select a route");
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
        // get download service and enqueue fileP
        DownloadManager manager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        BroadcastReceiver onDownloadComplete=new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {


                Toast.makeText(ctx, "Download Complete",Toast.LENGTH_LONG).show();
                KmlDocument kmlDocument = new KmlDocument();
                kmlDocument.parseKMLFile(new File("/storage/emulated/0/Download/name-of-the-file.kml"));
                View rootView = ((Activity)ctx).getWindow().getDecorView().findViewById(android.R.id.content);
                MapView map = (MapView) rootView.findViewById(R.id.map);
                FolderOverlay kmlOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument);
                map.getOverlays().add(kmlOverlay);
                map.invalidate();
                BoundingBox bb = kmlDocument.mKmlRoot.getBoundingBox();
                map.getController().setCenter(bb.getCenter());

            }
        };
        ctx.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        manager.enqueue(request);
    }

    private void setRouteDownloaded(int id) {
        OpenRunnerRouteDbHelper openRunnerRouteDbHelper = new OpenRunnerRouteDbHelper(ctx);
        SQLiteDatabase db = openRunnerRouteDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("downloaded","true");
        db.update(OpenRunnerRouteDbHelper.ORRoutes.TABLE_NAME,values,"id=?", new String[]{"id"});
    }

    private void putRouteIntoDB(int id, String name, String file) {
        OpenRunnerRouteDbHelper openRunnerRouteDbHelper = new OpenRunnerRouteDbHelper(ctx);
        SQLiteDatabase db = openRunnerRouteDbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(OpenRunnerRouteDbHelper.ORRoutes.TABLE_COLUMN_NAME,"testname");
        contentValues.put(OpenRunnerRouteDbHelper.ORRoutes.TABLE_COLUMN_DOWNLOADED,"false");
        db.insert(OpenRunnerRouteDbHelper.ORRoutes.TABLE_NAME,null,contentValues);
        
    }


}
