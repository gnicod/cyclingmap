package fr.ovski.ovskimap;

import android.content.Context;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.Toast;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import fr.ovski.ovskimap.models.ORPass;
import fr.ovski.ovskimap.tasks.OverpassQueryTask;
import hu.supercluster.overpasser.library.query.OverpassQuery;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, MapEventsReceiver {

    private MapView map;


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

        ITileSource tileSourceBase = TileSourceFactory.getTileSource(
                preferences.getString("tileSource","OpenTopoMap")
        );



        /*
        IGN GEOPORTAIL LAYER

        String key = "lv4ajb751s4scbbr4f5p2mo0";
        return "http://wxs.ign.fr/" + key
                + "/geoportail/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&"
                + "LAYER=" + layer + "&STYLE=normal&TILEMATRIXSET=PM&"
                + "TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&FORMAT=image%2Fjpeg";
        final OnlineTileSourceBase IGN_GEOPORTAIL = new XYTileSource("Geoportail",
                0, 18, 256, ".png",
                new String[] { "http://overlay.openstreetmap.nl/basemap/" });
        TileSourceFactory.addTileSource();
        */

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(tileSourceBase);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        IMapController mapController = map.getController();
        mapController.setZoom(12);

        map.getOverlays().add(0, mapEventsOverlay);

        // cluster
        RadiusMarkerClusterer poiMarkers = new RadiusMarkerClusterer(this);
        map.getOverlays().add(poiMarkers);
        Drawable clusterIconD = getResources().getDrawable(R.drawable.marker_cluster);
        Bitmap clusterIcon = ((BitmapDrawable)clusterIconD).getBitmap();
        poiMarkers.setIcon(clusterIcon);


        GeoPoint startPoint = new GeoPoint(45.65, 5.94);
        mapController.setCenter(startPoint);

        /*
        GPS
         */
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, mLocationListener);
        }catch ( SecurityException e ) {

        }

        /*
        SHOW COL FROM DB
         */
        OpenRunnerRouteDbHelper odb = new OpenRunnerRouteDbHelper(this);
        for(ORPass pass : odb.getAllPasses() ){
            Marker startMarker = new Marker(map);
            startMarker.setIcon(getResources().getDrawable(R.drawable.marker_cluster));
            startMarker.setPosition(new GeoPoint(pass.getLat(), pass.getLng()));
            startMarker.setTitle(pass.getName()+"\n"+pass.getAlt()+"m");
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            poiMarkers.add(startMarker);
            Log.i("OPENRUNNER","FROM db " + pass.toString());
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
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        Toast.makeText(this, "Tapped", Toast.LENGTH_SHORT).show();
        return false;
    }



    @Override
    public boolean longPressHelper(GeoPoint p) {
        final CharSequence sources[] = new CharSequence[] {"Insert POI", "Start routing", "Go to"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Que faire ?");
        builder.setItems(sources, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Toast.makeText(getApplicationContext(), "TODO: insert marker in db", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(getApplicationContext(), "TODO start routing", Toast.LENGTH_SHORT).show();
                        break;
                    case 20:
                        Toast.makeText(getApplicationContext(), "TODO go to ", Toast.LENGTH_SHORT).show();
                        break;
                }

            }
        });
        builder.show();
        /*
        Marker startMarker = new Marker(map);
        startMarker.setPosition(p);
        startMarker.setTitle("Col de lol");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        map.invalidate();
        Toast.makeText(this, "long tapped", Toast.LENGTH_SHORT).show();
        return false;
        */
        overpassTest();
        return false;
    }

    private void overpassTest(){
        new OverpassQueryTask(map.getBoundingBox()).execute();
        /*
        OverpassAPIProvider overpassProvider = new OverpassAPIProvider();
        String url = overpassProvider.urlForTagSearchKml("highway=speed_camera", map.getBoundingBox(), 500, 30);
        KmlDocument kmlDocument = new KmlDocument();


        boolean ok = overpassProvider.addInKmlFolder(kmlDocument.mKmlRoot, url);
        FolderOverlay kmlOverlay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument);
        map.getOverlays().add(kmlOverlay);
        */

        /*
        String query = new OverpassQuery().
                format(OutputFormat.JSON)
                .timeout(30)
                .filterQuery()
                .node()
                .amenity("drinking_water")
                .boundingBox(
                        47.48047027491862, 19.039797484874725,
                        47.51331674014172, 19.07404761761427
                )
                .end()
                .output(OutputVerbosity.BODY, OutputModificator.CENTER, OutputOrder.QT, 100)
                .build();
        Log.i("OPENRUNNER", query);
        */
    }
}
