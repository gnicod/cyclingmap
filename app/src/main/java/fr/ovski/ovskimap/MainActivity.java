package fr.ovski.ovskimap;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.IOrientationConsumer;
import org.osmdroid.views.overlay.compass.IOrientationProvider;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import fr.ovski.ovskimap.markers.MarkerManager;
import fr.ovski.ovskimap.tasks.GraphHopperTask;
import fr.ovski.ovskimap.tasks.OverpassQueryTask;
import kotlin.Unit;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, MapEventsReceiver, IOrientationConsumer ,LocationListener{

    private static final int RC_SIGN_IN = 6000;
    private MapView map;
    private FirebaseUser user;
    private LinearLayout routingView;

    private RadiusMarkerClusterer poiMarkers;

    private LocationManager mLocationManager;
    private MarkerManager markerManager = new MarkerManager();

    private static final int TAP_DEFAULT_MODE = 0;
    private static final int TAP_ROUTING_MODE = 1;

    private int tapState = TAP_DEFAULT_MODE;

    // Routing
    ArrayList<GeoPoint> waypoints;
    ArrayList<Marker> routingMarkers;
    private AsyncTask<Object, Object, Road> routingTask;


    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            // center on position ?
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
    private int deviceOrientation = 0;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                this.user = FirebaseAuth.getInstance().getCurrentUser();
                this.markerManager.setUser(user);
                Log.println(Log.DEBUG, "ovski", "user logged");
            } else {
                Log.println(Log.DEBUG, "ovski", "user not logged");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!"Android-x86".equalsIgnoreCase(Build.BRAND)) {


            //lock the device in current screen orientation
            int orientation;
            int rotation = ((WindowManager) this.getSystemService(
                    Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    this.deviceOrientation = 0;
                    screen_orientation = "ROTATION_0 SCREEN_ORIENTATION_PORTRAIT";
                    break;
                case Surface.ROTATION_90:
                    this.deviceOrientation = 90;
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    screen_orientation = "ROTATION_90 SCREEN_ORIENTATION_LANDSCAPE";
                    break;
                case Surface.ROTATION_180:
                    this.deviceOrientation = 180;
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    screen_orientation = "ROTATION_180 SCREEN_ORIENTATION_REVERSE_PORTRAIT";
                    break;
                default:
                    this.deviceOrientation = 270;
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    screen_orientation = "ROTATION_270 SCREEN_ORIENTATION_REVERSE_LANDSCAPE";
                    break;
            }

            this.setRequestedOrientation(orientation);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission. RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);

            }
        super.onCreate(savedInstanceState);
        Log.i("OVSKIMAP",getApplicationInfo().dataDir);
        Log.i("OVSKIMAP","here");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseAuth auth = FirebaseAuth.getInstance();

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
                createLayersSelectBox();
            }
        });
        buttonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    map.getController().setCenter(new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
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

        routingView = findViewById(R.id.routing_view);

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);

        map = findViewById(R.id.map);
        //map.setTileSource(tileSourceBase);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        IMapController mapController = map.getController();
        mapController.setZoom(12);


        GpsMyLocationProvider gpsMyLocationProvider = new GpsMyLocationProvider(MainActivity.this.getBaseContext());
        gpsMyLocationProvider.setLocationUpdateMinDistance(100); // [m]  // Set the minimum distance for location updates
        gpsMyLocationProvider.setLocationUpdateMinTime(10000);   // [ms] // Set the minimum time interval for location updates
        MyLocationNewOverlay mMyLocationOverlay = new MyLocationNewOverlay(gpsMyLocationProvider, map);
        mMyLocationOverlay.setDrawAccuracyEnabled(true);
        CompassOverlay mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map);

        mCompassOverlay.enableCompass();
        mMyLocationOverlay.enableMyLocation();
        map.getOverlays().add(0, mapEventsOverlay);
        map.getOverlays().add(mCompassOverlay);
        map.getOverlays().add(mMyLocationOverlay);

        // cluster
        map.getOverlays().add(poiMarkers);
        Drawable clusterIconD = getResources().getDrawable(R.drawable.marker_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        poiMarkers.setIcon(clusterIcon);

        if (auth.getCurrentUser() != null) {
            this.user = auth.getCurrentUser();
            markerManager.setUser(this.user);
            // TODO make function userSetted
            markerManager.getAllMarkers(map);
            map.invalidate();
            Log.println(Log.DEBUG, "AUTH", this.user.getDisplayName());
        } else {
            loginUI();
        }







        GeoPoint startPoint = new GeoPoint(45.65, 5.94);
        mapController.setCenter(startPoint);

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
                } catch (IOException | InterruptedException | ExecutionException e) {
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

    public void createLayersSelectBox() {
        String[] listItems = {"Hiking", "Cycling"};

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose items");

        boolean[] checkedItems = new boolean[]{false, false}; //this will checked the items when user open the dialog
        builder.setMultiChoiceItems(listItems, checkedItems, (dialogInterface, i, b) -> {
            // Add tiles layer with custom tile source
            Log.i("DIALOG", String.valueOf(i) + "=>" + b);
            String clicked = listItems[i];
            final MapTileProviderBasic tileProvider = new MapTileProviderBasic(this);
            final ITileSource tileSource = new XYTileSource(clicked, 3, 18, 256, ".png",
                    new String[]{"https://tile.waymarkedtrails.org/" + clicked.toLowerCase() + "/"});
            tileProvider.setTileSource(tileSource);
            final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this.getBaseContext());
            tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
            if (b) {
                map.getOverlays().add(tilesOverlay);
            } else {
                map.getOverlays().remove(tilesOverlay);
            }
        });

        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createSourceSelectBox() {

        final CharSequence[] sources = new CharSequence[]{"Mapnik", "CycleMap", "OpenTopoMap", "HikeBikeMap"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Map source ?");
        builder.setItems(sources, (dialog, which) -> {
            editor = preferences.edit();
            editor.putString("tileSource", sources[which].toString());
            editor.apply();
            ITileSource tileSourceBase = TileSourceFactory.getTileSource(
                    preferences.getString("tileSource", "OpenTopoMap")
            );
            map.setTileSource(tileSourceBase);
            map.invalidate();

        });
        builder.show();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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
        final CharSequence[] sources = new CharSequence[]{"Insert POI", "Start routing", "Go to", "test"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Que faire ?");
        final Context ctx = getApplicationContext();
        builder.setItems(sources, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        //overpassTest();
                        markerManager.createMarker(
                                MainActivity.this,
                                new com.google.firebase.firestore.GeoPoint(p.getLatitude(), p.getLongitude()),
                                (name, group)-> {
                                    Marker startMarker = new Marker(map);
                                    startMarker.setPosition(p);
                                    startMarker.setTitle(name);
                                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    map.getOverlays().add(startMarker);
                                    map.invalidate();
                                    return Unit.INSTANCE;
                                });
                        //Toast.makeText(getApplicationContext(), "TODO: insert marker in db", Toast.LENGTH_SHORT).show();
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
                    case 3:
                        overpassTest();
                        break;
                }

            }
        });
        builder.show();
        return false;
    }

    private void loginUI() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(), RC_SIGN_IN);
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
        Log.i("OVERPASS", map.getBoundingBox().toString());
        new OverpassQueryTask(map, map.getBoundingBox()).execute();
        /*
        OverpassAPIProvider overpassProvider = new OverpassAPIProvider();
        overpassProvider.setService("https://overpass-api.de/api/interpreter");
        String url = overpassProvider.urlForTagSearchKml("amenity=drinking_water", map.getBoundingBox(), 500, 30);
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


    Float trueNorth = 0f;
    float gpsspeed;
    float gpsbearing;
    float lat = 0;
    float lon = 0;
    float alt = 0;
    long timeOfFix = 0;
    String screen_orientation = "";

    @Override
    public void onLocationChanged(Location location) {
        if (map == null)
            return;

        gpsbearing = location.getBearing();
        gpsspeed = location.getSpeed();
        lat = (float) location.getLatitude();
        lon = (float) location.getLongitude();
        alt = (float) location.getAltitude(); //meters
        timeOfFix = location.getTime();


        //use gps bearing instead of the compass

        float t = (360 - gpsbearing - this.deviceOrientation);
        if (t < 0) {
            t += 360;
        }
        if (t > 360) {
            t -= 360;
        }
        //help smooth everything out
        t = (int) t;
        t = t / 5;
        t = (int) t;
        t = t * 5;

        if (gpsspeed >= 0.01) {
            map.setMapOrientation(t);
            //otherwise let the compass take over
        }

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


    @Override
    public void onOrientationChanged(float orientation, IOrientationProvider source) {
        Log.i("OVSKI", "orientation changed");
        //note, on devices without a compass this never fires...

        //only use the compass bit if we aren't moving, since gps is more accurate when we are moving
        if (gpsspeed < 0.01) {
            GeomagneticField gf = new GeomagneticField(lat, lon, alt, timeOfFix);
            trueNorth = orientation + gf.getDeclination();
            gf = null;
            synchronized (trueNorth) {
                if (trueNorth > 360.0f) {
                    trueNorth = trueNorth - 360.0f;
                }
                float actualHeading = 0f;

                //this part adjusts the desired map rotation based on device orientation and compass heading
                float t = (360 - trueNorth - this.deviceOrientation);
                if (t < 0) {
                    t += 360;
                }
                if (t > 360) {
                    t -= 360;
                }
                actualHeading = t;
                //help smooth everything out
                t = (int) t;
                t = t / 5;
                t = (int) t;
                t = t * 5;
                map.setMapOrientation(t);
            }
        }
    }


}
