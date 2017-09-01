package fr.ovski.ovskimap;

import android.Manifest;
import android.content.Context;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import fr.ovski.ovskimap.models.ORPass;
import fr.ovski.ovskimap.tasks.GraphHopperTask;
import fr.ovski.ovskimap.tasks.OverpassQueryTask;
import hu.supercluster.overpasser.library.query.OverpassQuery;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, MapEventsReceiver {

    private MapView map;
    private LinearLayout routingView;

    private RadiusMarkerClusterer poiMarkers;

    private LocationManager mLocationManager;

    private static final int TAP_DEFAULT_MODE = 0;
    private static final int TAP_ROUTING_MODE = 1;

    private int tapState = TAP_DEFAULT_MODE;

    // Routing
    ArrayList<GeoPoint> waypoints;
    ArrayList<Marker> routingMarkers;
    private AsyncTask<Object, Object, Road> routingTask;

    private void addMarkerUserLocation(Location location, boolean current) {
        Marker startMarker = new Marker(map);
        startMarker.setIcon(getResources().getDrawable(R.drawable.marker_default_focused_base));
        startMarker.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
        startMarker.setTitle("ICI");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            addMarkerUserLocation(location, true);
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
        Log.i("OVSKIMAP",getApplicationInfo().dataDir);
        File directory = new File(getApplicationInfo().dataDir+"/files");
        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            Log.d("Files", "FileName:" + files[i].getName());
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        poiMarkers = new RadiusMarkerClusterer(this);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Floating Action Menu
        ImageView icon = new ImageView(this); // Create an icon
        icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_manage));
        FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(icon)
                .build();
        //actionButton.setBackgroundTintList(ColorStateList.valueOf(Color.MAGENTA));
        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
        // repeat many times:
        ImageView itemIconLocation = new ImageView(this);
        itemIconLocation.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_mylocation));
        SubActionButton buttonLocation = itemBuilder.setContentView(itemIconLocation).build();
        // layer
        ImageView itemIconLayer = new ImageView(this);
        itemIconLayer.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_mapmode));
        SubActionButton buttonLayer = itemBuilder.setContentView(itemIconLayer).build();
        // show col
        ImageView itemIconShowCol = new ImageView(this);
        itemIconShowCol.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_gallery));
        SubActionButton buttonShowCol = itemBuilder.setContentView(itemIconShowCol).build();

        FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(buttonLocation)
                .addSubActionView(buttonLayer)
                .addSubActionView(buttonShowCol)
                .attachTo(actionButton)
                .build();

        buttonLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createSourceSelectBox();
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
        buttonShowCol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                poiMarkers.setEnabled(!poiMarkers.isEnabled());
                map.invalidate();
            }
        });
        buttonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    addMarkerUserLocation(lastKnownLocation, false);
                    map.invalidate();
                    mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);
                } catch (SecurityException e) {

                }
            }
        });

        findViewById(R.id.btn_save_route).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSaveRouteBox();
            }
        });

        findViewById(R.id.btn_cancel_route).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRouting();
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
                preferences.getString("tileSource", "OpenTopoMap")
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

        routingView = (LinearLayout) findViewById(R.id.routing_view);

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(tileSourceBase);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        IMapController mapController = map.getController();
        mapController.setZoom(12);

        map.getOverlays().add(0, mapEventsOverlay);

        // cluster
        map.getOverlays().add(poiMarkers);
        Drawable clusterIconD = getResources().getDrawable(R.drawable.marker_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        poiMarkers.setIcon(clusterIcon);


        GeoPoint startPoint = new GeoPoint(45.65, 5.94);
        mapController.setCenter(startPoint);

        /*
        SHOW COL FROM DB
         */
        OpenRunnerRouteDbHelper odb = new OpenRunnerRouteDbHelper(this);
        for (ORPass pass : odb.getAllPasses()) {
            Marker startMarker = new Marker(map);
            startMarker.setIcon(getResources().getDrawable(R.drawable.marker_cluster));
            startMarker.setPosition(new GeoPoint(pass.getLat(), pass.getLng()));
            startMarker.setTitle(pass.getName() + "\n" + pass.getAlt() + "m");
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            poiMarkers.add(startMarker);
            poiMarkers.setEnabled(false);
            Log.i("OPENRUNNER", "FROM db " + pass.toString());
        }

    }

    private void createSaveRouteBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_box_save_route);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton(R.string.save_route, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Road road;
                try {
                    road = routingTask.get();
                    FileOutputStream fos = null;
                    fos = getApplicationContext().openFileOutput("testroute", Context.MODE_PRIVATE);
                    ObjectOutputStream os = new ObjectOutputStream(fos);
                    os.writeObject(new SerializableRoad(road.mRouteHigh));
                    os.close();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    private void createSourceSelectBox() {
        final CharSequence sources[] = new CharSequence[]{"Mapnik", "CycleMap", "OpenTopoMap", "HikeBikeMap"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Map source ?");
        builder.setItems(sources, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editor = preferences.edit();
                editor.putString("tileSource", sources[which].toString());
                editor.commit();
                ITileSource tileSourceBase = TileSourceFactory.getTileSource(
                        preferences.getString("tileSource", "OpenTopoMap")
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
        switch (tapState) {
            case TAP_DEFAULT_MODE:
                Toast.makeText(this, "Tapped", Toast.LENGTH_SHORT).show();
                break;
            case TAP_ROUTING_MODE:
                this.addRoutingMarker(p);
                break;
        }
        return false;
    }


    @Override
    public boolean longPressHelper(final GeoPoint p) {
        final CharSequence sources[] = new CharSequence[]{"Insert POI", "Start routing", "Go to"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Que faire ?");
        builder.setItems(sources, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        overpassTest();
                        Toast.makeText(getApplicationContext(), "TODO: insert marker in db", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        tapState = TAP_ROUTING_MODE;
                        waypoints = new ArrayList<GeoPoint>();
                        routingMarkers = new ArrayList<Marker>();
                        addRoutingMarker(p);
                        break;
                    case 2:
                        tapState = TAP_ROUTING_MODE;
                        waypoints = new ArrayList<GeoPoint>();
                        routingMarkers = new ArrayList<Marker>();
                        try {
                            Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            GeoPoint start = (new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                            addRoutingMarker(start);
                            addRoutingMarker(p);
                        } catch (SecurityException e) {
                            Toast.makeText(getApplicationContext(), "TODO go to ", Toast.LENGTH_SHORT).show();
                        }
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
        //overpassTest();
        return false;
    }

    private void addRoutingMarker(GeoPoint point) {
        Toast.makeText(this, "add new point ", Toast.LENGTH_SHORT).show();
        waypoints.add(point);
        if (waypoints.size()>1) {
            routingTask = new GraphHopperTask(map, routingView, waypoints).execute();
        }
        Marker startMarker = new Marker(map);
        startMarker.setPosition(point);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        routingMarkers.add(startMarker);
        map.getOverlays().add(startMarker);
        map.invalidate();
    }

    private void overpassTest() {
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

    public void cancelRouting(){
        routingView.setVisibility(View.GONE);
        map.getOverlays().removeAll(routingMarkers);
        map.invalidate();
        for(Overlay o :  map.getOverlays()) {
            if(o instanceof Polyline){
                if(((Polyline) o).getTitle() == GraphHopperTask.OVERLAY_TITLE) {
                    Log.i("OVSKIMAP","remove");
                    ((Polyline) o).setVisible(false);
                    o.setEnabled(false);
                    map.invalidate();
                }
            }
            Log.i("OVSKIMAP",o.getClass() +"");
        }
        tapState = TAP_DEFAULT_MODE;
    }
}
