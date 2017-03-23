package fr.ovski.ovskimap;

import android.app.DownloadManager;
import android.content.Context;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.config.Configuration;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;

import fr.ovski.ovskimap.OpenRunnerHelper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MapEventsReceiver {

    private MapView map;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private LocationManager mLocationManager;
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Marker startMarker = new Marker(map);
            startMarker.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
            startMarker.setTitle("ICI");
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(startMarker);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createSourceSelectBox();
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        Configuration.getInstance().load(ctx, preferences);
        editor = preferences.edit();

        ITileSource tileSourceBase = TileSourceFactory.getTileSource(
                preferences.getString("tileSource","OpenTopoMap")
        );
        String key = "lv4ajb751s4scbbr4f5p2mo0";
        return "http://wxs.ign.fr/" + key
                + "/geoportail/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&"
                + "LAYER=" + layer + "&STYLE=normal&TILEMATRIXSET=PM&"
                + "TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=image%2Fjpeg";
        final OnlineTileSourceBase IGN_GEOPORTAIL = new XYTileSource("Geoportail",
                0, 18, 256, ".png",
                new String[] { "http://overlay.openstreetmap.nl/basemap/" });
        TileSourceFactory.addTileSource();

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(tileSourceBase);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        IMapController mapController = map.getController();
        mapController.setZoom(12);

        map.getOverlays().add(0, mapEventsOverlay);

        GeoPoint startPoint = new GeoPoint(45.65, 5.94);
        mapController.setCenter(startPoint);

        /*
        openrunner
         */
        String username = preferences.getString("openrunnerUsername","ovskywalker");
        String password = preferences.getString("openrunnerPassword","17c2509");
        new OpenRunnerTask(this).execute(new OpenRunnerLogin(username,password));

        /*
        KML
         */
        /*
        KmlDocument kmlDocument = new KmlDocument();
        kmlDocument.parseKMLUrl("http://mapsengine.google.com/map/kml?forcekml=1&mid=z6IJfj90QEd4.kUUY9FoHFRdE");
        FolderOverlay kmlOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument);
        map.getOverlays().add(1,kmlOverlay);
        map.invalidate();
        */

        /*
        GPS
         */
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, mLocationListener);
        }catch ( SecurityException e ) {

        }

        try {
            OpenRunnerHelper openrunner = new OpenRunnerHelper(preferences);
        } catch (IOException e) {
            Log.i("AAAAAAA","aaaaa");
            e.printStackTrace();
        }


    }

    private void createSourceSelectBox(){
        final CharSequence sources[] = new CharSequence[] {"Mapnik", "CycleMap", "OpenTopoMap", "HikeBikeMap"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Map source ?");
        builder.setItems(sources, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editor = preferences.edit();
                editor.putString("tileSource",sources[which].toString());
                editor.commit();
                ITileSource tileSourceBase = TileSourceFactory.getTileSource(
                        preferences.getString("tileSource","OpenTopoMap")
                );
                map.setTileSource(tileSourceBase);
                map.invalidate();

            }
        });
        builder.show();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        Toast.makeText(this, "Tapped", Toast.LENGTH_SHORT).show();
        return false;
    }



    @Override
    public boolean longPressHelper(GeoPoint p) {

        Marker startMarker = new Marker(map);
        startMarker.setPosition(p);
        startMarker.setTitle("Col de lol");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        map.invalidate();
        Toast.makeText(this, "long tapped", Toast.LENGTH_SHORT).show();
        return false;
    }
}
